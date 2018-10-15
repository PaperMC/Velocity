package com.velocitypowered.proxy.connection.forge;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;
import java.util.Optional;

public class ForgeUtil {
    public static final String FORGE_LEGACY_HANDSHAKE_CHANNEL = "FML|HS";
    public static final String FORGE_LEGACY_CHANNEL = "FML";
    public static final String FORGE_MULTIPART_LEGACY_CHANNEL = "FML|MP";
    public static final byte[] FORGE_LEGACY_HANDSHAKE_RESET_DATA = new byte[] { -2, 0 };

    private ForgeUtil() {
        throw new AssertionError();
    }

    public static Optional<List<ModInfo.Mod>> readModList(PluginMessage message) {
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkArgument(message.getChannel().equals(FORGE_LEGACY_HANDSHAKE_CHANNEL),
                "message is not a FML HS plugin message");

        ByteBuf byteBuf = Unpooled.wrappedBuffer(message.getData());
        try {
            byte discriminator = byteBuf.readByte();

            if (discriminator == 2) {
                ImmutableList.Builder<ModInfo.Mod> mods = ImmutableList.builder();
                int modCount = ProtocolUtils.readVarInt(byteBuf);

                for (int index = 0; index < modCount; index++) {
                    String id = ProtocolUtils.readString(byteBuf);
                    String version = ProtocolUtils.readString(byteBuf);
                    mods.add(new ModInfo.Mod(id, version));
                }

                return Optional.of(mods.build());
            }

            return Optional.empty();
        } finally {
            byteBuf.release();
        }
    }
}
