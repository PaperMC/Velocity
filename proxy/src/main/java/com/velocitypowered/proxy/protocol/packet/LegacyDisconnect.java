package com.velocitypowered.proxy.protocol.packet;

import static net.kyori.text.serializer.ComponentSerializers.LEGACY;
import static net.kyori.text.serializer.ComponentSerializers.PLAIN;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;

public class LegacyDisconnect {

  private static final ServerPing.Players FAKE_PLAYERS = new ServerPing.Players(0, 0,
      ImmutableList.of());
  private static final String LEGACY_COLOR_CODE = "\u00a7";

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
  @SuppressWarnings("deprecation") // we use these on purpose to service older clients!
  public static LegacyDisconnect fromServerPing(ServerPing response,
      LegacyMinecraftPingVersion version) {
    Players players = response.getPlayers().orElse(FAKE_PLAYERS);

    switch (version) {
      case MINECRAFT_1_3:
        // Minecraft 1.3 and below use the section symbol as a delimiter. Accordingly, we must
        // remove all section symbols, along with fetching just the first line of an (unformatted)
        // MOTD.
        return new LegacyDisconnect(String.join(LEGACY_COLOR_CODE,
            cleanSectionSymbol(getFirstLine(PLAIN.serialize(response.getDescription()))),
            Integer.toString(players.getOnline()),
            Integer.toString(players.getMax())));
      case MINECRAFT_1_4:
      case MINECRAFT_1_6:
        // Minecraft 1.4-1.6 provide support for more fields, and additionally support color codes.
        return new LegacyDisconnect(String.join("\0",
            LEGACY_COLOR_CODE + "1",
            Integer.toString(response.getVersion().getProtocol()),
            response.getVersion().getName(),
            getFirstLine(LEGACY.serialize(response.getDescription())),
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
    @SuppressWarnings("deprecation")
    String serialized = ComponentSerializers.LEGACY.serialize(component);
    return new LegacyDisconnect(serialized);
  }

  public String getReason() {
    return reason;
  }
}
