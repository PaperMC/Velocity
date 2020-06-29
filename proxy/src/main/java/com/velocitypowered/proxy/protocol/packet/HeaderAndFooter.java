package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.protocol.ProtocolUtils.writeString;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializer;
import net.kyori.text.serializer.gson.GsonComponentSerializer;

public class HeaderAndFooter implements MinecraftPacket {

  private static final String EMPTY_COMPONENT = "{\"translate\":\"\"}";
  private static final HeaderAndFooter RESET = new HeaderAndFooter();

  private String header;
  private String footer;

  public HeaderAndFooter() {
    this(EMPTY_COMPONENT, EMPTY_COMPONENT);
  }

  public HeaderAndFooter(String header, String footer) {
    this.header = Preconditions.checkNotNull(header, "header");
    this.footer = Preconditions.checkNotNull(footer, "footer");
  }

  public String getHeader() {
    return header;
  }

  public String getFooter() {
    return footer;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException("Decode is not implemented");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    writeString(buf, header);
    writeString(buf, footer);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static HeaderAndFooter create(Component header, Component footer) {
    ComponentSerializer<Component, Component, String> json = GsonComponentSerializer.INSTANCE;
    return new HeaderAndFooter(json.serialize(header), json.serialize(footer));
  }

  public static HeaderAndFooter create(net.kyori.adventure.text.Component header,
      net.kyori.adventure.text.Component footer, ProtocolVersion protocolVersion) {
    Gson serializer = VelocityServer.getGsonInstance(protocolVersion);
    return new HeaderAndFooter(serializer.toJson(header), serializer.toJson(footer));
  }

  public static HeaderAndFooter reset() {
    return RESET;
  }
}
