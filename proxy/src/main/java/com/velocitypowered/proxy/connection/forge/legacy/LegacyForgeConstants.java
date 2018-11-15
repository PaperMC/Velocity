package com.velocitypowered.proxy.connection.forge.legacy;

/**
 * Constants for use with Legacy Forge systems.
 */
public class LegacyForgeConstants {

  /**
   * Clients attempting to connect to 1.8+ Forge servers will have
   * this token appended to the hostname in the initial handshake
   * packet.
   */
  public static final String HANDSHAKE_HOSTNAME_TOKEN = "\0FML\0";

  /**
   * The channel for legacy forge handshakes.
   */
  public static final String FORGE_LEGACY_HANDSHAKE_CHANNEL = "FML|HS";

  /**
   * The reset packet discriminator.
   */
  private static final int RESET_DATA_DISCRIMINATOR = -2;

  /**
   * The acknowledgement packet discriminator.
   */
  static final int ACK_DISCRIMINATOR = -1;

  /**
   * The Server -> Client Hello discriminator.
   */
  static final int SERVER_HELLO_DISCRIMINATOR = 0;

  /**
   * The Client -> Server Hello discriminator.
   */
  static final int CLIENT_HELLO_DISCRIMINATOR = 1;

  /**
   * The Mod List discriminator.
   */
  static final int MOD_LIST_DISCRIMINATOR = 2;

  /**
   * The Registry discriminator.
   */
  static final int REGISTRY_DISCRIMINATOR = 3;

  /**
   * The form of the data for the reset packet
   */
  static final byte[] FORGE_LEGACY_HANDSHAKE_RESET_DATA = new byte[]{RESET_DATA_DISCRIMINATOR, 0};

  private LegacyForgeConstants() {
    throw new AssertionError();
  }

}
