package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.util.AddressUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ForcedHostsTest {
  @Test
  void testIsHostMatchingPattern() {
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.miscpvp.org", "play.miscpvp.org"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.miscpvp.org", "yt.miscpvp.org"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.miscpvp.org", "ip.miscpvp.org"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("test.*.miscpvp.org", "test.example.miscpvp.org"));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("*.miscpvp.org", "test.example.miscpvp.org"));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("*.miscpvp.org", "miscpvp.org"));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("miscpvp.minehunt.gg", "ip.miscpvp.org"));
  }
}
