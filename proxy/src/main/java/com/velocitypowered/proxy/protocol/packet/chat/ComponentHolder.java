/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LazilyParsedNumber;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Represents a holder for components used in chat or other text-based data in the Minecraft
 * protocol.
 * This class supports various formats including JSON and NBT (Named Binary Tag) for storing and
 * transmitting text components.
 */
public class ComponentHolder {
  private static final Logger logger = LogManager.getLogger(ComponentHolder.class);
  public static final int DEFAULT_MAX_STRING_SIZE = 262143;

  private final ProtocolVersion version;
  private @MonotonicNonNull Component component;
  private @MonotonicNonNull String json;
  private @MonotonicNonNull BinaryTag binaryTag;

  public ComponentHolder(ProtocolVersion version, Component component) {
    this.version = version;
    this.component = component;
  }

  public ComponentHolder(ProtocolVersion version, String json) {
    this.version = version;
    this.json = json;
  }

  public ComponentHolder(ProtocolVersion version, BinaryTag binaryTag) {
    this.version = version;
    this.binaryTag = binaryTag;
  }

  /**
   * Retrieves the {@link Component} stored in this {@link ComponentHolder}.
   * If the component is not yet initialized, it will attempt to deserialize it from either
   * the JSON or NBT representation, depending on which is available.
   *
   * @return the {@link Component} stored in this holder
   * @throws IllegalStateException if both the JSON and binary representations fail to deserialize
   */
  public Component getComponent() {
    if (component == null) {
      if (json != null) {
        component = ProtocolUtils.getJsonChatSerializer(version).deserialize(json);
      } else if (binaryTag != null) {
        // TODO: replace this with adventure-text-serializer-nbt
        try {
          json = deserialize(binaryTag).toString();
          component = ProtocolUtils.getJsonChatSerializer(version).deserialize(json);
        } catch (Exception ex) {
          logger.error(
              "Error converting binary component to JSON component! "
                  + "Binary: " + binaryTag + " JSON: " + json, ex);
          throw ex;
        }
      }
    }
    return component;
  }

  /**
   * Retrieves the JSON representation of the {@link Component} stored in this
   * {@link ComponentHolder}.
   * If the JSON string is not yet initialized, it will serialize the component into a JSON string.
   *
   * @return the JSON string representing the {@link Component}
   */
  public String getJson() {
    if (json == null) {
      json = ProtocolUtils.getJsonChatSerializer(version).serialize(getComponent());
    }
    return json;
  }

  /**
   * Retrieves the NBT (Named Binary Tag) representation of the {@link Component} stored in this
   * {@link ComponentHolder}.
   * If the NBT tag is not yet initialized, it will serialize the component into an NBT
   * representation.
   *
   * @return the {@link BinaryTag} representing the {@link Component}
   */
  public BinaryTag getBinaryTag() {
    if (binaryTag == null) {
      // TODO: replace this with adventure-text-serializer-nbt
      binaryTag = serialize(ProtocolUtils.getJsonChatSerializer(version).serializeToTree(getComponent()));
    }
    return binaryTag;
  }

  /**
   * Serializes a {@link JsonElement} into a {@link BinaryTag} format.
   * This method converts JSON primitives (numbers, strings, booleans) and complex structures
   * (arrays, objects)
   * into their corresponding NBT representations.
   *
   * @param json the {@link JsonElement} to be serialized into a {@link BinaryTag}
   * @return the {@link BinaryTag} representing the serialized JSON element
   * @throws IllegalArgumentException if the JSON element is of an unsupported or unknown type
   */
  public static BinaryTag serialize(JsonElement json) {
    if (json instanceof JsonPrimitive jsonPrimitive) {
      if (jsonPrimitive.isNumber()) {
        final Number number = json.getAsNumber();

        return switch (number) {
          case Byte b -> ByteBinaryTag.byteBinaryTag(b);
          case Short s -> ShortBinaryTag.shortBinaryTag(s);
          case Integer i -> IntBinaryTag.intBinaryTag(i);
          case Long l -> LongBinaryTag.longBinaryTag(l);
          case Float f -> FloatBinaryTag.floatBinaryTag(f);
          case Double d -> DoubleBinaryTag.doubleBinaryTag(d);
          case LazilyParsedNumber l -> IntBinaryTag.intBinaryTag(l.intValue());
          default -> throw new IllegalArgumentException("Unknown number type: " + number);
        };
      } else if (jsonPrimitive.isString()) {
        return StringBinaryTag.stringBinaryTag(jsonPrimitive.getAsString());
      } else if (jsonPrimitive.isBoolean()) {
        return ByteBinaryTag.byteBinaryTag((byte) (jsonPrimitive.getAsBoolean() ? 1 : 0));
      } else {
        throw new IllegalArgumentException("Unknown JSON primitive: " + jsonPrimitive);
      }
    } else if (json instanceof JsonObject object) {
      CompoundBinaryTag.Builder compound = CompoundBinaryTag.builder();

      for (Map.Entry<String, JsonElement> property : object.entrySet()) {
        compound.put(property.getKey(), serialize(property.getValue()));
      }

      return compound.build();
    } else if (json instanceof JsonArray array) {
      List<JsonElement> jsonArray = array.asList();

      if (jsonArray.isEmpty()) {
        return ListBinaryTag.empty();
      }

      List<BinaryTag> tagItems = new ArrayList<>(jsonArray.size());
      BinaryTagType<? extends BinaryTag> listType = null;

      for (JsonElement jsonEl : jsonArray) {
        BinaryTag tag = serialize(jsonEl);
        tagItems.add(tag);

        if (listType == null) {
          listType = tag.type();
        } else if (listType != tag.type()) {
          listType = BinaryTagTypes.COMPOUND;
        }
      }

      switch (listType.id()) {
        case 1 -> { // BinaryTagTypes.BYTE:
          byte[] bytes = new byte[jsonArray.size()];
          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = jsonArray.get(i).getAsNumber().byteValue();
          }

          return ByteArrayBinaryTag.byteArrayBinaryTag(bytes);
        }
        case 3 -> { // BinaryTagTypes.INT:
          int[] ints = new int[jsonArray.size()];
          for (int i = 0; i < ints.length; i++) {
            ints[i] = jsonArray.get(i).getAsNumber().intValue();
          }

          return IntArrayBinaryTag.intArrayBinaryTag(ints);
        }
        case 4 -> { // BinaryTagTypes.LONG:
          long[] longs = new long[jsonArray.size()];
          for (int i = 0; i < longs.length; i++) {
            longs[i] = jsonArray.get(i).getAsNumber().longValue();
          }

          return LongArrayBinaryTag.longArrayBinaryTag(longs);
        }
        case 10 -> // BinaryTagTypes.COMPOUND:
            tagItems.replaceAll(tag -> {
              if (tag.type() == BinaryTagTypes.COMPOUND) {
                return tag;
              } else {
                return CompoundBinaryTag.builder().put("", tag).build();
              }
            });
        default -> {
        }
      }

      return ListBinaryTag.listBinaryTag(listType, tagItems);
    }

    return EndBinaryTag.endBinaryTag();
  }

  /**
   * Deserializes a {@link BinaryTag} into a {@link JsonElement}.
   * This method converts NBT (Named Binary Tag) data into its corresponding JSON representation,
   * including handling of primitive types, arrays, and compound structures.
   *
   * @param tag the {@link BinaryTag} to be deserialized into a {@link JsonElement}
   * @return the {@link JsonElement} representing the deserialized NBT data
   * @throws IllegalArgumentException if the NBT tag type is unsupported or unknown
   */
  public static JsonElement deserialize(BinaryTag tag) {
    return switch (tag.type().id()) {
      // BinaryTagTypes.BYTE
      case 1 -> new JsonPrimitive(((ByteBinaryTag) tag).value());
      // BinaryTagTypes.SHORT
      case 2 -> new JsonPrimitive(((ShortBinaryTag) tag).value());
      // BinaryTagTypes.INT:
      case 3 -> new JsonPrimitive(((IntBinaryTag) tag).value());
      // BinaryTagTypes.LONG:
      case 4 -> new JsonPrimitive(((LongBinaryTag) tag).value());
      // BinaryTagTypes.FLOAT:
      case 5 -> new JsonPrimitive(((FloatBinaryTag) tag).value());
      // BinaryTagTypes.DOUBLE:
      case 6 -> new JsonPrimitive(((DoubleBinaryTag) tag).value());
      // BinaryTagTypes.BYTE_ARRAY:
      case 7 -> {
        byte[] byteArray = ((ByteArrayBinaryTag) tag).value();

        JsonArray jsonByteArray = new JsonArray(byteArray.length);
        for (byte b : byteArray) {
          jsonByteArray.add(new JsonPrimitive(b));
        }

        yield jsonByteArray;
      }
      // BinaryTagTypes.STRING:
      case 8 -> new JsonPrimitive(((StringBinaryTag) tag).value());
      // BinaryTagTypes.LIST:
      case 9 -> {
        ListBinaryTag items = (ListBinaryTag) tag;
        JsonArray jsonList = new JsonArray(items.size());

        for (BinaryTag subTag : items) {
          jsonList.add(deserialize(subTag));
        }

        yield jsonList;
      }
      // BinaryTagTypes.COMPOUND:
      case 10 -> {
        CompoundBinaryTag compound = (CompoundBinaryTag) tag;
        JsonObject jsonObject = new JsonObject();

        compound.keySet().forEach(key -> {
          // [{"text":"test1"},"test2"] can't be represented as a binary list tag
          // it is represented by a list tag with two compound tags
          // the second compound tag will have an empty key mapped to "test2"
          // without this fix this would lead to an invalid json component:
          // [{"text":"test1"},{"":"test2"}]
          jsonObject.add(key.isEmpty() ? "text" : key, deserialize(compound.get(key)));
        });

        yield jsonObject;
      }
      // BinaryTagTypes.INT_ARRAY:
      case 11 -> {
        int[] intArray = ((IntArrayBinaryTag) tag).value();

        JsonArray jsonIntArray = new JsonArray(intArray.length);
        for (int i : intArray) {
          jsonIntArray.add(new JsonPrimitive(i));
        }

        yield jsonIntArray;
      }
      // BinaryTagTypes.LONG_ARRAY:
      case 12 -> {
        long[] longArray = ((LongArrayBinaryTag) tag).value();

        JsonArray jsonLongArray = new JsonArray(longArray.length);
        for (long l : longArray) {
          jsonLongArray.add(new JsonPrimitive(l));
        }

        yield jsonLongArray;
      }
      default -> throw new IllegalArgumentException("Unknown NBT tag: " + tag);
    };
  }

  /**
   * Reads a {@link ComponentHolder} from the provided {@link ByteBuf} using the specified
   * {@link ProtocolVersion}.
   * This method deserializes a component from either its binary (NBT) or JSON representation,
   * depending on the protocol version.
   * - For Minecraft versions 1.20.3 and later, it reads a binary tag.
   * - For Minecraft versions 1.13 and later, it reads a JSON string with a size limit.
   * - For earlier versions, it reads a standard JSON string.
   *
   * @param buf the {@link ByteBuf} containing the serialized component data
   * @param version the {@link ProtocolVersion} indicating how the component should be deserialized
   * @return a {@link ComponentHolder} containing the deserialized component
   */
  public static ComponentHolder read(ByteBuf buf, ProtocolVersion version) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      return new ComponentHolder(version,
          ProtocolUtils.readBinaryTag(buf, version, BinaryTagIO.reader()));
    } else if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
      return new ComponentHolder(version, ProtocolUtils.readString(buf, DEFAULT_MAX_STRING_SIZE));
    } else {
      return new ComponentHolder(version, ProtocolUtils.readString(buf));
    }
  }

  /**
   * Writes the {@link ComponentHolder}'s data to the provided {@link ByteBuf}.
   * This method serializes the component into either its binary (NBT) or JSON representation
   * based on the protocol version.
   * - For Minecraft versions 1.20.3 and later, it writes the component as a binary tag (NBT).
   * - For earlier versions, it writes the component as a JSON string.
   *
   * @param buf the {@link ByteBuf} where the component data will be written
   */
  public void write(ByteBuf buf) {
    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      ProtocolUtils.writeBinaryTag(buf, version, getBinaryTag());
    } else {
      ProtocolUtils.writeString(buf, getJson());
    }
  }
}
