package com.velocitypowered.proxy.network.serialization.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.proxy.network.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Serializes properties for {@link StringArgumentType}.
 */
class StringArgumentPropertySerializer implements ArgumentPropertySerializer<StringArgumentType> {

  public static final ArgumentPropertySerializer<StringArgumentType> STRING =
      new StringArgumentPropertySerializer();

  private StringArgumentPropertySerializer() {

  }

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
