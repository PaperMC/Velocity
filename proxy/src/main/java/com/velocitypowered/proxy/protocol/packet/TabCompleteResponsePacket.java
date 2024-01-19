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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TabCompleteResponsePacket implements MinecraftPacket {

  private int transactionId;
  private int start;
  private int length;
  private final List<Offer> offers = new ArrayList<>();

  public int getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(int transactionId) {
    this.transactionId = transactionId;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public List<Offer> getOffers() {
    return offers;
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
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(MINECRAFT_1_13)) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.start = ProtocolUtils.readVarInt(buf);
      this.length = ProtocolUtils.readVarInt(buf);
      int offersAvailable = ProtocolUtils.readVarInt(buf);
      for (int i = 0; i < offersAvailable; i++) {
        String offer = ProtocolUtils.readString(buf);
        ComponentHolder tooltip = buf.readBoolean() ? ComponentHolder.read(buf, version) : null;
        offers.add(new Offer(offer, tooltip));
      }
    } else {
      int offersAvailable = ProtocolUtils.readVarInt(buf);
      for (int i = 0; i < offersAvailable; i++) {
        offers.add(new Offer(ProtocolUtils.readString(buf), null));
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.noLessThan(MINECRAFT_1_13)) {
      ProtocolUtils.writeVarInt(buf, this.transactionId);
      ProtocolUtils.writeVarInt(buf, this.start);
      ProtocolUtils.writeVarInt(buf, this.length);
      ProtocolUtils.writeVarInt(buf, offers.size());
      for (Offer offer : offers) {
        ProtocolUtils.writeString(buf, offer.text);
        buf.writeBoolean(offer.tooltip != null);
        if (offer.tooltip != null) {
          offer.tooltip.write(buf);
        }
      }
    } else {
      ProtocolUtils.writeVarInt(buf, offers.size());
      for (Offer offer : offers) {
        ProtocolUtils.writeString(buf, offer.text);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
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
