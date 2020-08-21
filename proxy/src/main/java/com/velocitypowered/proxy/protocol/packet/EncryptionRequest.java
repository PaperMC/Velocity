package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class EncryptionRequest implements MinecraftPacket {

  private String serverId = "";
  private byte[] publicKey = EMPTY_BYTE_ARRAY;
  private byte[] verifyToken = EMPTY_BYTE_ARRAY;

  public byte[] getPublicKey() {
    return publicKey.clone();
  }

  public void setPublicKey(byte[] publicKey) {
    this.publicKey = publicKey.clone();
  }

  public byte[] getVerifyToken() {
    return verifyToken.clone();
  }

  public void setVerifyToken(byte[] verifyToken) {
    this.verifyToken = verifyToken.clone();
  }

  @Override
  public String toString() {
    return "EncryptionRequest{"
        + "publicKey=" + Arrays.toString(publicKey)
        + ", verifyToken=" + Arrays.toString(verifyToken)
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.serverId = ProtocolUtils.readString(buf, 20);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      publicKey = ProtocolUtils.readByteArray(buf, 256);
      verifyToken = ProtocolUtils.readByteArray(buf, 16);
    } else {
      publicKey = ProtocolUtils.readByteArray17(buf);
      verifyToken = ProtocolUtils.readByteArray17(buf);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, this.serverId);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ProtocolUtils.writeByteArray(buf, publicKey);
      ProtocolUtils.writeByteArray(buf, verifyToken);
    } else {
      ProtocolUtils.writeByteArray17(publicKey, buf, false);
      ProtocolUtils.writeByteArray17(verifyToken, buf, false);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
