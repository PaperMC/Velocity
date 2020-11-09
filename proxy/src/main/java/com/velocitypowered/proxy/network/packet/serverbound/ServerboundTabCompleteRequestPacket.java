package com.velocitypowered.proxy.network.packet.serverbound;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerboundTabCompleteRequestPacket implements Packet {
  public static final PacketReader<ServerboundTabCompleteRequestPacket> DECODER = PacketReader.method(ServerboundTabCompleteRequestPacket::new);

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
  public void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (version.gte(MINECRAFT_1_13)) {
      this.transactionId = ProtocolUtils.readVarInt(buf);
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
    } else {
      this.command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
      if (version.gte(MINECRAFT_1_9)) {
        this.assumeCommand = buf.readBoolean();
      }
      if (version.gte(MINECRAFT_1_8)) {
        this.hasPosition = buf.readBoolean();
        if (hasPosition) {
          this.position = buf.readLong();
        }
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }

    if (version.gte(MINECRAFT_1_13)) {
      ProtocolUtils.writeVarInt(buf, transactionId);
      ProtocolUtils.writeString(buf, command);
    } else {
      ProtocolUtils.writeString(buf, command);
      if (version.gte(MINECRAFT_1_9)) {
        buf.writeBoolean(assumeCommand);
      }
      if (version.gte(MINECRAFT_1_8)) {
        buf.writeBoolean(hasPosition);
        if (hasPosition) {
          buf.writeLong(position);
        }
      }
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
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
}
