/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
