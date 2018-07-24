package io.minimum.minecraft.velocity.protocol;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public enum ProtocolUtils { ;
    private static final int DEFAULT_MAX_STRING_SIZE = 65536; // 64KiB

    public static int readVarInt(ByteBuf buf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = buf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        do {
            byte temp = (byte)(value & 0b01111111);
            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            buf.writeByte(temp);
        } while (value != 0);
    }

    public static String readString(ByteBuf buf) {
        return readString(buf, DEFAULT_MAX_STRING_SIZE);
    }

    public static String readString(ByteBuf buf, int cap) {
        int length = readVarInt(buf);
        Preconditions.checkArgument(length < cap, "Bad string size (got %s, maximum is %s)", length, cap);
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
