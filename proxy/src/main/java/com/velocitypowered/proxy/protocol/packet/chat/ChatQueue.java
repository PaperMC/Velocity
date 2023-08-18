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
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * A precisely ordered queue which allows for outside entries into the ordered queue through
 * piggybacking timestamps.
 */
public class ChatQueue {

  private final Object internalLock;
  private final ConnectedPlayer player;
  private CompletableFuture<WrappedPacket> packetFuture;

  /**
   * Instantiates a {@link ChatQueue} for a specific {@link ConnectedPlayer}.
   *
   * @param player the {@link ConnectedPlayer} to maintain the queue for.
   */
  public ChatQueue(ConnectedPlayer player) {
    this.player = player;
    this.packetFuture = CompletableFuture.completedFuture(new WrappedPacket(Instant.EPOCH, null));
    this.internalLock = new Object();
  }

  /**
   * Queues a packet sent from the player - all packets must wait until this processes to send their
   * packets. This maintains order on the server-level for the client insertions of commands
   * and messages. All entries are locked through an internal object lock.
   *
   * @param nextPacket the {@link CompletableFuture} which will provide the next-processed packet.
   * @param timestamp  the {@link Instant} timestamp of this packet so we can allow piggybacking.
   */
  public void queuePacket(CompletableFuture<MinecraftPacket> nextPacket, Instant timestamp) {
    synchronized (internalLock) { // wait for the lock to resolve - we don't want to drop packets
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();

      CompletableFuture<WrappedPacket> nextInLine = WrappedPacket.wrap(timestamp, nextPacket);
      awaitChat(smc, this.packetFuture,
          nextInLine); // we await chat, binding `this.packetFuture` -> `nextInLine`
      this.packetFuture = nextInLine;
    }
  }

  /**
   * Hijacks the latest sent packet's timestamp to provide an in-order packet without polling the
   * physical, or prior packets sent through the stream.
   *
   * @param packet        the {@link MinecraftPacket} to send.
   * @param instantMapper the {@link InstantPacketMapper} which maps the prior timestamp and current
   *                      packet to a new packet.
   * @param <K>           the type of base to expect when mapping the packet.
   * @param <V>           the type of packet for instantMapper type-checking.
   */
  public <K, V extends MinecraftPacket> void hijack(K packet,
      InstantPacketMapper<K, V> instantMapper) {
    synchronized (internalLock) {
      CompletableFuture<K> trueFuture = CompletableFuture.completedFuture(packet);
      MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();

      this.packetFuture = hijackCurrentPacket(smc, this.packetFuture, trueFuture, instantMapper);
    }
  }

  private static BiConsumer<WrappedPacket, Throwable> writePacket(MinecraftConnection connection) {
    return (wrappedPacket, throwable) -> {
      if (wrappedPacket != null && !connection.isClosed()) {
        wrappedPacket.write(connection);
      }
    };
  }

  private static <T extends MinecraftPacket> void awaitChat(
      MinecraftConnection connection,
      CompletableFuture<WrappedPacket> binder,
      CompletableFuture<WrappedPacket> future
  ) {
    // the binder will run -> then the future will get the `write packet` caller
    binder.whenComplete((ignored1, ignored2) -> future.whenComplete(writePacket(connection)));
  }

  private static <K, V extends MinecraftPacket> CompletableFuture<WrappedPacket> hijackCurrentPacket(
      MinecraftConnection connection,
      CompletableFuture<WrappedPacket> binder,
      CompletableFuture<K> future,
      InstantPacketMapper<K, V> packetMapper
  ) {
    CompletableFuture<WrappedPacket> awaitedFuture = new CompletableFuture<>();
    // the binder will complete -> then the future will get the `write packet` caller
    binder.whenComplete((previous, ignored) -> {
      // map the new packet into a better "designed" packet with the hijacked packet's timestamp
      WrappedPacket.wrap(previous.timestamp,
              future.thenApply(item -> packetMapper.map(previous.timestamp, item)))
          .whenCompleteAsync(writePacket(connection), connection.eventLoop())
          .whenComplete(
              (packet, throwable) -> awaitedFuture.complete(throwable != null ? null : packet));
    });
    return awaitedFuture;
  }

  /**
   * Provides an {@link Instant} based timestamp mapper from an existing object to create a packet.
   *
   * @param <K> The base object type to map.
   * @param <V> The resulting packet type.
   */
  public interface InstantPacketMapper<K, V extends MinecraftPacket> {

    /**
     * Maps a value into a packet with it and a timestamp.
     *
     * @param nextInstant   the {@link Instant} timestamp to use for tracking.
     * @param currentObject the current item to map to the packet.
     * @return The resulting packet from the mapping.
     */
    V map(Instant nextInstant, K currentObject);
  }

  private static class WrappedPacket {

    private final Instant timestamp;
    private final MinecraftPacket packet;

    private WrappedPacket(Instant timestamp, MinecraftPacket packet) {
      this.timestamp = timestamp;
      this.packet = packet;
    }

    public void write(MinecraftConnection connection) {
      if (packet != null) {
        connection.write(packet);
      }
    }

    private static CompletableFuture<WrappedPacket> wrap(Instant timestamp,
        CompletableFuture<MinecraftPacket> nextPacket) {
      return nextPacket
          .thenApply(pkt -> new WrappedPacket(timestamp, pkt))
          .exceptionally(ignored -> new WrappedPacket(timestamp, null));
    }
  }
}
