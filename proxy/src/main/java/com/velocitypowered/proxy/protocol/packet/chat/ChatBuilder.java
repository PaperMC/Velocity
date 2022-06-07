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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class ChatBuilder {

    private final ProtocolVersion version;

    private @MonotonicNonNull Component component;
    private @MonotonicNonNull String message;
    private @MonotonicNonNull SignedChatMessage signedChatMessage;

    private @Nullable Player sender;

    private @MonotonicNonNull ChatType type;

    private ChatBuilder(ProtocolVersion version) {
        this.version = version;
    }

    public static ChatBuilder builder(ProtocolVersion version) {
        return new ChatBuilder(Preconditions.checkNotNull(version));
    }


    public ChatBuilder message(String message) {
        Preconditions.checkArgument(this.message == null);
        this.message = Preconditions.checkNotNull(message);
        return this;
    }

    public ChatBuilder component(Component message) {
        this.component = Preconditions.checkNotNull(message);
    }

    public ChatBuilder message(SignedChatMessage message) {
        Preconditions.checkNotNull(message);
        Preconditions.checkArgument(this.message == null);
        this.message = message.getMessage();
        this.signedChatMessage = message;
        return this;
    }



    public void setType(ChatType type) {
        this.type = type;
    }

    public ChatBuilder asPlayer(Player player){
        this.sender = Preconditions.checkNotNull(player);
        return this;
    }

    public ChatBuilder asServer(){
        this.sender = null;
        return this;
    }

    public MinecraftPacket toClient() {
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
            if (sender == null) {
                return new SystemChat(component == null ? Component.text(message) : component, type.getId());
            } else {
                if (signedChatMessage == null) {

                }
            }
        } else {
            new LegacyChat()
        }
    }

    public MinecraftPacket toServer() {

    }

    static enum ChatType {
        CHAT((byte) 0),
        SYSTEM((byte) 1),
        GAME_INFO((byte) 2);

        private final byte raw;
        ChatType(byte raw) {
            this.raw = raw;
        }

        public byte getId() {
            return raw;
        }
    }
}
