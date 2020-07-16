package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

public class LegacyDisconnect {

  private static final ServerPing.Players FAKE_PLAYERS = new ServerPing.Players(0, 0,
      ImmutableList.of());
  private static final String LEGACY_COLOR_CODE = Character
      .toString(LegacyComponentSerializer.SECTION_CHAR);

  private final String reason;

  private LegacyDisconnect(String reason) {
    this.reason = reason;
  }

  /**
   * Converts a modern server list ping response into an legacy disconnect packet.
   * @param response the server ping to convert
   * @param version the requesting clients' version
   * @return the disconnect packet
   */
  public static LegacyDisconnect fromServerPing(ServerPing response,
      LegacyMinecraftPingVersion version) {
    Players players = response.getPlayers().orElse(FAKE_PLAYERS);

    switch (version) {
      case MINECRAFT_1_3:
        // Minecraft 1.3 and below use the section symbol as a delimiter. Accordingly, we must
        // remove all section symbols, along with fetching just the first line of an (unformatted)
        // MOTD.
        return new LegacyDisconnect(String.join(LEGACY_COLOR_CODE,
            cleanSectionSymbol(getFirstLine(PlainComponentSerializer.plain().serialize(
                response.getDescriptionComponent()))),
            Integer.toString(players.getOnline()),
            Integer.toString(players.getMax())));
      case MINECRAFT_1_4:
      case MINECRAFT_1_6:
        // Minecraft 1.4-1.6 provide support for more fields, and additionally support color codes.
        return new LegacyDisconnect(String.join("\0",
            LEGACY_COLOR_CODE + "1",
            Integer.toString(response.getVersion().getProtocol()),
            response.getVersion().getName(),
            getFirstLine(LegacyComponentSerializer.legacy().serialize(response
                .getDescriptionComponent())),
            Integer.toString(players.getOnline()),
            Integer.toString(players.getMax())
        ));
      default:
        throw new IllegalArgumentException("Unknown version " + version);
    }
  }

  private static String cleanSectionSymbol(String string) {
    return string.replaceAll(LEGACY_COLOR_CODE, "");
  }

  private static String getFirstLine(String legacyMotd) {
    int newline = legacyMotd.indexOf('\n');
    return newline == -1 ? legacyMotd : legacyMotd.substring(0, newline);
  }

  /**
   * Converts a {@link TextComponent} into a legacy disconnect packet.
   * @param component the component to convert
   * @return the disconnect packet
   */
  public static LegacyDisconnect from(TextComponent component) {
    // We intentionally use the legacy serializers, because the old clients can't understand JSON.
    String serialized = LegacyComponentSerializer.legacy().serialize(component);
    return new LegacyDisconnect(serialized);
  }

  public String getReason() {
    return reason;
  }
}
