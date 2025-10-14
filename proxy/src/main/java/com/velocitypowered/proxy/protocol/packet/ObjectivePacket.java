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
import com.velocitypowered.proxy.protocol.*;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;

public class ObjectivePacket implements MinecraftPacket {

    public static final byte ADD = 0;
    public static final byte REMOVE = 1;
    public static final byte UPDATE = 2;

    private String name;
    private byte action;
    private String value;
    private ComponentHolder componentValue;
    private String type;
    private int intType;
    private boolean numberFormat;
    private ComponentHolder numberFormatFixed;
    private BinaryTag numberFormatStyled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getAction() {
        return action;
    }

    public void setAction(byte action) {
        this.action = action;
    }

    @Override
    public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        name = ProtocolUtils.readString(buf);
        action = buf.readByte();
        if (action == ADD || action == UPDATE) {
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
                componentValue = ComponentHolder.read(buf, version);
                intType = ProtocolUtils.readVarInt(buf);
            } else {
                value = ProtocolUtils.readString(buf);
                type = ProtocolUtils.readString(buf);
            }
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
                numberFormat = buf.readBoolean();
                if (numberFormat) {
                    switch (ProtocolUtils.readVarInt(buf)) {
                        case 0:
                            break;
                        case 1:
                            numberFormatStyled = ProtocolUtils.readBinaryTag(buf, version, BinaryTagIO.reader());
                            break;
                        case 2:
                            numberFormatFixed = ComponentHolder.read(buf, version);
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        ProtocolUtils.writeString(buf, name);
        buf.writeByte(action);
        if (action == ADD || action == UPDATE) {
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
                componentValue.write(buf);
                ProtocolUtils.writeVarInt(buf, intType);
            } else {
                ProtocolUtils.writeString(buf, value);
                ProtocolUtils.writeString(buf, type);
            }
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
                buf.writeBoolean(numberFormat);
                if (numberFormat) {
                    if (numberFormatStyled != null) {
                        ProtocolUtils.writeVarInt(buf, 1);
                        ProtocolUtils.writeBinaryTag(buf, version, numberFormatStyled);
                    } else if (numberFormatFixed != null) {
                        ProtocolUtils.writeVarInt(buf, 2);
                        numberFormatFixed.write(buf);
                    } else {
                        ProtocolUtils.writeVarInt(buf, 0);
                    }
                }
            }
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        return handler.handle(this);
    }
}
