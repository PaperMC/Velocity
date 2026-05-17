/*
 * Copyright (C) 2025 Velocity Contributors
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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import net.kyori.adventure.text.Component;

public abstract class RateLimitedCommandHandler<T extends MinecraftPacket> implements CommandHandler<T> {

    private final Player player;
    private final VelocityServer velocityServer;

    private int failedAttempts;

    protected RateLimitedCommandHandler(Player player, VelocityServer velocityServer) {
        this.player = player;
        this.velocityServer = velocityServer;
    }

    @Override
    public boolean handlePlayerCommand(MinecraftPacket packet) {
        if (packetClass().isInstance(packet)) {
            if (!velocityServer.getCommandRateLimiter().attempt(player.getUniqueId())) {
                if (velocityServer.getConfiguration().isKickOnCommandRateLimit() && failedAttempts++ >= velocityServer.getConfiguration().getKickAfterRateLimitedCommands()) {
                    player.disconnect(Component.translatable("velocity.kick.command-rate-limit"));
                }

                if (velocityServer.getConfiguration().isForwardCommandsIfRateLimited()) {
                    // Route through the chat queue rather than letting the dispatcher write the
                    // packet directly: a direct write would race ahead of any queued earlier
                    // commands whose plugin events are still pending, causing the backend to see
                    // out-of-order timestamps and kick the player for "out-of-order chat".
                    forwardRateLimited(packetClass().cast(packet));
                    return true;
                }
            } else {
                failedAttempts = 0;
            }

            handlePlayerCommandInternal(packetClass().cast(packet));
            return true;
        }

        return false;
    }

    // Implementations forward the packet through the chat queue WITHOUT firing
    // CommandExecuteEvent - rate-limited commands are intentionally not seen by plugins.
    protected abstract void forwardRateLimited(T packet);
}
