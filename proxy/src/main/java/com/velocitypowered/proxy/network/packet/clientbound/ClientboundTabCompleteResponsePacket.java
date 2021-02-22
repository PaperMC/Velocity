package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundTabCompleteResponsePacket implements Packet {
  public static final PacketReader<ClientboundTabCompleteResponsePacket> DECODER = PacketReader.method(ClientboundTabCompleteResponsePacket::new);
  public static final PacketWriter<ClientboundTabCompleteResponsePacket> ENCODER = PacketWriter.deprecatedEncode();

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
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_13)) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.start = ProtocolUtils.readVarInt(buf);
      this.length = ProtocolUtils.readVarInt(buf);
      int offersAvailable = ProtocolUtils.readVarInt(buf);
      for (int i = 0; i < offersAvailable; i++) {
        String offer = ProtocolUtils.readString(buf);
        Component tooltip = buf.readBoolean() ? ProtocolUtils.getJsonChatSerializer(version)
            .deserialize(ProtocolUtils.readString(buf)) : null;
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
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_13)) {
      ProtocolUtils.writeVarInt(buf, this.transactionId);
      ProtocolUtils.writeVarInt(buf, this.start);
      ProtocolUtils.writeVarInt(buf, this.length);
      ProtocolUtils.writeVarInt(buf, offers.size());
      for (Offer offer : offers) {
        ProtocolUtils.writeString(buf, offer.text);
        buf.writeBoolean(offer.tooltip != null);
        if (offer.tooltip != null) {
          ProtocolUtils.writeString(buf, ProtocolUtils.getJsonChatSerializer(version)
              .serialize(offer.tooltip));
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
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("transactionId", this.transactionId)
      .add("start", this.start)
      .add("length", this.length)
      .add("offers", this.offers)
      .toString();
  }

  public static class Offer implements Comparable<Offer> {
    private final String text;
    private final @Nullable Component tooltip;

    public Offer(String text) {
      this(text, null);
    }

    public Offer(String text,
        @Nullable Component tooltip) {
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
