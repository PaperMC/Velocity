package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Serializes properties for {@link StringArgumentType}.
 */
class StringArgumentPropertySerializer implements ArgumentPropertySerializer<StringArgumentType> {

  public static final ArgumentPropertySerializer<StringArgumentType> STRING =
      new StringArgumentPropertySerializer();

  private StringArgumentPropertySerializer() {

  }

  @Nullable
  @Override
  public StringArgumentType deserialize(ByteBuf buf) {
    int type = ProtocolUtils.readVarInt(buf);
    switch (type) {
      case 0:
        return StringArgumentType.word();
      case 1:
        return StringArgumentType.string();
      case 2:
        return StringArgumentType.greedyString();
      default:
        throw new IllegalArgumentException("Invalid string argument type " + type);
    }
  }

  @Override
  public void serialize(StringArgumentType object, ByteBuf buf) {
    switch (object.getType()) {
      case SINGLE_WORD:
        ProtocolUtils.writeVarInt(buf, 0);
        break;
      case QUOTABLE_PHRASE:
        ProtocolUtils.writeVarInt(buf, 1);
        break;
      case GREEDY_PHRASE:
        ProtocolUtils.writeVarInt(buf, 2);
        break;
      default:
        throw new IllegalArgumentException("Invalid string argument type " + object.getType());
    }
  }
}
