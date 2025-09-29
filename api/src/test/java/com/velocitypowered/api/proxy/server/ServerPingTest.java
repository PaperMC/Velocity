/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ServerPingTest {

  @Test
  void asBuilderConsistency() {
    ServerPing ping = new ServerPing(new Version(404, "1.13.2"),
        new Players(1, 1, ImmutableList.of(new SamplePlayer("tuxed", UUID.randomUUID()))),
        Component.text("test"), null);
    assertEquals(ping, ping.asBuilder().build());
  }
}