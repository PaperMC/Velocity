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

package com.velocitypowered.proxy.connection.forge.modern;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.client.ClientConnectionPhases;
import com.velocitypowered.proxy.connection.util.ConnectionTypeImpl;

/**
 * Contains extra logic for {@link ConnectionTypes#MODERN_FORGE}.
 */
public class ModernForgeConnectionType extends ConnectionTypeImpl {

    private static final GameProfile.Property IS_FORGE_CLIENT_PROPERTY =
            new GameProfile.Property("forgeClient", "true", "");

    public ModernForgeConnectionType() {
        super(ClientConnectionPhases.VANILLA, BackendConnectionPhases.VANILLA);
    }

    @Override
    public GameProfile addGameProfileTokensIfRequired(GameProfile original,PlayerInfoForwarding forwardingType) {
        if (forwardingType == PlayerInfoForwarding.MODERN) {
            original.addProperty(IS_FORGE_CLIENT_PROPERTY);
        }
        return original;
    }
}
