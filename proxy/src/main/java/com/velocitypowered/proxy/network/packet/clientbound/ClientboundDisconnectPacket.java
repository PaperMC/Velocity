package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundDisconnectPacket implements Packet {
  public static final Decoder<ClientboundDisconnectPacket> DECODER = Decoder.method(ClientboundDisconnectPacket::new);

  private @Nullable String reason;

  public ClientboundDisconnectPacket() {
  }

  public ClientboundDisconnectPacket(String reason) {
    this.reason = Preconditions.checkNotNull(reason, "reason");
  }

  public String getReason() {
    if (reason == null) {
      throw new IllegalStateException("No reason specified");
    }
    return reason;
  }

  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  @Override
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    reason = ProtocolUtils.readString(buf);
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (reason == null) {
      throw new IllegalStateException("No reason specified.");
    }
    ProtocolUtils.writeString(buf, reason);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public static ClientboundDisconnectPacket create(Component component, ProtocolVersion version) {
    Preconditions.checkNotNull(component, "component");
    return new ClientboundDisconnectPacket(ProtocolUtils.getJsonChatSerializer(version).serialize(component));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("reason", this.reason)
      .toString();
  }
}
