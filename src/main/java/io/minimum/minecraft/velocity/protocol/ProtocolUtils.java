package io.minimum.minecraft.velocity.protocol;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

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
        byte[] str = new byte[length];
        buf.readBytes(str);
        return new String(str, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String str) {
        byte[] asUtf8 = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, asUtf8.length);
        buf.writeBytes(asUtf8);
    }
}
