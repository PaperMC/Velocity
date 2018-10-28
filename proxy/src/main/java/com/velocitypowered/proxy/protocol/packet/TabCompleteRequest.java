package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.MINECRAFT_1_9;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TabCompleteRequest implements MinecraftPacket {

  private @Nullable String command;
  private boolean assumeCommand;
  private boolean hasPosition;
  private long position;

  public String getCommand() {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public boolean isAssumeCommand() {
    return assumeCommand;
  }

  public void setAssumeCommand(boolean assumeCommand) {
    this.assumeCommand = assumeCommand;
  }

  public boolean isHasPosition() {
    return hasPosition;
  }

  public void setHasPosition(boolean hasPosition) {
    this.hasPosition = hasPosition;
  }

  public long getPosition() {
    return position;
  }

  public void setPosition(long position) {
    this.position = position;
  }

  @Override
  public String toString() {
    return "TabCompleteRequest{" +
        "command='" + command + '\'' +
        ", assumeCommand=" + assumeCommand +
        ", hasPosition=" + hasPosition +
        ", position=" + position +
        '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
    this.command = ProtocolUtils.readString(buf);
    if (protocolVersion >= MINECRAFT_1_9) {
      this.assumeCommand = buf.readBoolean();
    }
    this.hasPosition = buf.readBoolean();
    if (hasPosition) {
      this.position = buf.readLong();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }
    ProtocolUtils.writeString(buf, command);
    if (protocolVersion >= MINECRAFT_1_9) {
      buf.writeBoolean(assumeCommand);
    }
    buf.writeBoolean(hasPosition);
    if (hasPosition) {
      buf.writeLong(position);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
