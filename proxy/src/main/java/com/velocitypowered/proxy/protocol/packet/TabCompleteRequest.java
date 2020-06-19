package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TabCompleteRequest implements MinecraftPacket {

  private static final int VANILLA_MAX_TAB_COMPLETE_LEN = 2048;

  private @Nullable String command;
  private int transactionId;
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

  public boolean hasPosition() {
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

  public int getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(int transactionId) {
    this.transactionId = transactionId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("command", command)
        .add("transactionId", transactionId)
        .add("assumeCommand", assumeCommand)
        .add("hasPosition", hasPosition)
        .add("position", position)
        .toString();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (version.compareTo(MINECRAFT_1_13) >= 0) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
    } else {
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
      if (version.compareTo(MINECRAFT_1_9) >= 0) {
        this.assumeCommand = buf.readBoolean();
      }
      if (version.compareTo(MINECRAFT_1_8) >= 0) {
        this.hasPosition = buf.readBoolean();
        if (hasPosition) {
          this.position = buf.readLong();
        }
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }

    if (version.compareTo(MINECRAFT_1_13) >= 0) {
      ProtocolUtils.writeVarInt(buf, transactionId);
      ProtocolUtils.writeString(buf, command);
    } else {
      ProtocolUtils.writeString(buf, command);
      if (version.compareTo(MINECRAFT_1_9) >= 0) {
        buf.writeBoolean(assumeCommand);
      }
      if (version.compareTo(MINECRAFT_1_8) >= 0) {
        buf.writeBoolean(hasPosition);
        if (hasPosition) {
          buf.writeLong(position);
        }
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
