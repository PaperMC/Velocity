package com.velocitypowered.proxy.protocol.packet;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;

public class LegacyDisconnect {

  private final String reason;

  private LegacyDisconnect(String reason) {
    this.reason = reason;
  }

  /**
   * Converts a legacy response into a disconnect packet.
   * @param response the response to convert
   * @return the disconnect packet
   */
  public static LegacyDisconnect fromPingResponse(LegacyPingResponse response) {
    String kickMessage = String.join("\0",
        "§1",
        Integer.toString(response.getProtocolVersion()),
        response.getServerVersion(),
        response.getMotd(),
        Integer.toString(response.getPlayersOnline()),
        Integer.toString(response.getPlayersMax())
    );
    return new LegacyDisconnect(kickMessage);
  }

  public static LegacyDisconnect from(TextComponent component) {
    return new LegacyDisconnect(ComponentSerializers.LEGACY.serialize(component));
  }

  public String getReason() {
    return reason;
  }
}
