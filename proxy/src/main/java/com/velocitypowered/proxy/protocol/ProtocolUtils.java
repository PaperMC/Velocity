package com.velocitypowered.proxy.protocol;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.util.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public enum ProtocolUtils { ;
    private static final int DEFAULT_MAX_STRING_SIZE = 65536; // 64KiB

    public static int readVarInt(ByteBuf buf) {
        int i = 0;
        int j = 0;
        while (true) {
            int k = buf.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while (true) {
            if ((value & 0xFFFFFF80) == 0) {
                buf.writeByte(value);
                return;
            }

            buf.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
    }

    public static String readString(ByteBuf buf) {
        return readString(buf, DEFAULT_MAX_STRING_SIZE);
    }

    public static String readString(ByteBuf buf, int cap) {
        int length = readVarInt(buf);
        Preconditions.checkArgument(length <= cap, "Bad string size (got %s, maximum is %s)", length, cap);
        String str = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
        buf.skipBytes(length);
        return str;
    }

    public static void writeString(ByteBuf buf, String str) {
        int size = ByteBufUtil.utf8Bytes(str);
        writeVarInt(buf, size);
        ByteBufUtil.writeUtf8(buf, str);
    }

    public static byte[] readByteArray(ByteBuf buf) {
        return readByteArray(buf, DEFAULT_MAX_STRING_SIZE);
    }

    public static byte[] readByteArray(ByteBuf buf, int cap) {
        int length = readVarInt(buf);
        Preconditions.checkArgument(length <= cap, "Bad string size (got %s, maximum is %s)", length, cap);
        byte[] array = new byte[length];
        buf.readBytes(array);
        return array;
    }

    public static void writeByteArray(ByteBuf buf, byte[] array) {
        writeVarInt(buf, array.length);
        buf.writeBytes(array);
    }

    public static UUID readUuid(ByteBuf buf) {
        long msb = buf.readLong();
        long lsb = buf.readLong();
        return new UUID(msb, lsb);
    }

    public static void writeUuid(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writeProperties(ByteBuf buf, List<GameProfile.Property> properties) {
        writeVarInt(buf, properties.size());
        for (GameProfile.Property property : properties) {
            writeString(buf, property.getName());
            writeString(buf, property.getValue());
            String signature = property.getSignature();
            if (signature != null) {
                buf.writeBoolean(true);
                writeString(buf, signature);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public static List<GameProfile.Property> readProperties(ByteBuf buf) {
        List<GameProfile.Property> properties = new ArrayList<>();
        int size = readVarInt(buf);
        for (int i = 0; i < size; i++) {
            String name = readString(buf);
            String value = readString(buf);
            String signature = "";
            boolean hasSignature = buf.readBoolean();
            if (hasSignature) {
                signature = readString(buf);
            }
            properties.add(new GameProfile.Property(name, value, signature));
        }
        return properties;
    }
}
