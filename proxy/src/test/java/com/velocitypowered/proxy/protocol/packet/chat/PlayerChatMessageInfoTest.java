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

package com.velocitypowered.proxy.protocol.packet.chat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import java.lang.reflect.Field;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class PlayerChatMessageInfoTest {

  private static final UUID PLAYER_ID = new UUID(0, 42);
  private static final Instant EXPIRY = Instant.ofEpochMilli(123456789L);

  @Test
  void sessionSignedChatPreservesOriginalSignedMessage() throws Exception {
    byte[] signature = new byte[] {1, 2, 3};
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, signature);
    PlayerChatEvent.MessageInfo info = PlayerChatMessageInfo.fromSessionPacket(player(), packet);

    assertEquals(PlayerChatEvent.SignedState.SIGNED, info.getSignedState());
    SignedMessage signedMessage = info.getSignedMessage().orElseThrow();
    assertEquals("signed body", signedMessage.getMessage());
    assertEquals(PLAYER_ID, signedMessage.getSignerUuid());
    assertEquals(EXPIRY, signedMessage.getExpiryTemporal());
    assertArrayEquals(signature, signedMessage.getSignature());
    assertArrayEquals(packet.getSaltBytes(), signedMessage.getSalt());
  }

  @Test
  void keyedSignedChatPreservesSignatureSaltAndPreviewFlag() throws Exception {
    byte[] signature = new byte[] {4, 5, 6};
    byte[] salt = new byte[] {7, 8};
    KeyedPlayerChatPacket packet = keyedPacket("keyed body", false, EXPIRY, signature, salt, true);
    PlayerChatEvent.MessageInfo info = PlayerChatMessageInfo.fromKeyedPacket(player(), packet);

    assertEquals(PlayerChatEvent.SignedState.SIGNED, info.getSignedState());
    SignedMessage signedMessage = info.getSignedMessage().orElseThrow();
    assertEquals("keyed body", signedMessage.getMessage());
    assertArrayEquals(signature, signedMessage.getSignature());
    assertArrayEquals(salt, signedMessage.getSalt());
    assertTrue(signedMessage.isPreviewSigned());
  }

  @Test
  void unsignedModernChatDoesNotExposeSignedMessage() throws Exception {
    SessionPlayerChatPacket packet = sessionPacket("unsigned body", false, 0L, new byte[0]);
    PlayerChatEvent.MessageInfo info = PlayerChatMessageInfo.fromSessionPacket(player(), packet);

    assertEquals(PlayerChatEvent.SignedState.UNSIGNED, info.getSignedState());
    assertFalse(info.getSignedMessage().isPresent());
  }

  @Test
  void signedChatWithoutKeyStillReportsSignedState() throws Exception {
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, new byte[] {1});

    PlayerChatEvent.MessageInfo info = PlayerChatMessageInfo.fromSessionPacket(player, packet);

    assertEquals(PlayerChatEvent.SignedState.SIGNED, info.getSignedState());
    assertFalse(info.getSignedMessage().isPresent());
  }

  @Test
  void legacyChatHasNoFabricatedSignedMessage() {
    PlayerChatEvent.MessageInfo info = PlayerChatEvent.MessageInfo.legacy();

    assertEquals(PlayerChatEvent.SignedState.LEGACY, info.getSignedState());
    assertFalse(info.getSignedMessage().isPresent());
  }

  @Test
  void signedMessageDefensivelyCopiesMutableSignatureData() throws Exception {
    byte[] signature = new byte[] {9, 10, 11};
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, signature);
    SignedMessage signedMessage = PlayerChatMessageInfo.fromSessionPacket(player(), packet)
        .getSignedMessage()
        .orElseThrow();

    signature[0] = 99;
    byte[] exposed = signedMessage.getSignature();
    exposed[1] = 88;

    assertArrayEquals(new byte[] {9, 10, 11}, signedMessage.getSignature());
  }

  @Test
  void signedChatCancelAndRewriteSafetyStillDisconnectsPlayer() {
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    Logger logger = mock(Logger.class);
    when(player.getUsername()).thenReturn("player");

    KeyedChatHandler.invalidCancel(logger, player);
    KeyedChatHandler.invalidChange(logger, player);

    verify(player, times(2)).disconnect(Component.text("A proxy plugin caused an illegal protocol state. "
        + "Contact your network administrator."));
  }

  private static ConnectedPlayer player() throws Exception {
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    IdentifiedKey key = mock(IdentifiedKey.class);
    PublicKey publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

    when(key.getSignedPublicKey()).thenReturn(publicKey);
    when(key.getExpiryTemporal()).thenReturn(EXPIRY);
    when(player.getIdentifiedKey()).thenReturn(key);
    when(player.getUniqueId()).thenReturn(PLAYER_ID);
    return player;
  }

  private static SessionPlayerChatPacket sessionPacket(String message, boolean signed, long salt,
      byte[] signature) throws Exception {
    SessionPlayerChatPacket packet = new SessionPlayerChatPacket();
    set(packet, "message", message);
    set(packet, "signed", signed);
    set(packet, "salt", salt);
    set(packet, "signature", signature);
    set(packet, "timestamp", Instant.EPOCH);
    return packet;
  }

  private static KeyedPlayerChatPacket keyedPacket(String message, boolean unsigned,
      Instant expiry, byte[] signature, byte[] salt, boolean signedPreview) throws Exception {
    KeyedPlayerChatPacket packet = new KeyedPlayerChatPacket();
    set(packet, "message", message);
    set(packet, "unsigned", unsigned);
    set(packet, "expiry", expiry);
    set(packet, "signature", signature);
    set(packet, "salt", salt);
    set(packet, "signedPreview", signedPreview);
    return packet;
  }

  private static void set(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
