/*
 * Copyright (C) 2022-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A precisely ordered queue which allows for outside entries into the ordered queue through
 * piggybacking timestamps.
 */
public class ChatQueue implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(ChatQueue.class);

  // bVelocity: backpressure water marks on the head-chain depth. Netty's WriteBufferWaterMark does
  // not engage here because the queue serializes writes (one packet in flight per player), so the
  // outbound buffer never crosses the high mark. Without these marks a peer that stops reading
  // would let the head chain grow without bound (each pending task pins a MinecraftPacket + ByteBuf).
  // HIGH/LOW provide hysteresis for toggling client autoRead; HARD_MAX is the safety net that
  // disconnects a player whose backend is wedged beyond recovery. LOW >= 2*WINDOW_SIZE leaves room
  // for the acknowledgement window so normal bursts don't flap autoRead.
  private static final int BACKPRESSURE_LOW = 2 * LastSeenMessages.WINDOW_SIZE;
  private static final int BACKPRESSURE_HIGH = 4 * LastSeenMessages.WINDOW_SIZE;
  private static final int BACKPRESSURE_HARD_MAX = 8 * LastSeenMessages.WINDOW_SIZE;
  private static final long WRITE_TIMEOUT_MILLIS = 15_000L;

  private final Object internalLock = new Object();
  private final ConnectedPlayer player;
  private final ChatState chatState = new ChatState();
  private CompletableFuture<Void> head = CompletableFuture.completedFuture(null);
  private final AtomicInteger pending = new AtomicInteger();
  private volatile boolean autoReadPaused;
  private volatile boolean closed;

  /**
   * Instantiates a {@link ChatQueue} for a specific {@link ConnectedPlayer}.
   *
   * @param player the {@link ConnectedPlayer} to maintain the queue for.
   */
  public ChatQueue(ConnectedPlayer player) {
    this.player = player;
  }

  private void queueTask(Task task) {
    synchronized (internalLock) {
      if (closed) {
        throw new IllegalStateException("ChatQueue has already been closed");
      }
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();

      // bVelocity: backpressure on the head-chain depth. The write path no longer blocks the event
      // loop (see writePacket), so a peer that stops reading would otherwise let this chain grow
      // without bound. Throttle inbound reads on the client connection when the queue deepens past
      // HIGH, and resume once it drains below LOW — reusing the existing AutoReadHolderHandler path
      // rather than blocking a thread. HARD_MAX disconnects a player whose backend is wedged.
      final int depth = pending.incrementAndGet();
      if (depth >= BACKPRESSURE_HARD_MAX) {
        pending.decrementAndGet();
        LOGGER.warn("ChatQueue for {} exceeded {} pending packets; disconnecting (wedged backend?).",
            player, BACKPRESSURE_HARD_MAX);
        player.disconnect(Component.translatable("velocity.error.player-connection-error",
            NamedTextColor.RED));
        return;
      }
      if (depth >= BACKPRESSURE_HIGH && !autoReadPaused) {
        autoReadPaused = true;
        pauseClientAutoRead();
      }

      head = head.thenCompose(v -> {
        if (closed) {
          return CompletableFuture.completedFuture(null);
        }
        try {
          return task.update(chatState, smc).exceptionally(ignored -> null);
        } catch (Throwable ignored) {
          return CompletableFuture.completedFuture(null);
        }
      }).whenComplete((ignored, throwable) -> drainPending());
    }
  }

  private void drainPending() {
    final int depth = pending.decrementAndGet();
    if (depth <= BACKPRESSURE_LOW && autoReadPaused) {
      autoReadPaused = false;
      resumeClientAutoRead();
    }
  }

  /**
   * Pauses autoRead on the player's client connection so inbound chat/command packets are held by
   * the existing {@code AutoReadHolderHandler} instead of piling onto the head chain. Must run on
   * that connection's event loop ({@link MinecraftConnection#setAutoReading(boolean)} asserts it).
   */
  private void pauseClientAutoRead() {
    final MinecraftConnection conn = player.getConnection();
    if (conn != null && !conn.isClosed()) {
      conn.eventLoop().execute(() -> conn.setAutoReading(false));
    }
  }

  private void resumeClientAutoRead() {
    final MinecraftConnection conn = player.getConnection();
    if (conn != null && !conn.isClosed()) {
      conn.eventLoop().execute(() -> conn.setAutoReading(true));
    }
  }

  /**
   * Queues a packet sent from the player - all packets must wait until this processes to send their
   * packets. This maintains order on the server-level for the client insertions of commands
   * and messages. All entries are locked through an internal object lock.
   *
   * @param nextPacket       a function mapping {@link LastSeenMessages} state to a {@link CompletableFuture} that will
   *                         provide the next-processed packet. This should include the fixed {@link LastSeenMessages}.
   * @param timestamp        the new {@link Instant} timestamp of this packet to update the internal chat state.
   * @param lastSeenMessages the new {@link LastSeenMessages} last seen messages to update the internal chat state.
   */
  public void queuePacket(Function<LastSeenMessages, CompletableFuture<MinecraftPacket>> nextPacket, @Nullable Instant timestamp, @Nullable LastSeenMessages lastSeenMessages) {
    queueTask((chatState, smc) -> {
      LastSeenMessages newLastSeenMessages = chatState.updateFromMessage(timestamp, lastSeenMessages);
      return nextPacket.apply(newLastSeenMessages).thenCompose(packet -> writePacket(packet, smc));
    });
  }

  /**
   * Hijacks the latest sent packet's chat state to provide an in-order packet without polling the
   * physical, or prior packets sent through the stream.
   *
   * @param packetFunction a function that maps the prior {@link ChatState} into a new packet.
   * @param <T>            the type of packet to send.
   */
  public <T extends MinecraftPacket> void queuePacket(Function<ChatState, T> packetFunction) {
    queueTask((chatState, smc) -> {
      T packet = packetFunction.apply(chatState);
      return writePacket(packet, smc);
    });
  }

  public void handleAcknowledgement(int offset) {
    queueTask((chatState, smc) -> {
      int ackCountToForward = chatState.accumulateAckCount(offset);
      if (ackCountToForward > 0) {
        return writePacket(new ChatAcknowledgementPacket(ackCountToForward), smc);
      }
      return CompletableFuture.completedFuture(null);
    });
  }

  private <T extends MinecraftPacket> CompletableFuture<Void> writePacket(T packet, MinecraftConnection smc) {
    if (closed || smc.isClosed()) {
      return CompletableFuture.completedFuture(null);
    }
    final ChannelFuture future = smc.write(packet);
    if (future == null) {
      return CompletableFuture.completedFuture(null);
    }
    // bVelocity: do not block the event loop waiting for the write. The original
    // implementation called future.awaitUninterruptibly() from a task scheduled on the event loop,
    // which blocks that loop's thread until the flush completes. If the peer stops reading and the
    // socket send buffer fills, writeAndFlush only completes once OP_WRITE fires — but the loop
    // thread is parked here and cannot process OP_WRITE, stalling every connection on that loop.
    // Using a listener continuation keeps the loop free to run while the write is in flight, while
    // still preserving the per-player ordering guarantee: callers chain off the returned future via
    // head.thenCompose(...), so the next packet is only written once this one resolves.
    final CompletableFuture<Void> done = new CompletableFuture<>();
    future.addListener((ChannelFutureListener) f -> {
      if (f.isSuccess()) {
        done.complete(null);
      } else {
        done.completeExceptionally(f.cause());
      }
    });
    // bVelocity: bound the write so a wedged backend (peer stops reading, OP_WRITE never fires)
    // fails the chain instead of letting the head chain pile up to the HARD_MAX disconnect. The
    // timeout fires on the write's own event loop; completeExceptionally is a no-op if the listener
    // already completed the future.
    smc.eventLoop().schedule(() -> done.completeExceptionally(
        new java.util.concurrent.TimeoutException("ChatQueue write timed out")), WRITE_TIMEOUT_MILLIS,
        TimeUnit.MILLISECONDS);
    return done;
  }

  @Override
  public void close() {
    closed = true;
    // bVelocity: ensure we never leave the client connection with autoRead paused after this queue
    // is discarded (e.g. on server switch). A fresh ChatQueue starts unpaused.
    if (autoReadPaused) {
      autoReadPaused = false;
      resumeClientAutoRead();
    }
  }

  private interface Task {
    CompletableFuture<Void> update(ChatState chatState, MinecraftConnection smc);
  }

  /**
   * Tracks the last Secure Chat state that we received from the client. This is important to always have a valid 'last
   * seen' state that is consistent with future and past updates from the client (which may be signed). This state is
   * used to construct 'spoofed' command packets from the proxy to the server.
   * <ul>
   *     <li>If we last forwarded a chat or command packet from the client, we have a known 'last seen' that we can
   *     reuse.</li>
   *     <li>If we last forwarded a {@link ChatAcknowledgementPacket}, the previous 'last seen' cannot be reused. We
   *     cannot predict an up-to-date 'last seen', as we do not know which messages the client actually saw.</li>
   *     <li>Therefore, we need to hold back any acknowledgement packets so that we can continue to reuse the last valid
   *     'last seen' state.</li>
   *     <li>However, there is a limit to the number of messages that can remain unacknowledged on the server.</li>
   *     <li>To address this, we know that if the client has moved its 'last seen' window far enough, we can fill in the
   *     gap with dummy 'last seen', and it will never be checked.</li>
   * </ul>
   *
   * Note that this is effectively unused for 1.20.5+ clients, as commands without any signature do not send 'last seen'
   * updates.
   */
  public static class ChatState {
    private static final int MINIMUM_DELAYED_ACK_COUNT = LastSeenMessages.WINDOW_SIZE;
    private static final BitSet DUMMY_LAST_SEEN_MESSAGES = new BitSet();

    public volatile Instant lastTimestamp = Instant.EPOCH;
    private volatile BitSet lastSeenMessages = new BitSet();
    private final AtomicInteger delayedAckCount = new AtomicInteger();

    private ChatState() {
    }

    @Nullable
    public LastSeenMessages updateFromMessage(@Nullable Instant timestamp, @Nullable LastSeenMessages lastSeenMessages) {
      if (timestamp != null) {
        this.lastTimestamp = timestamp;
      }
      if (lastSeenMessages != null) {
        // We held back some acknowledged messages, so flush that out now that we have a known 'last seen' state again
        int delayedAckCount = this.delayedAckCount.getAndSet(0);
        this.lastSeenMessages = lastSeenMessages.getAcknowledged();
        return lastSeenMessages.offset(delayedAckCount);
      }
      return null;
    }

    public int accumulateAckCount(int ackCount) {
      int delayedAckCount = this.delayedAckCount.addAndGet(ackCount);
      int ackCountToForward = delayedAckCount - MINIMUM_DELAYED_ACK_COUNT;
      if (ackCountToForward >= LastSeenMessages.WINDOW_SIZE) {
        // Because we only forward acknowledgements above the window size, we don't have to shift the previous 'last seen' state
        this.lastSeenMessages = DUMMY_LAST_SEEN_MESSAGES;
        this.delayedAckCount.set(MINIMUM_DELAYED_ACK_COUNT);
        return ackCountToForward;
      }
      return 0;
    }

    public LastSeenMessages createLastSeen() {
      return new LastSeenMessages(0, lastSeenMessages, (byte) 0);
    }
  }
}
