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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;

public class TeamPacket implements MinecraftPacket {

    public static final byte ADD = 0;
    public static final byte REMOVE = 1;
    public static final byte UPDATE = 2;
    public static final byte ADD_PLAYER = 3;
    public static final byte REMOVE_PLAYER = 4;

    private String name;
    private byte mode;
    private ComponentHolder displayName;
    private ComponentHolder prefix;
    private ComponentHolder suffix;
    private String nameTagVisibility;
    private int nameTagVisibilityInt;
    private String collisionRule;
    private int collisionRuleInt;
    private int color;
    private byte friendlyFire;
    private String[] players;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getMode() {
        return mode;
    }

    public void setMode(byte mode) {
        this.mode = mode;
    }

    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        name = ProtocolUtils.readString(buf);
        mode = buf.readByte();
        if (mode == ADD || mode == UPDATE) {
            displayName = ComponentHolder.read(buf, version);
            if (version.lessThan(ProtocolVersion.MINECRAFT_1_13)) {
                prefix = ComponentHolder.read(buf, version);
                suffix = ComponentHolder.read(buf, version);
            }
            friendlyFire = buf.readByte();
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
                nameTagVisibilityInt = ProtocolUtils.readVarInt(buf);
                collisionRuleInt = ProtocolUtils.readVarInt(buf);
            } else {
                nameTagVisibility = ProtocolUtils.readString(buf);
                if (version.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
                    collisionRule = ProtocolUtils.readString(buf);
                }
            }
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
                color = ProtocolUtils.readVarInt(buf);
                prefix = ComponentHolder.read(buf, version);
                suffix = ComponentHolder.read(buf, version);
            } else {
                color = buf.readByte();
            }
        }
        if (mode == ADD || mode == ADD_PLAYER || mode == REMOVE_PLAYER) {
            int len = ProtocolUtils.readVarInt(buf);
            players = new String[len];
            for (int i = 0; i < len; i++) {
                players[i] = ProtocolUtils.readString(buf);
            }
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(buf, name);
        buf.writeByte(mode);
        if (mode == ADD || mode == UPDATE) {
            displayName.write(buf);
            if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_13)) {
                prefix.write(buf);
                suffix.write(buf);
            }
            buf.writeByte(friendlyFire);
            if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
                ProtocolUtils.writeVarInt(buf, nameTagVisibilityInt);
                ProtocolUtils.writeVarInt(buf, collisionRuleInt);
            } else {
                ProtocolUtils.writeString(buf, nameTagVisibility);
                if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
                    ProtocolUtils.writeString(buf, collisionRule);
                }
            }
            if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
                ProtocolUtils.writeVarInt(buf, color);
                prefix.write(buf);
                suffix.write(buf);
            } else {
                buf.writeByte(color);
            }
        }
        if (mode == ADD || mode == ADD_PLAYER || mode == REMOVE_PLAYER) {
            ProtocolUtils.writeVarInt(buf, players.length);
            for (String player : players) {
                ProtocolUtils.writeString(buf, player);
            }
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
