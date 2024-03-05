/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.protocol.packet.brigadier;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_3;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;
import static com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier.id;
import static com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier.mapSet;
import static com.velocitypowered.proxy.protocol.packet.brigadier.DoubleArgumentPropertySerializer.DOUBLE;
import static com.velocitypowered.proxy.protocol.packet.brigadier.EmptyArgumentPropertySerializer.EMPTY;
import static com.velocitypowered.proxy.protocol.packet.brigadier.FloatArgumentPropertySerializer.FLOAT;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.INTEGER;
import static com.velocitypowered.proxy.protocol.packet.brigadier.LongArgumentPropertySerializer.LONG;
import static com.velocitypowered.proxy.protocol.packet.brigadier.ModArgumentPropertySerializer.MOD;
import static com.velocitypowered.proxy.protocol.packet.brigadier.StringArgumentPropertySerializer.STRING;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

public class ArgumentPropertyRegistry {

  private ArgumentPropertyRegistry() {
    throw new AssertionError();
  }

  private static final Map<ArgumentIdentifier, ArgumentPropertySerializer<?>> byIdentifier =
      new HashMap<>();
  private static final Map<Class<? extends ArgumentType>,
      ArgumentPropertySerializer<?>> byClass = new HashMap<>();
  private static final Map<Class<? extends ArgumentType>, ArgumentIdentifier> classToId =
      new HashMap<>();

  private static <T extends ArgumentType<?>> void register(ArgumentIdentifier identifier,
      Class<T> klazz, ArgumentPropertySerializer<T> serializer) {
    byIdentifier.put(identifier, serializer);
    byClass.put(klazz, serializer);
    classToId.put(klazz, identifier);
  }

  private static <T> void empty(ArgumentIdentifier identifier) {
    empty(identifier, EMPTY);
  }

  private static <T> void empty(ArgumentIdentifier identifier,
      ArgumentPropertySerializer<T> serializer) {
    byIdentifier.put(identifier, serializer);
  }

  /**
   * Deserializes the {@link ArgumentType}.
   *
   * @param buf the buffer to deserialize
   * @return the deserialized {@link ArgumentType}
   */
  public static ArgumentType<?> deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    ArgumentIdentifier identifier = readIdentifier(buf, protocolVersion);

    ArgumentPropertySerializer<?> serializer = byIdentifier.get(identifier);
    if (serializer == null) {
      throw new IllegalArgumentException("Argument type identifier " + identifier + " unknown.");
    }
    Object result = serializer.deserialize(buf, protocolVersion);

    if (result instanceof ArgumentType) {
      return (ArgumentType<?>) result;
    } else {
      return new PassthroughProperty(identifier, serializer, result);
    }
  }

  /**
   * Serializes the {@code type} into the provided {@code buf}.
   *
   * @param buf  the buffer to serialize into
   * @param type the type to serialize
   */
  public static void serialize(ByteBuf buf, ArgumentType<?> type,
      ProtocolVersion protocolVersion) {
    if (type instanceof PassthroughProperty) {
      PassthroughProperty property = (PassthroughProperty) type;
      writeIdentifier(buf, property.getIdentifier(), protocolVersion);
      if (property.getResult() != null) {
        property.getSerializer().serialize(property.getResult(), buf, protocolVersion);
      }
    } else if (type instanceof ModArgumentProperty) {
      ModArgumentProperty property = (ModArgumentProperty) type;
      writeIdentifier(buf, property.getIdentifier(), protocolVersion);
      buf.writeBytes(property.getData());
    } else {
      ArgumentPropertySerializer serializer = byClass.get(type.getClass());
      ArgumentIdentifier id = classToId.get(type.getClass());
      if (serializer == null || id == null) {
        throw new IllegalArgumentException("Don't know how to serialize "
            + type.getClass().getName());
      }
      writeIdentifier(buf, id, protocolVersion);
      serializer.serialize(type, buf, protocolVersion);
    }
  }

  /**
   * Writes the {@link ArgumentIdentifier} to a version-specific buffer.
   *
   * @param buf             the buffer to write to
   * @param identifier      the identifier to write
   * @param protocolVersion the protocol version to use
   */
  public static void writeIdentifier(ByteBuf buf, ArgumentIdentifier identifier,
      ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(MINECRAFT_1_19)) {
      Integer id = identifier.getIdByProtocolVersion(protocolVersion);
      Preconditions.checkNotNull(id, "Don't know how to serialize type " + identifier);

      ProtocolUtils.writeVarInt(buf, id);
    } else {
      ProtocolUtils.writeString(buf, identifier.getIdentifier());
    }

  }

  /**
   * Reads the {@link ArgumentIdentifier} from a version-specific buffer.
   *
   * @param buf             the buffer to write to
   * @param protocolVersion the protocol version to use
   * @return the identifier read from the buffer
   */
  public static ArgumentIdentifier readIdentifier(ByteBuf buf, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(MINECRAFT_1_19)) {
      int id = ProtocolUtils.readVarInt(buf);
      for (ArgumentIdentifier i : byIdentifier.keySet()) {
        Integer v = i.getIdByProtocolVersion(protocolVersion);
        if (v != null && v == id) {
          return i;
        }
      }
    } else {
      String identifier = ProtocolUtils.readString(buf);
      for (ArgumentIdentifier i : byIdentifier.keySet()) {
        if (i.getIdentifier().equals(identifier)) {
          return i;
        }
      }
    }
    return null;
  }

  static {
    // Base Brigadier argument types
    register(id("brigadier:bool", mapSet(MINECRAFT_1_19, 0)), BoolArgumentType.class,
        new ArgumentPropertySerializer<>() {
          @Override
          public BoolArgumentType deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
            return BoolArgumentType.bool();
          }

          @Override
          public void serialize(BoolArgumentType object, ByteBuf buf,
              ProtocolVersion protocolVersion) {

          }
        });
    register(id("brigadier:float", mapSet(MINECRAFT_1_19, 1)), FloatArgumentType.class, FLOAT);
    register(id("brigadier:double", mapSet(MINECRAFT_1_19, 2)), DoubleArgumentType.class, DOUBLE);
    register(id("brigadier:integer", mapSet(MINECRAFT_1_19, 3)), IntegerArgumentType.class,
        INTEGER);
    register(id("brigadier:long", mapSet(MINECRAFT_1_19, 4)), LongArgumentType.class, LONG);
    register(id("brigadier:string", mapSet(MINECRAFT_1_19, 5)), StringArgumentType.class, STRING);

    empty(id("minecraft:entity", mapSet(MINECRAFT_1_19, 6)), ByteArgumentPropertySerializer.BYTE);
    empty(id("minecraft:game_profile", mapSet(MINECRAFT_1_19, 7)));
    empty(id("minecraft:block_pos", mapSet(MINECRAFT_1_19, 8)));
    empty(id("minecraft:column_pos", mapSet(MINECRAFT_1_19, 9)));
    empty(id("minecraft:vec3", mapSet(MINECRAFT_1_19, 10)));
    empty(id("minecraft:vec2", mapSet(MINECRAFT_1_19, 11)));
    empty(id("minecraft:block_state", mapSet(MINECRAFT_1_19, 12)));
    empty(id("minecraft:block_predicate", mapSet(MINECRAFT_1_19, 13)));
    empty(id("minecraft:item_stack", mapSet(MINECRAFT_1_19, 14)));
    empty(id("minecraft:item_predicate", mapSet(MINECRAFT_1_19, 15)));
    empty(id("minecraft:color", mapSet(MINECRAFT_1_19, 16)));
    empty(id("minecraft:component", mapSet(MINECRAFT_1_19, 17)));
    empty(id("minecraft:style", mapSet(MINECRAFT_1_20_3, 18))); // added 1.20.3
    empty(id("minecraft:message", mapSet(MINECRAFT_1_20_3, 19), mapSet(MINECRAFT_1_19, 18)));
    empty(id("minecraft:nbt_compound_tag", mapSet(MINECRAFT_1_20_3, 20),
        mapSet(MINECRAFT_1_19, 19))); // added in 1.14
    empty(id("minecraft:nbt_tag", mapSet(MINECRAFT_1_20_3, 21),
        mapSet(MINECRAFT_1_19, 20))); // added in 1.14
    empty(id("minecraft:nbt_path", mapSet(MINECRAFT_1_20_3, 22), mapSet(MINECRAFT_1_19, 21)));
    empty(id("minecraft:objective", mapSet(MINECRAFT_1_20_3, 23), mapSet(MINECRAFT_1_19, 22)));
    empty(id("minecraft:objective_criteria", mapSet(MINECRAFT_1_20_3, 24),
        mapSet(MINECRAFT_1_19, 23)));
    empty(id("minecraft:operation", mapSet(MINECRAFT_1_20_3, 25), mapSet(MINECRAFT_1_19, 24)));
    empty(id("minecraft:particle", mapSet(MINECRAFT_1_20_3, 26), mapSet(MINECRAFT_1_19, 25)));
    empty(id("minecraft:angle", mapSet(MINECRAFT_1_20_3, 27),
        mapSet(MINECRAFT_1_19, 26))); // added in 1.16.2
    empty(id("minecraft:rotation", mapSet(MINECRAFT_1_20_3, 28), mapSet(MINECRAFT_1_19, 27)));
    empty(
        id("minecraft:scoreboard_slot", mapSet(MINECRAFT_1_20_3, 29), mapSet(MINECRAFT_1_19, 28)));
    empty(id("minecraft:score_holder", mapSet(MINECRAFT_1_20_3, 30), mapSet(MINECRAFT_1_19, 29)),
        ByteArgumentPropertySerializer.BYTE);
    empty(id("minecraft:swizzle", mapSet(MINECRAFT_1_20_3, 31), mapSet(MINECRAFT_1_19, 30)));
    empty(id("minecraft:team", mapSet(MINECRAFT_1_20_3, 32), mapSet(MINECRAFT_1_19, 31)));
    empty(id("minecraft:item_slot", mapSet(MINECRAFT_1_20_3, 33), mapSet(MINECRAFT_1_19, 32)));
    empty(id("minecraft:resource_location", mapSet(MINECRAFT_1_20_3, 34),
        mapSet(MINECRAFT_1_19, 33)));
    empty(id("minecraft:mob_effect", mapSet(MINECRAFT_1_19_3, -1), mapSet(MINECRAFT_1_19, 34)));
    empty(id("minecraft:function", mapSet(MINECRAFT_1_20_3, 35), mapSet(MINECRAFT_1_19_3, 34),
        mapSet(MINECRAFT_1_19, 35)));
    empty(id("minecraft:entity_anchor", mapSet(MINECRAFT_1_20_3, 36), mapSet(MINECRAFT_1_19_3, 35),
        mapSet(MINECRAFT_1_19, 36)));
    empty(id("minecraft:int_range", mapSet(MINECRAFT_1_20_3, 37), mapSet(MINECRAFT_1_19_3, 36),
        mapSet(MINECRAFT_1_19, 37)));
    empty(id("minecraft:float_range", mapSet(MINECRAFT_1_20_3, 38), mapSet(MINECRAFT_1_19_3, 37),
        mapSet(MINECRAFT_1_19, 38)));
    empty(
        id("minecraft:item_enchantment", mapSet(MINECRAFT_1_19_3, -1), mapSet(MINECRAFT_1_19, 39)));
    empty(id("minecraft:entity_summon", mapSet(MINECRAFT_1_19_3, -1), mapSet(MINECRAFT_1_19, 40)));
    empty(id("minecraft:dimension", mapSet(MINECRAFT_1_20_3, 39), mapSet(MINECRAFT_1_19_3, 38),
        mapSet(MINECRAFT_1_19, 41)));
    empty(id("minecraft:gamemode", mapSet(MINECRAFT_1_20_3, 40),
        mapSet(MINECRAFT_1_19_3, 39))); // 1.19.3

    empty(id("minecraft:time", mapSet(MINECRAFT_1_20_3, 41), mapSet(MINECRAFT_1_19_3, 40),
        mapSet(MINECRAFT_1_19, 42)), TimeArgumentSerializer.TIME); // added in 1.14

    register(
        id("minecraft:resource_or_tag", mapSet(MINECRAFT_1_20_3, 42), mapSet(MINECRAFT_1_19_3, 41),
            mapSet(MINECRAFT_1_19, 43)),
        RegistryKeyArgument.class, RegistryKeyArgumentSerializer.REGISTRY);
    register(id("minecraft:resource_or_tag_key", mapSet(MINECRAFT_1_20_3, 43),
            mapSet(MINECRAFT_1_19_3, 42)),
        RegistryKeyArgumentList.ResourceOrTagKey.class,
        RegistryKeyArgumentList.ResourceOrTagKey.Serializer.REGISTRY);
    register(id("minecraft:resource", mapSet(MINECRAFT_1_20_3, 44), mapSet(MINECRAFT_1_19_3, 43),
            mapSet(MINECRAFT_1_19, 44)),
        RegistryKeyArgument.class, RegistryKeyArgumentSerializer.REGISTRY);
    register(
        id("minecraft:resource_key", mapSet(MINECRAFT_1_20_3, 45), mapSet(MINECRAFT_1_19_3, 44)),
        RegistryKeyArgumentList.ResourceKey.class,
        RegistryKeyArgumentList.ResourceKey.Serializer.REGISTRY);

    empty(id("minecraft:template_mirror", mapSet(MINECRAFT_1_20_3, 46),
        mapSet(MINECRAFT_1_19, 45))); // 1.19
    empty(id("minecraft:template_rotation", mapSet(MINECRAFT_1_20_3, 47),
        mapSet(MINECRAFT_1_19, 46))); // 1.19
    empty(id("minecraft:heightmap", mapSet(MINECRAFT_1_20_3, 49),
        mapSet(MINECRAFT_1_19_4, 47))); // 1.19.4

    empty(id("minecraft:uuid", mapSet(MINECRAFT_1_20_3, 48), mapSet(MINECRAFT_1_19_4, 48),
        mapSet(MINECRAFT_1_19, 47))); // added in 1.16

    // Crossstitch support
    register(id("crossstitch:mod_argument", mapSet(MINECRAFT_1_19, -256)),
        ModArgumentProperty.class, MOD);

    empty(id("minecraft:nbt")); // No longer in 1.19+
  }
}