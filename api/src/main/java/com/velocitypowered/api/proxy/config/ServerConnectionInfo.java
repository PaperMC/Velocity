/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.config;

import java.util.Objects;

/**
 * Exposes certain server information.
 */
public final class ServerConnectionInfo {
  private final String address;
  private final PlayerInfoForwarding forwarding;

  public ServerConnectionInfo(String address, PlayerInfoForwarding forwarding) {
    this.address = address;
    this.forwarding = forwarding;
  }

  public String getAddress() {
    return address;
  }

  public PlayerInfoForwarding getForwarding() {
    return forwarding;
  }

  public static ServerConnectionInfo of(String address, PlayerInfoForwarding forwarding) {
    return new ServerConnectionInfo(address, forwarding);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, forwarding);
  }

  @Override
  public String toString() {
    if (forwarding == PlayerInfoForwarding.DEFAULT) {
      return "{ address = \"" + address + "\" }";
    } else {
      return "{ address = \"" + address + "\", forwarding = \"" + forwarding.name() + "\" }";
    }
  }
}
