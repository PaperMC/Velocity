/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TabCompleteResponsePacket implements MinecraftPacket {

  private final int transactionId;
  private final int start;
  private final int length;
  private final List<Offer> offers;

  public TabCompleteResponsePacket(int transactionId, int start, int length, List<Offer> offers) {
    this.transactionId = transactionId;
    this.start = start;
    this.length = length;
    this.offers = offers;
  }

  public int transactionId() {
    return transactionId;
  }

  public int start() {
    return start;
  }

  public int length() {
    return length;
  }

  public List<Offer> offers() {
    return offers;
  }

  public int getTransactionId() {
    return transactionId();
  }

  public int getStart() {
    return start();
  }

  public int getLength() {
    return length();
  }

  public List<Offer> getOffers() {
    return offers();
  }

  public TabCompleteResponsePacket withOffers(List<Offer> offers) {
    return new TabCompleteResponsePacket(transactionId, start, length, offers);
  }

  @Override
  public String toString() {
    return "TabCompleteResponse{"
        + "transactionId=" + transactionId
        + ", start=" + start
        + ", length=" + length
        + ", offers=" + offers
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<TabCompleteResponsePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public TabCompleteResponsePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      if (version.noLessThan(MINECRAFT_1_13)) {
        int transactionId = ProtocolUtils.readVarInt(buf);
        int start = ProtocolUtils.readVarInt(buf);
        int length = ProtocolUtils.readVarInt(buf);
        int offersAvailable = ProtocolUtils.readVarInt(buf);
        List<Offer> offers = new ArrayList<>(offersAvailable);
        for (int i = 0; i < offersAvailable; i++) {
          String offer = ProtocolUtils.readString(buf);
          ComponentHolder tooltip = buf.readBoolean() ? ComponentHolder.read(buf, version) : null;
          offers.add(new Offer(offer, tooltip));
        }
        return new TabCompleteResponsePacket(transactionId, start, length, offers);
      } else {
        int offersAvailable = ProtocolUtils.readVarInt(buf);
        List<Offer> offers = new ArrayList<>(offersAvailable);
        for (int i = 0; i < offersAvailable; i++) {
          offers.add(new Offer(ProtocolUtils.readString(buf), null));
        }
        return new TabCompleteResponsePacket(0, 0, 0, offers);
      }
    }

    @Override
    public void encode(TabCompleteResponsePacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion version) {
      if (version.noLessThan(MINECRAFT_1_13)) {
        ProtocolUtils.writeVarInt(buf, packet.transactionId);
        ProtocolUtils.writeVarInt(buf, packet.start);
        ProtocolUtils.writeVarInt(buf, packet.length);
        ProtocolUtils.writeVarInt(buf, packet.offers.size());
        for (Offer offer : packet.offers) {
          ProtocolUtils.writeString(buf, offer.text);
          buf.writeBoolean(offer.tooltip != null);
          if (offer.tooltip != null) {
            offer.tooltip.write(buf);
          }
        }
      } else {
        ProtocolUtils.writeVarInt(buf, packet.offers.size());
        for (Offer offer : packet.offers) {
          ProtocolUtils.writeString(buf, offer.text);
        }
      }
    }
  }

  public static class Offer implements Comparable<Offer> {

    private final String text;
    private final @Nullable ComponentHolder tooltip;

    public Offer(String text) {
      this(text, null);
    }

    public Offer(String text, @Nullable ComponentHolder tooltip) {
      this.text = text;
      this.tooltip = tooltip;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Offer offer = (Offer) o;

      return text.equals(offer.text);
    }

    @Override
    public int hashCode() {
      return text.hashCode();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("text", text)
          .add("tooltip", tooltip)
          .toString();
    }

    @Override
    public int compareTo(Offer o) {
      return this.text.compareTo(o.text);
    }

    public String getText() {
      return text;
    }
  }
}
