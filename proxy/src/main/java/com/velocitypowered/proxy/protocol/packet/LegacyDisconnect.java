package com.velocitypowered.proxy.protocol.packet;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;

public class LegacyDisconnect {

  private final String reason;

  private LegacyDisconnect(String reason) {
    this.reason = reason;
  }

  /**
   * Converts a legacy response into an legacy disconnect packet.
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

  /**
   * Converts a {@link TextComponent} into a legacy disconnect packet.
   * @param component the component to convert
   * @return the disconnect packet
   */
  public static LegacyDisconnect from(TextComponent component) {
    // We intentionally use the legacy serializers, because the old clients can't understand JSON.
    @SuppressWarnings("deprecated")
    String serialized = ComponentSerializers.LEGACY.serialize(component);
    return new LegacyDisconnect(serialized);
  }

  public String getReason() {
    return reason;
  }
}
