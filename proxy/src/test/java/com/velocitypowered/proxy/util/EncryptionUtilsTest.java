/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.velocitypowered.proxy.crypto.EncryptionUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class EncryptionUtilsTest {

  @Test
  void twosComplementHexdigest() throws Exception {
    String notchHash = mojangLoginSha1("Notch");
    assertEquals("4ed1f46bbe04bc756bcb17c0c7ce3e4632f06a48", notchHash);

    String jebHash = mojangLoginSha1("jeb_");
    assertEquals("-7c9d5b0044c130109a5d7b5fb5c317c02b4e28c1", jebHash);
  }

  private String mojangLoginSha1(String str) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    digest.update(str.getBytes(StandardCharsets.UTF_8));
    byte[] digested = digest.digest();
    return EncryptionUtils.twosComplementHexdigest(digested);
  }
}
