/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.server.QueryResponse.PluginInformation;
import org.junit.jupiter.api.Test;

class QueryResponseTest {

  @Test
  void toBuilderConsistency() {
    QueryResponse response = new QueryResponse("test", "test", "test",
        1, 2, "test", 1234, ImmutableList.of("tuxed"),
        "0.0.1", ImmutableList.of(new PluginInformation("test", "1.0.0"),
          new PluginInformation("test2", null)));
    assertEquals(response, response.toBuilder().build());
  }
}
