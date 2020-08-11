package com.velocitypowered.proxy.protocol.packet.brigadier;

import static com.velocitypowered.proxy.protocol.packet.brigadier.DoubleArgumentPropertySerializer.DOUBLE;
import static com.velocitypowered.proxy.protocol.packet.brigadier.DummyVoidArgumentPropertySerializer.DUMMY;
import static com.velocitypowered.proxy.protocol.packet.brigadier.FloatArgumentPropertySerializer.FLOAT;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.INTEGER;
import static com.velocitypowered.proxy.protocol.packet.brigadier.LongArgumentPropertySerializer.LONG;
import static com.velocitypowered.proxy.protocol.packet.brigadier.StringArgumentPropertySerializer.STRING;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

public class ArgumentPropertyRegistry {
  private ArgumentPropertyRegistry() {
    throw new AssertionError();
  }

  private static final Map<String, ArgumentPropertySerializer<?>> byId = new HashMap<>();
  private static final Map<Class<? extends ArgumentType>,
      ArgumentPropertySerializer<?>> byClass = new HashMap<>();
  private static final Map<Class<? extends ArgumentType>, String> classToId = new HashMap<>();

  private static <T extends ArgumentType<?>> void register(String identifier, Class<T> klazz,
      ArgumentPropertySerializer<T> serializer) {
    byId.put(identifier, serializer);
    byClass.put(klazz, serializer);
    classToId.put(klazz, identifier);
  }

  private static <T> void dummy(String identifier, ArgumentPropertySerializer<T> serializer) {
    byId.put(identifier, serializer);
  }

  /**
   * Deserializes the {@link ArgumentType}.
   * @param buf the buffer to deserialize
   * @return the deserialized {@link ArgumentType}
   */
  public static ArgumentType<?> deserialize(ByteBuf buf) {
    String identifier = ProtocolUtils.readString(buf);
    ArgumentPropertySerializer<?> serializer = byId.get(identifier);
    if (serializer == null) {
      throw new IllegalArgumentException("Argument type identifier " + identifier + " unknown.");
    }
    Object result = serializer.deserialize(buf);

    if (result instanceof ArgumentType) {
      return (ArgumentType<?>) result;
    } else {
      return new DummyProperty(identifier, serializer, result);
    }
  }

  /**
   * Serializes the {@code type} into the provided {@code buf}.
   * @param buf the buffer to serialize into
   * @param type the type to serialize
   */
  public static void serialize(ByteBuf buf, ArgumentType<?> type) {
    if (type instanceof DummyProperty) {
      DummyProperty property = (DummyProperty) type;
      ProtocolUtils.writeString(buf, property.getIdentifier());
      if (property.getResult() != null) {
        property.getSerializer().serialize(property.getResult(), buf);
      }
    } else {
      ArgumentPropertySerializer serializer = byClass.get(type.getClass());
      String id = classToId.get(type.getClass());
      if (serializer == null || id == null) {
        throw new IllegalArgumentException("Don't know how to serialize "
            + type.getClass().getName());
      }
      ProtocolUtils.writeString(buf, id);
      serializer.serialize(type, buf);
    }
  }

  static {
    // Base Brigadier argument types
    register("brigadier:string", StringArgumentType.class, STRING);
    register("brigadier:integer", IntegerArgumentType.class, INTEGER);
    register("brigadier:float", FloatArgumentType.class, FLOAT);
    register("brigadier:double", DoubleArgumentType.class, DOUBLE);
    register("brigadier:bool", BoolArgumentType.class,
        VoidArgumentPropertySerializer.create(BoolArgumentType::bool));
    register("brigadier:long", LongArgumentType.class, LONG);

    // Minecraft argument types with extra properties
    dummy("minecraft:entity", ByteArgumentPropertySerializer.BYTE);
    dummy("minecraft:score_holder", ByteArgumentPropertySerializer.BYTE);

    // Minecraft argument types
    dummy("minecraft:game_profile", DUMMY);
    dummy("minecraft:block_pos", DUMMY);
    dummy("minecraft:column_pos", DUMMY);
    dummy("minecraft:vec3", DUMMY);
    dummy("minecraft:vec2", DUMMY);
    dummy("minecraft:block_state", DUMMY);
    dummy("minecraft:block_predicate", DUMMY);
    dummy("minecraft:item_stack", DUMMY);
    dummy("minecraft:item_predicate", DUMMY);
    dummy("minecraft:color", DUMMY);
    dummy("minecraft:component", DUMMY);
    dummy("minecraft:message", DUMMY);
    dummy("minecraft:nbt", DUMMY);
    dummy("minecraft:nbt_compound_tag", DUMMY); // added in 1.14
    dummy("minecraft:nbt_tag", DUMMY); // added in 1.14
    dummy("minecraft:nbt_path", DUMMY);
    dummy("minecraft:objective", DUMMY);
    dummy("minecraft:objective_criteria", DUMMY);
    dummy("minecraft:operation", DUMMY);
    dummy("minecraft:particle", DUMMY);
    dummy("minecraft:rotation", DUMMY);
    dummy("minecraft:scoreboard_slot", DUMMY);
    dummy("minecraft:swizzle", DUMMY);
    dummy("minecraft:team", DUMMY);
    dummy("minecraft:item_slot", DUMMY);
    dummy("minecraft:resource_location", DUMMY);
    dummy("minecraft:mob_effect", DUMMY);
    dummy("minecraft:function", DUMMY);
    dummy("minecraft:entity_anchor", DUMMY);
    dummy("minecraft:item_enchantment", DUMMY);
    dummy("minecraft:entity_summon", DUMMY);
    dummy("minecraft:dimension", DUMMY);
    dummy("minecraft:int_range", DUMMY);
    dummy("minecraft:float_range", DUMMY);
    dummy("minecraft:time", DUMMY); // added in 1.14
    dummy("minecraft:uuid", DUMMY); // added in 1.16
    dummy("minecraft:angle", DUMMY); // added in 1.16.2
  }
}
