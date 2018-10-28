package com.velocitypowered.proxy.protocol.packet;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.text.serializer.ComponentSerializers;

public class LegacyPingResponse {

  private static final ServerPing.Players FAKE_PLAYERS = new ServerPing.Players(0, 0,
      ImmutableList.of());
  private final int protocolVersion;
  private final String serverVersion;
  private final String motd;
  private final int playersOnline;
  private final int playersMax;

  public LegacyPingResponse(int protocolVersion, String serverVersion, String motd,
      int playersOnline, int playersMax) {
    this.protocolVersion = protocolVersion;
    this.serverVersion = serverVersion;
    this.motd = motd;
    this.playersOnline = playersOnline;
    this.playersMax = playersMax;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public String getMotd() {
    return motd;
  }

  public int getPlayersOnline() {
    return playersOnline;
  }

  public int getPlayersMax() {
    return playersMax;
  }

  @Override
  public String toString() {
    return "LegacyPingResponse{"
        + "protocolVersion=" + protocolVersion
        + ", serverVersion='" + serverVersion + '\''
        + ", motd='" + motd + '\''
        + ", playersOnline=" + playersOnline
        + ", playersMax=" + playersMax
        + '}';
  }

  /**
   * Transforms a {@link ServerPing} into a legacy ping response.
   * @param ping the response to transform
   * @return the legacy ping response
   */
  public static LegacyPingResponse from(ServerPing ping) {
    return new LegacyPingResponse(ping.getVersion().getProtocol(),
        ping.getVersion().getName(),
        ComponentSerializers.LEGACY.serialize(ping.getDescription()),
        ping.getPlayers().orElse(FAKE_PLAYERS).getOnline(),
        ping.getPlayers().orElse(FAKE_PLAYERS).getMax());
  }
}
