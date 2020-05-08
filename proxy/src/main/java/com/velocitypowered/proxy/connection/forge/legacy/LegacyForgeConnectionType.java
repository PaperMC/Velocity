package com.velocitypowered.proxy.connection.forge.legacy;

import com.velocitypowered.api.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.util.ConnectionTypeImpl;

/**
 * Contains extra logic for {@link ConnectionTypes#LEGACY_FORGE}.
 */
public class LegacyForgeConnectionType extends ConnectionTypeImpl {

  private static final GameProfile.Property IS_FORGE_CLIENT_PROPERTY =
      new GameProfile.Property("forgeClient", "true", "");

  public LegacyForgeConnectionType() {
    super(LegacyForgeHandshakeClientPhase.NOT_STARTED,
        LegacyForgeHandshakeBackendPhase.NOT_STARTED);
  }

  @Override
  public GameProfile addGameProfileTokensIfRequired(GameProfile original,
      PlayerInfoForwarding forwardingType) {
    // We can't forward the FML token to the server when we are running in legacy forwarding mode,
    // since both use the "hostname" field in the handshake. We add a special property to the
    // profile instead, which will be ignored by non-Forge servers and can be intercepted by a
    // Forge coremod, such as SpongeForge.
    if (forwardingType == PlayerInfoForwarding.LEGACY) {
      return original.addProperty(IS_FORGE_CLIENT_PROPERTY);
    }

    return original;
  }
}
