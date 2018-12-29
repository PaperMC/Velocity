package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TabCompleteResponse implements MinecraftPacket {

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
    if (version.compareTo(MINECRAFT_1_13) >= 0) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.start = ProtocolUtils.readVarInt(buf);
      this.length = ProtocolUtils.readVarInt(buf);
      int offersAvailable = ProtocolUtils.readVarInt(buf);
      for (int i = 0; i < offersAvailable; i++) {
        String offer = ProtocolUtils.readString(buf);
        Component tooltip = buf.readBoolean() ? ComponentSerializers.JSON.deserialize(
            ProtocolUtils.readString(buf)) : null;
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
    if (version.compareTo(MINECRAFT_1_13) >= 0) {
      ProtocolUtils.writeVarInt(buf, this.transactionId);
      ProtocolUtils.writeVarInt(buf, this.start);
      ProtocolUtils.writeVarInt(buf, this.length);
      ProtocolUtils.writeVarInt(buf, offers.size());
      for (Offer offer : offers) {
        ProtocolUtils.writeString(buf, offer.text);
        buf.writeBoolean(offer.tooltip != null);
        if (offer.tooltip != null) {
          ProtocolUtils.writeString(buf, ComponentSerializers.JSON.serialize(offer.tooltip));
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
    @Nullable
    private final Component tooltip;

    public Offer(String text,
        @Nullable Component tooltip) {
      this.text = text;
      this.tooltip = tooltip;
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
  }
}
