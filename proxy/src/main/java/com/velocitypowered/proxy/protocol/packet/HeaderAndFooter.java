package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants.Direction;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.writeString;

import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializer;
import net.kyori.text.serializer.ComponentSerializers;

public class HeaderAndFooter implements MinecraftPacket {

    private String header;
    private String footer;

    public HeaderAndFooter() {
    }

    public HeaderAndFooter(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    @Override
    public void decode(ByteBuf buf, Direction direction, int protocolVersion) {
        // We dont handle this packet from backend
    }

    @Override
    public void encode(ByteBuf buf, Direction direction, int protocolVersion) {
        writeString(buf, header);
        writeString(buf, footer);
    }

    public static HeaderAndFooter create(Component header, Component footer) {
        ComponentSerializer<Component, Component, String> json = ComponentSerializers.JSON;
        return new HeaderAndFooter(json.serialize(header), json.serialize(footer));
    }
    
}
