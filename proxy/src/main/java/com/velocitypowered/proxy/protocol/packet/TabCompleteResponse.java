package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;

import java.util.ArrayList;
import java.util.List;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.MINECRAFT_1_13;

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
        return "TabCompleteResponse{" +
                "transactionId=" + transactionId +
                ", start=" + start +
                ", length=" + length +
                ", offers=" + offers +
                '}';
    }

    @Override
    public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocolVersion >= MINECRAFT_1_13) {
            this.transactionId = ProtocolUtils.readVarInt(buf);
            this.start = ProtocolUtils.readVarInt(buf);
            this.length = ProtocolUtils.readVarInt(buf);
            int offersAvailable = ProtocolUtils.readVarInt(buf);
            for (int i = 0; i < offersAvailable; i++) {
                String entry = ProtocolUtils.readString(buf);
                Component component = buf.readBoolean() ? ComponentSerializers.JSON.deserialize(ProtocolUtils.readString(buf)) :
                        null;
                offers.add(new Offer(entry, component));
            }
        } else {
            int offersAvailable = ProtocolUtils.readVarInt(buf);
            for (int i = 0; i < offersAvailable; i++) {
                offers.add(new Offer(ProtocolUtils.readString(buf), null));
            }
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        if (protocolVersion >= MINECRAFT_1_13) {
            ProtocolUtils.writeVarInt(buf, transactionId);
            ProtocolUtils.writeVarInt(buf, start);
            ProtocolUtils.writeVarInt(buf, length);
            ProtocolUtils.writeVarInt(buf, offers.size());
            for (Offer offer : offers) {
                ProtocolUtils.writeString(buf, offer.entry);
                buf.writeBoolean(offer.tooltip != null);
                if (offer.tooltip != null) {
                    ProtocolUtils.writeString(buf, ComponentSerializers.JSON.serialize(offer.tooltip));
                }
            }
        } else {
            ProtocolUtils.writeVarInt(buf, offers.size());
            for (Offer offer : offers) {
                ProtocolUtils.writeString(buf, offer.entry);
            }
        }
    }

    public static class Offer {
        private final String entry;
        private final Component tooltip;

        public Offer(String entry, Component tooltip) {
            this.entry = entry;
            this.tooltip = tooltip;
        }

        public String getEntry() {
            return entry;
        }

        public Component getTooltip() {
            return tooltip;
        }

        @Override
        public String toString() {
            return "Offer{" +
                    "entry='" + entry + '\'' +
                    ", tooltip=" + tooltip +
                    '}';
        }
    }
}
