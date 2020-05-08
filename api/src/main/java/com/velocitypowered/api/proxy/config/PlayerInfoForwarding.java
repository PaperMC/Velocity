package com.velocitypowered.api.proxy.config;

public enum PlayerInfoForwarding {
  /**
   * Forward player IPs and UUIDs in a format supported by the BungeeGuard
   * plugin. Use this if you run servers using Minecraft 1.12 or lower, and are"
   * unable to implement network level firewalling (on a shared host).
   */
  BUNGEEGUARD,

  /**
   * Forwarding mode will be picked from Velocity's configuration.
   */
  DEFAULT,

  /**
   * No forwarding will be done. All players will appear to be connecting from the
   * proxy and will have offline-mode UUIDs.
   */
  NONE,

  /**
   * Forward player IPs and UUIDs in a BungeeCord-compatible format. Use this if
   * you run servers using Minecraft 1.12 or lower.
   */
  LEGACY,

  /**
   * Forward player IPs and UUIDs as part of the login process using Velocity's
   * native forwarding. Only applicable for Minecraft 1.13 or higher.
   */
  MODERN,
  ;
}
