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

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;

public class PlayerCommand implements MinecraftPacket {

    private String command;
    private @Nullable Instant timestamp;
    private boolean signedPreview; // Good god. Please no.

    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        command = ProtocolUtils.readString(buf, 256);
        long instant = buf.readLong();

        long salt = buf.readLong();
        int mapSize = ProtocolUtils.readVarInt(buf);
        // Mapped as Argument : signature
        // I have no f*cking clue what the salt is for
        ImmutableMap.Builder<String, byte[]> entries = ImmutableMap.builderWithExpectedSize(mapSize);
        for (int i = 0; i < mapSize; i++) {
            entries.put(ProtocolUtils.readString(buf, 16), ProtocolUtils.readByteArray(buf));
        }

        signedPreview = buf.readBoolean();
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {

    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
