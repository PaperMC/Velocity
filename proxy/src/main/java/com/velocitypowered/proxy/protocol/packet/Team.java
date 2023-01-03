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
import io.netty.buffer.ByteBuf;

public class Team implements MinecraftPacket {

    public static final byte CREATE = 0;
    public static final byte REMOVE = 1;
    public static final byte UPDATE_INFO = 2;
    public static final byte ADD_PLAYER = 3;
    public static final byte REMOVE_PLAYER = 4;

    private String name;
    private byte mode;
    private String displayName;
    private String prefix;
    private String suffix;
    private String nameTagVisibility;
    private String collisionRule;
    private int color;
    private byte friendlyFire;
    private String[] players;

    public Team() {
    }

    public Team(String name) {
        this.name = name;
        this.mode = REMOVE;
    }

    public Team(String name, byte mode) {
        this.name = name;
        this.mode = mode;
    }

    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        name = ProtocolUtils.readString(buf);
        mode = buf.readByte();
        if (mode == CREATE || mode == UPDATE_INFO) {
            displayName = ProtocolUtils.readString(buf);
            if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
                prefix = ProtocolUtils.readString(buf);
                suffix = ProtocolUtils.readString(buf);
            }
            friendlyFire = buf.readByte();
            nameTagVisibility = ProtocolUtils.readString(buf);
            if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
                collisionRule = ProtocolUtils.readString(buf);
            }
            color = ( version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0 ) ? ProtocolUtils.readVarInt(buf) : buf.readByte();
            if ( version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0 ) {
                prefix = ProtocolUtils.readString(buf);
                suffix = ProtocolUtils.readString(buf);
            }
        }
        if (mode == CREATE || mode == ADD_PLAYER || mode == REMOVE_PLAYER) {
            int len = ProtocolUtils.readVarInt(buf);
            players = new String[len];
            for (int i = 0; i < len; i++) {
                players[i] = ProtocolUtils.readString(buf);
            }
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        ProtocolUtils.writeString(buf, name);
        buf.writeByte(mode);
        if (mode == CREATE || mode == UPDATE_INFO) {
            ProtocolUtils.writeString(buf, displayName);
            if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
                ProtocolUtils.writeString(buf, prefix);
                ProtocolUtils.writeString(buf, suffix);
            }
            buf.writeByte(friendlyFire);
            ProtocolUtils.writeString(buf, nameTagVisibility);
            if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
                ProtocolUtils.writeString(buf, collisionRule);
            }

            if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
                ProtocolUtils.writeVarInt(buf, color);
                ProtocolUtils.writeString(buf, prefix);
                ProtocolUtils.writeString(buf, suffix);
            } else {
                buf.writeByte( color );
            }
        }
        if (mode == CREATE || mode == ADD_PLAYER || mode == REMOVE_PLAYER) {
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getNameTagVisibility() {
        return nameTagVisibility;
    }

    public void setNameTagVisibility(String nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
    }

    public String getCollisionRule() {
        return collisionRule;
    }

    public void setCollisionRule(String collisionRule) {
        this.collisionRule = collisionRule;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public byte getFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(byte friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public String[] getPlayers() {
        return players;
    }

    public void setPlayers(String[] players) {
        this.players = players;
    }

    @Override
    public String toString() {
        return "Team{" +
                "name=" + name +
                ", mode=" + mode +
                ", displayName=" + displayName +
                ", prefix=" + prefix +
                ", suffix=" + suffix +
                ", friendlyFire=" + friendlyFire +
                ", nameTagVisibility=" + nameTagVisibility +
                ", collisionRule=" + collisionRule +
                ", color=" + color +
                ", players=[" + String.join(",", players) + "]" +
                '}';
    }
}
