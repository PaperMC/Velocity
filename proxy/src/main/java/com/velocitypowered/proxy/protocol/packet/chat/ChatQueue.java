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
import org.checkerframework.checker.nullness.qual.Nullable;
import java.time.Instant;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A precisely ordered queue which allows for outside entries into the ordered queue through
 * piggybacking timestamps.
 */
public class ChatQueue {

  private final Object internalLock = new Object();
  private final ConnectedPlayer player;
  private final ChatState chatState = new ChatState();
  private CompletableFuture<Void> head = CompletableFuture.completedFuture(null);

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
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();
      head = head.thenCompose(v -> {
        try {
          return task.update(chatState, smc).exceptionally(ignored -> null);
        } catch (Throwable ignored) {
          return CompletableFuture.completedFuture(null);
        }
      });
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

  private static <T extends MinecraftPacket> CompletableFuture<Void> writePacket(T packet, MinecraftConnection smc) {
    return CompletableFuture.runAsync(() -> {
      if (!smc.isClosed()) {
        ChannelFuture future = smc.write(packet);
        if (future != null) {
          future.awaitUninterruptibly();
        }
      }
    }, smc.eventLoop());
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
      return new LastSeenMessages(0, lastSeenMessages);
    }
  }
}
