/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import com.velocitypowered.natives.util.BufferPreference;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.crypto.SessionKeyExchange.SessionKeys;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionKeyExchangeTest {

  private static final String SERVER = "server";
  private static final String CLIENT = "Notch";
  private static final int PROTOCOL = 349;
  private static final long SALT = 0x0123456789ABCDEFL;

  private final SecureRandom random = new SecureRandom();
  private KeyPair serverPair;
  private KeyPair clientPair;

  @BeforeEach
  void setUp() {
    serverPair = SessionKeyExchange.generateKeyPair(random);
    clientPair = SessionKeyExchange.generateKeyPair(random);
  }

  @Test
  void base32HexMatchesRfc4648() {
    // RFC 4648, section 10 - only whole 5-byte groups, as we never pad
    assertEquals("", SessionKeyExchange.base32Hex(new byte[0]));
    assertEquals("CPNMUOJ1", SessionKeyExchange.base32Hex(ascii("fooba")));
    assertEquals("0000000000000000", SessionKeyExchange.base32Hex(new byte[10]));
    byte[] ones = new byte[5];
    Arrays.fill(ones, (byte) 0xFF);
    assertEquals("VVVVVVVV", SessionKeyExchange.base32Hex(ones));
  }

  @Test
  void parsePublicKeyRoundTrips() throws GeneralSecurityException {
    byte[] encoded = clientPair.getPublic().getEncoded();
    assertArrayEquals(encoded, SessionKeyExchange.parsePublicKey(encoded).getEncoded());
  }

  @Test
  void parsePublicKeyRejectsOtherCurves() throws GeneralSecurityException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("XDH");
    generator.initialize(NamedParameterSpec.X448, random);
    byte[] x448 = generator.generateKeyPair().getPublic().getEncoded();
    assertThrows(GeneralSecurityException.class, () -> SessionKeyExchange.parsePublicKey(x448));
  }

  @Test
  void parsePublicKeyRejectsGarbage() {
    assertThrows(GeneralSecurityException.class,
        () -> SessionKeyExchange.parsePublicKey(new byte[] {1, 2, 3}));
  }

  @Test
  void derivedKeysHaveTheRightShape() throws GeneralSecurityException {
    SessionKeys keys = derive(serverPair, clientPair, SALT);

    assertEquals("AES", keys.serverToClient().getAlgorithm());
    assertEquals("AES", keys.clientToServer().getAlgorithm());
    assertEquals(16, keys.serverToClient().getEncoded().length);
    assertEquals(16, keys.clientToServer().getEncoded().length);
    assertEquals(VelocityCipherFactory.IV_LENGTH, keys.serverToClientIv().length);
    assertEquals(VelocityCipherFactory.IV_LENGTH, keys.clientToServerIv().length);
    assertEquals(32, keys.integrityDigest().length);
    // 25 bytes of HMAC in base32hex
    assertEquals(40, keys.serverId().length());
    assertTrue(keys.serverId().matches("[0-9A-V]+"), keys.serverId());

    // each direction gets its own key and IV
    assertFalse(Arrays.equals(keys.serverToClient().getEncoded(),
        keys.clientToServer().getEncoded()));
    assertFalse(Arrays.equals(keys.serverToClientIv(), keys.clientToServerIv()));
  }

  @Test
  void derivationIsDeterministic() throws GeneralSecurityException {
    assertSameKeys(derive(serverPair, clientPair, SALT), derive(serverPair, clientPair, SALT));
  }

  @Test
  void bothSidesAgreeOnTheStreamKeys() throws GeneralSecurityException {
    // X25519 is symmetric: deriving with the client's private key and the server's public key
    // must produce the same stream keys (the integrity digest and IVs hash the public keys in
    // "our key, their key" order, so those differ when the roles are swapped like this).
    SessionKeys server = derive(serverPair, clientPair, SALT);
    SessionKeys mirrored = SessionKeyExchange.deriveServerSide(clientPair, SERVER,
        serverPair.getPublic(), CLIENT, PROTOCOL, SALT, true);
    assertArrayEquals(server.serverToClient().getEncoded(),
        mirrored.serverToClient().getEncoded());
    assertArrayEquals(server.clientToServer().getEncoded(),
        mirrored.clientToServer().getEncoded());
  }

  @Test
  void everyInputChangesTheResult() throws GeneralSecurityException {
    SessionKeys base = derive(serverPair, clientPair, SALT);

    assertAllDifferent(base, derive(serverPair, clientPair, SALT + 1));
    assertAllDifferent(base, derive(serverPair, SessionKeyExchange.generateKeyPair(random), SALT));
    assertAllDifferent(base, SessionKeyExchange.deriveServerSide(serverPair, SERVER,
        clientPair.getPublic(), "jeb_", PROTOCOL, SALT, true));
    assertAllDifferent(base, SessionKeyExchange.deriveServerSide(serverPair, "proxy",
        clientPair.getPublic(), CLIENT, PROTOCOL, SALT, true));

    // the protocol version only goes into the stream keys and the integrity digest
    SessionKeys otherProtocol = SessionKeyExchange.deriveServerSide(serverPair, SERVER,
        clientPair.getPublic(), CLIENT, PROTOCOL + 1, SALT, true);
    assertFalse(Arrays.equals(base.serverToClient().getEncoded(),
        otherProtocol.serverToClient().getEncoded()));
    assertFalse(Arrays.equals(base.integrityDigest(), otherProtocol.integrityDigest()));
    assertEquals(base.serverId(), otherProtocol.serverId());

    // the authenticate flag only goes into the integrity digest (and so the IVs)
    SessionKeys noAuth = SessionKeyExchange.deriveServerSide(serverPair, SERVER,
        clientPair.getPublic(), CLIENT, PROTOCOL, SALT, false);
    assertFalse(Arrays.equals(base.integrityDigest(), noAuth.integrityDigest()));
    assertArrayEquals(base.serverToClient().getEncoded(), noAuth.serverToClient().getEncoded());
    assertEquals(base.serverId(), noAuth.serverId());
  }

  @Test
  void sessionKeysWorkWithJavaCipher() throws GeneralSecurityException {
    assertCiphersWork(JavaVelocityCipher.FACTORY, JavaVelocityCipher.FACTORY);
  }

  @Test
  void sessionKeysWorkWithLoadedCipher() throws GeneralSecurityException {
    // whatever this host loaded (native if available), against the Java cipher on the other end
    assertCiphersWork(Natives.cipher.get(), JavaVelocityCipher.FACTORY);
  }

  // `proxy` is the factory the proxy uses, `client` stands in for the client's cipher
  private void assertCiphersWork(VelocityCipherFactory proxy, VelocityCipherFactory client)
      throws GeneralSecurityException {
    SessionKeys keys = derive(serverPair, clientPair, SALT);
    byte[] data = new byte[4096];
    new Random(42).nextBytes(data);

    // server -> client
    byte[] outbound = process(proxy.forEncryption(keys.serverToClient(), keys.serverToClientIv()),
        data);
    assertFalse(Arrays.equals(data, outbound));
    assertArrayEquals(data, process(
        client.forDecryption(keys.serverToClient(), keys.serverToClientIv()), outbound));

    // client -> server
    byte[] inbound = process(
        client.forEncryption(keys.clientToServer(), keys.clientToServerIv()), data);
    assertArrayEquals(data, process(
        proxy.forDecryption(keys.clientToServer(), keys.clientToServerIv()), inbound));

    // the two directions must not produce the same stream
    assertFalse(Arrays.equals(outbound, inbound));
  }

  private static byte[] process(VelocityCipher cipher, byte[] data) {
    BufferPreference preference = cipher.preferredBufferType();
    ByteBuf buf = preference == BufferPreference.DIRECT_REQUIRED
        || preference == BufferPreference.DIRECT_PREFERRED
        ? Unpooled.directBuffer(data.length) : Unpooled.buffer(data.length);
    try {
      buf.writeBytes(data);
      cipher.process(buf);
      return ByteBufUtil.getBytes(buf);
    } finally {
      buf.release();
      cipher.close();
    }
  }

  private static SessionKeys derive(KeyPair server, KeyPair client, long salt)
      throws GeneralSecurityException {
    return SessionKeyExchange.deriveServerSide(server, SERVER, client.getPublic(), CLIENT,
        PROTOCOL, salt, true);
  }

  private static void assertSameKeys(SessionKeys expected, SessionKeys actual) {
    assertArrayEquals(expected.serverToClient().getEncoded(),
        actual.serverToClient().getEncoded());
    assertArrayEquals(expected.serverToClientIv(), actual.serverToClientIv());
    assertArrayEquals(expected.clientToServer().getEncoded(),
        actual.clientToServer().getEncoded());
    assertArrayEquals(expected.clientToServerIv(), actual.clientToServerIv());
    assertArrayEquals(expected.integrityDigest(), actual.integrityDigest());
    assertEquals(expected.serverId(), actual.serverId());
  }

  private static void assertAllDifferent(SessionKeys a, SessionKeys b) {
    assertFalse(Arrays.equals(a.serverToClient().getEncoded(), b.serverToClient().getEncoded()));
    assertFalse(Arrays.equals(a.serverToClientIv(), b.serverToClientIv()));
    assertFalse(Arrays.equals(a.clientToServer().getEncoded(), b.clientToServer().getEncoded()));
    assertFalse(Arrays.equals(a.clientToServerIv(), b.clientToServerIv()));
    assertFalse(Arrays.equals(a.integrityDigest(), b.integrityDigest()));
    assertFalse(a.serverId().equals(b.serverId()));
  }

  private static byte[] ascii(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }
}
