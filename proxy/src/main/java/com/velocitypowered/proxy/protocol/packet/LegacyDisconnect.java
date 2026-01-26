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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Represents a legacy disconnect packet that contains a reason for disconnection.
 * This class is used to convert modern server ping responses into the legacy format,
 * which is compatible with older Minecraft versions.
 *
 * @param reason the string reason for disconnection
 */
public record LegacyDisconnect(String reason) {

  private static final ServerPing.Players FAKE_PLAYERS = new ServerPing.Players(0, 0,
      ImmutableList.of());
  private static final String LEGACY_COLOR_CODE = Character
      .toString(LegacyComponentSerializer.SECTION_CHAR);

  /**
   * Converts a modern server list ping response into an legacy disconnect packet.
   *
   * @param response the server ping to convert
   * @param version  the requesting clients' version
   * @return the disconnect packet
   */
  public static LegacyDisconnect fromServerPing(ServerPing response,
      LegacyMinecraftPingVersion version) {
    final Players players = response.getPlayers().orElse(FAKE_PLAYERS);

    return switch (version) {
      case MINECRAFT_1_3 ->
          // Minecraft 1.3 and below use the section symbol as a delimiter. Accordingly, we must
          // remove all section symbols, along with fetching just the first line of an (unformatted)
          // MOTD.
          new LegacyDisconnect(String.join(LEGACY_COLOR_CODE,
            cleanSectionSymbol(getFirstLine(PlainTextComponentSerializer.plainText().serialize(
                response.getDescriptionComponent()))),
            Integer.toString(players.getOnline()),
            Integer.toString(players.getMax())));
      case MINECRAFT_1_4, MINECRAFT_1_6 ->
          // Minecraft 1.4-1.6 provide support for more fields, and additionally support color codes.
          new LegacyDisconnect(String.join("\0",
            LEGACY_COLOR_CODE + "1",
            Integer.toString(response.getVersion().getProtocol()),
            response.getVersion().getName(),
            getFirstLine(LegacyComponentSerializer.legacySection().serialize(response
                .getDescriptionComponent())),
            Integer.toString(players.getOnline()),
            Integer.toString(players.getMax())
        ));
      default -> throw new IllegalArgumentException("Unknown version " + version);
    };
  }

  private static String cleanSectionSymbol(String string) {
    return string.replaceAll(LEGACY_COLOR_CODE, "");
  }

  private static String getFirstLine(String legacyMotd) {
    final int newline = legacyMotd.indexOf('\n');
    return newline == -1 ? legacyMotd : legacyMotd.substring(0, newline);
  }

  /**
   * Converts a {@link TextComponent} into a legacy disconnect packet.
   *
   * @param component the component to convert
   * @return the disconnect packet
   */
  public static LegacyDisconnect from(TextComponent component) {
    // We intentionally use the legacy serializers, because the old clients can't understand JSON.
    final String serialized = LegacyComponentSerializer.legacySection().serialize(component);
    return new LegacyDisconnect(serialized);
  }
}
