/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.HeaderData;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerPlayerChat implements MinecraftPacket {

  private static final QuietRuntimeException BODY_HASHING_ERROR
          = new QuietRuntimeException("Cannot hash message body");

  private SignaturePair header;
  private byte[] headerSignature;

  private String message;
  private @Nullable Component previewed;
  private Instant expiry;
  private long salt;
  private SignaturePair[] lastSeen;
  private @Nullable Component unsignedContent;
  
  private FilterType filter;
  private long @Nullable[] mask;

  private int chatType;
  private Component name;
  private @Nullable Component targetName;

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    header = ProtocolUtils.readSignatureHeader(buf);
    headerSignature = ProtocolUtils.readByteArray(buf);

    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);
    message = ProtocolUtils.readString(buf, 256);
    previewed = buf.readBoolean() ? serializer.deserialize(ProtocolUtils.readString(buf)) : null;
    expiry = Instant.ofEpochMilli(buf.readLong());
    salt = buf.readLong();
    lastSeen = ProtocolUtils.readSignaturePairArray(buf, 5);
    unsignedContent = buf.readBoolean() ? serializer.deserialize(ProtocolUtils.readString(buf)) : null;
    
    filter = FilterType.values()[ProtocolUtils.readVarInt(buf)];
    if (filter == FilterType.PARTIAL) {
      mask = ProtocolUtils.readLongArray(buf);
    }

    chatType = ProtocolUtils.readVarInt(buf);
    name = serializer.deserialize(ProtocolUtils.readString(buf));
    targetName = buf.readBoolean() ? serializer.deserialize(ProtocolUtils.readString(buf)) : null;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeSignatureHeader(buf, header);
    ProtocolUtils.writeByteArray(buf, headerSignature);

    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(protocolVersion);
    ProtocolUtils.writeString(buf, message);
    if (previewed != null) {
      buf.writeBoolean(true);
      ProtocolUtils.writeString(buf, serializer.serialize(previewed));
    } else {
      buf.writeBoolean(false);
    }
    buf.writeLong(expiry.toEpochMilli());
    buf.writeLong(salt);
    ProtocolUtils.writeSignaturePairArray(buf, lastSeen);
    if (unsignedContent != null) {
      buf.writeBoolean(true);
      ProtocolUtils.writeString(buf, serializer.serialize(unsignedContent));
    } else {
      buf.writeBoolean(false);
    }

    ProtocolUtils.writeVarInt(buf, filter.ordinal());
    if (filter == FilterType.PARTIAL) {
      ProtocolUtils.writeLongArray(buf, mask);
    }

    ProtocolUtils.writeVarInt(buf, chatType);
    ProtocolUtils.writeString(buf, serializer.serialize(name));
    if (targetName != null) {
      buf.writeBoolean(true);
      ProtocolUtils.writeString(buf, serializer.serialize(targetName));
    } else {
      buf.writeBoolean(false);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
  
  // Another magic network registry for no reason.
  static enum FilterType {
    NONE,
    FULL,
    PARTIAL;
  }

  /**
   * Creates the {@link HeaderData} from the given message.
   *
   * @return The {@link HeaderData} or null if unsigned
   */
  public HeaderData getHeaderData() {
    if (headerSignature.length > 0) {
      return new HeaderData(header, headerSignature, contentHash());
    }
    return null;
  }


  private byte[] contentHash() {
    HashingOutputStream hashStream = new HashingOutputStream(Hashing.sha256(), OutputStream.nullOutputStream());

    try {
      DataOutputStream dataOut = new DataOutputStream(hashStream);
      dataOut.writeLong(salt);
      dataOut.writeLong(expiry.getEpochSecond());

      OutputStreamWriter outputStream = new OutputStreamWriter(dataOut, StandardCharsets.UTF_8);
      outputStream.write(message);
      outputStream.flush();
      dataOut.write(70);
      if (previewed != null) {
        outputStream.write(ProtocolUtils.STABLE_MODERN_SERIALIZER.serialize(previewed));
        outputStream.flush();
      }

      for (SignaturePair pair : lastSeen) {
        dataOut.writeByte(70);
        dataOut.writeLong(pair.getSigner().getMostSignificantBits());
        dataOut.writeLong(pair.getSigner().getLeastSignificantBits());
        dataOut.write(pair.getSignature());
      }

    } catch (IOException e) {
      throw BODY_HASHING_ERROR;
    }

    return hashStream.hash().asBytes();
  }
}
