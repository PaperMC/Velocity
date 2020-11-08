package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.protocol.ProtocolUtils.writeString;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import io.netty.buffer.ByteBuf;

public class HeaderAndFooterPacket implements Packet {
  public static final Decoder<HeaderAndFooterPacket> DECODER = Decoder.method(HeaderAndFooterPacket::new);

  private static final String EMPTY_COMPONENT = "{\"translate\":\"\"}";
  private static final HeaderAndFooterPacket RESET = new HeaderAndFooterPacket();

  private final String header;
  private final String footer;

  public HeaderAndFooterPacket() {
    this(EMPTY_COMPONENT, EMPTY_COMPONENT);
  }

  public HeaderAndFooterPacket(String header, String footer) {
    this.header = Preconditions.checkNotNull(header, "header");
    this.footer = Preconditions.checkNotNull(footer, "footer");
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    writeString(buf, header);
    writeString(buf, footer);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public String getHeader() {
    return header;
  }

  public String getFooter() {
    return footer;
  }

  public static HeaderAndFooterPacket reset() {
    return RESET;
  }
}
