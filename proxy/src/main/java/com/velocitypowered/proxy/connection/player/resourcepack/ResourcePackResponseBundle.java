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

package com.velocitypowered.proxy.connection.player.resourcepack;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import java.util.UUID;

/**
 * Bundles the details of a resource pack response: the pack it refers to and the status the
 * player reported for it.
 *
 * @param uuid the unique id of the resource pack
 * @param hash the hash of the resource pack
 * @param status the status the player reported for the pack
 */
public record ResourcePackResponseBundle(UUID uuid, String hash,
                                         PlayerResourcePackStatusEvent.Status status) {
}
