package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * A precisely ordered queue which allows for outside entries into the ordered queue through piggybacking timestamps.
 */
public class ChatQueue {
  private final Object internalLock;
  private final ConnectedPlayer player;
  private CompletableFuture<TimestampedPacket> packetFuture;

  /**
   * Instantiates a {@link ChatQueue} for a specific {@link ConnectedPlayer}.
   *
   * @param player the {@link ConnectedPlayer} to maintain the queue for.
   */
  public ChatQueue(ConnectedPlayer player) {
    this.player = player;
    this.packetFuture = CompletableFuture.completedFuture(new TimestampedPacket(Instant.EPOCH, null));
    this.internalLock = new Object();
  }

  /**
   * Queues a packet sent from the player - all packets must wait until this processes to send their packets.
   * <br />
   * This maintains order on the server-level for the client insertions of commands and messages. All entries are locked
   * through an internal object lock.
   *
   * @param nextSender the {@link CompletableFuture} which will provide the next-processed packet.
   * @param timestamp  the {@link Instant} timestamp of this packet so we can allow piggybacking.
   */
  public void queuePacket(CompletableFuture<MinecraftPacket> nextSender, Instant timestamp) {
    synchronized (internalLock) {
      CompletableFuture<TimestampedPacket> trueFuture = nextSender
          .thenApply(packet -> new TimestampedPacket(timestamp, packet))
          .exceptionally(ignored -> new TimestampedPacket(timestamp, null));
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();
      this.packetFuture.thenRun(() -> trueFuture.whenCompleteAsync((timestampedPacket, throwable) -> {
        if (timestampedPacket != null) {
          smc.write(timestampedPacket.packet);
        }
      }, smc.eventLoop()));

      this.packetFuture = trueFuture;
    }
  }

  /**
   * Piggybacks the latest sent packet's timestamp to provide an in-order packet without polling the physical, or prior
   * packets sent through the stream.
   *
   * @param packet        the {@link MinecraftPacket} to send.
   * @param instantMapper the {@link BiFunction} which maps the prior timestamp and current packet to a new packet.
   * @param <T>           the type of packet for instantMapper type-checking.
   */
  public <T extends MinecraftPacket> void piggyBack(T packet, BiFunction<Instant, T, T> instantMapper) {
    synchronized (internalLock) {
      CompletableFuture<T> trueFuture = CompletableFuture.completedFuture(packet);
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();

      this.packetFuture.whenComplete((ts, throwable) -> {
        TimestampedPacket tss = Objects.requireNonNullElseGet(ts, () -> new TimestampedPacket(Instant.now(), null));
        trueFuture.thenApply(pkt -> new TimestampedPacket(tss.timestamp, instantMapper.apply(tss.timestamp, pkt)))
            .exceptionally(ignored -> new TimestampedPacket(tss.timestamp, null))
            .whenCompleteAsync((timestampedPacket, throwable2) -> {
              if (timestampedPacket != null) {
                smc.write(timestampedPacket.packet);
              }
            }, smc.eventLoop());
      });
    }
  }

  private static class TimestampedPacket {
    private final Instant timestamp;
    private final MinecraftPacket packet;

    private TimestampedPacket(Instant timestamp, MinecraftPacket packet) {
      this.timestamp = timestamp;
      this.packet = packet;
    }
  }
}
