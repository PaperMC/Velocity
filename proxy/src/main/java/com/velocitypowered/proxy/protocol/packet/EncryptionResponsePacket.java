package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class EncryptionResponsePacket implements Packet {
  public static final Decoder<EncryptionResponsePacket> DECODER = Decoder.method(EncryptionResponsePacket::new);

  private byte[] sharedSecret = EMPTY_BYTE_ARRAY;
  private byte[] verifyToken = EMPTY_BYTE_ARRAY;

  public byte[] getSharedSecret() {
    return sharedSecret.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  @Override
  public String toString() {
    return "EncryptionResponsePacket{"
        + "sharedSecret=" + Arrays.toString(sharedSecret)
        + ", verifyToken=" + Arrays.toString(verifyToken)
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      this.sharedSecret = ProtocolUtils.readByteArray(buf, 256);
      this.verifyToken = ProtocolUtils.readByteArray(buf, 128);
    } else {
      this.sharedSecret = ProtocolUtils.readByteArray17(buf);
      this.verifyToken = ProtocolUtils.readByteArray17(buf);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeByteArray(buf, sharedSecret);
      ProtocolUtils.writeByteArray(buf, verifyToken);
    } else {
      ProtocolUtils.writeByteArray17(sharedSecret, buf, false);
      ProtocolUtils.writeByteArray17(verifyToken, buf, false);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
