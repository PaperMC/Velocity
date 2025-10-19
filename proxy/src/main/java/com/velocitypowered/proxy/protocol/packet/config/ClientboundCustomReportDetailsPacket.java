/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

public record ClientboundCustomReportDetailsPacket(Map<String, String> details) implements MinecraftPacket {

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }

    public static class Codec implements PacketCodec<ClientboundCustomReportDetailsPacket> {
        public static final Codec INSTANCE = new Codec();

        @Override
        public ClientboundCustomReportDetailsPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                                           ProtocolVersion protocolVersion) {
            int detailsCount = ProtocolUtils.readVarInt(buf);

            Map<String, String> details = new HashMap<>(detailsCount);
            for (int i = 0; i < detailsCount; i++) {
                details.put(ProtocolUtils.readString(buf), ProtocolUtils.readString(buf));
            }
            return new ClientboundCustomReportDetailsPacket(details);
        }

        @Override
        public void encode(ClientboundCustomReportDetailsPacket packet, ByteBuf buf,
                           ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
            ProtocolUtils.writeVarInt(buf, packet.details.size());

            packet.details.forEach((key, detail) -> {
                ProtocolUtils.writeString(buf, key);
                ProtocolUtils.writeString(buf, detail);
            });
        }
    }
}
