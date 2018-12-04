package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class EncryptionResponse implements MinecraftPacket {

  private byte[] sharedSecret = EMPTY_BYTE_ARRAY;
  private byte[] verifyToken = EMPTY_BYTE_ARRAY;

  public byte[] getSharedSecret() {
    return sharedSecret;
  }

  public void setSharedSecret(byte[] sharedSecret) {
    this.sharedSecret = sharedSecret;
  }

  public byte[] getVerifyToken() {
    return verifyToken;
  }

  public void setVerifyToken(byte[] verifyToken) {
    this.verifyToken = verifyToken;
  }

  @Override
  public String toString() {
    return "EncryptionResponse{"
        + "sharedSecret=" + Arrays.toString(sharedSecret)
        + ", verifyToken=" + Arrays.toString(verifyToken)
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    this.sharedSecret = ProtocolUtils.readByteArray(buf, 256);
    this.verifyToken = ProtocolUtils.readByteArray(buf, 128);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeByteArray(buf, sharedSecret);
    ProtocolUtils.writeByteArray(buf, verifyToken);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
