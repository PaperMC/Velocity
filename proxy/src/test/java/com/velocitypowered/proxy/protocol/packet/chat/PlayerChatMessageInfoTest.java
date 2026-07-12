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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.player.PlayerChatMessage;
import com.velocitypowered.api.event.player.PlayerChatProtocol;
import com.velocitypowered.api.event.player.PlayerChatSignedState;
import com.velocitypowered.api.event.player.PlayerChatValidationFlag;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.api.proxy.player.ChatSession;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.crypto.SignaturePair;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.tablist.InternalTabList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.BitSet;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class PlayerChatMessageInfoTest {

  private static final UUID PLAYER_ID = new UUID(0, 42);
  private static final UUID SESSION_ID = new UUID(1, 99);
  private static final Instant EXPIRY = Instant.ofEpochMilli(123456789L);

  @Test
  void legacyChatHasNoFabricatedSessionOrSignature() {
    ConnectedPlayer player = playerWithSession(null, null);
    PlayerChatMessage message = PlayerChatMessageInfo.legacyMessage(player, "legacy body");

    assertEquals(PlayerChatProtocol.LEGACY_UNSIGNED, message.getProtocol());
    assertEquals(PlayerChatSignedState.LEGACY, message.getSignedState());
    assertFalse(message.getSignature().isPresent());
    assertFalse(message.getSessionInfo().isPresent());
  }

  @Test
  void modernUnsignedChatKeepsBodyWithoutSignedMessage() throws Exception {
    SessionPlayerChatPacket packet = sessionPacket("unsigned body", false, 0L, new byte[0]);
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(playerWithSession(null, null),
        packet);

    assertEquals(PlayerChatProtocol.SESSION_CHAT, message.getProtocol());
    assertEquals(PlayerChatSignedState.UNSIGNED, message.getSignedState());
    assertEquals("unsigned body", message.getMessage());
    assertFalse(message.getSignature().isPresent());
    assertFalse(message.getSessionInfo().isPresent());
  }

  @Test
  void keyedSignedChatPreservesLegacyKeySignatureExpiryAndChain() throws Exception {
    byte[] signature = new byte[] {4, 5, 6};
    byte[] salt = new byte[] {7, 8};
    SignaturePair previous = new SignaturePair(new UUID(2, 3), new byte[] {9});
    KeyedPlayerChatPacket packet = keyedPacket("keyed body", false, EXPIRY, signature, salt,
        true, new SignaturePair[] {previous}, null);
    PlayerChatMessage message = PlayerChatMessageInfo.keyedMessage(playerWithSession(key(), null),
        packet);

    assertEquals(PlayerChatSignedState.KEYED_SIGNED, message.getSignedState());
    assertEquals(EXPIRY, message.getKeyInfo().orElseThrow().getKeyExpiry());
    assertArrayEquals(signature, message.getSignature().orElseThrow().getSignature());
    assertArrayEquals(salt, message.getSignature().orElseThrow().getSaltBytes().orElseThrow());
    assertTrue(message.getSignature().orElseThrow().isPreviewSigned());
    assertEquals(previous.getSigner(), message.getChainInfo().orElseThrow()
        .getPreviousMessages().get(0).getSigner());
    assertFalse(message.hasValidationFlag(PlayerChatValidationFlag.SIGNATURE_VALIDATED));
  }

  @Test
  void sessionSignedChatUsesRemoteChatSessionAndPreservesPacketFields() throws Exception {
    byte[] signature = new byte[] {1, 2, 3};
    BitSet acknowledged = new BitSet();
    acknowledged.set(2);
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, signature);
    set(packet, "lastSeenMessages", new LastSeenMessages(4, acknowledged, (byte) 8, true));
    IdentifiedKey key = key();
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(playerWithSession(null,
        new RemoteChatSession(SESSION_ID, key)), packet);

    assertEquals(PlayerChatSignedState.SESSION_SIGNED, message.getSignedState());
    assertEquals("signed body", message.getMessage());
    assertEquals(SESSION_ID, message.getSessionInfo().orElseThrow().getSessionId());
    assertSame(key.getSignedPublicKey(), message.getSessionInfo().orElseThrow().getPublicKey());
    assertArrayEquals(signature, message.getSignature().orElseThrow().getSignature());
    assertEquals(Instant.EPOCH, message.getSignature().orElseThrow().getTimestamp().orElseThrow());
    assertEquals(7L, message.getSignature().orElseThrow().getSalt().orElseThrow());
    assertEquals(4, message.getChainInfo().orElseThrow().getLastSeenOffset().orElseThrow());
    assertTrue(message.getChainInfo().orElseThrow().getAcknowledged().orElseThrow().get(2));
    assertEquals((byte) 8, message.getChainInfo().orElseThrow().getChecksum().orElseThrow());
    assertTrue(message.hasValidationFlag(PlayerChatValidationFlag.KEY_AVAILABLE));
    assertTrue(message.hasValidationFlag(PlayerChatValidationFlag.SESSION_MATCHED));
    assertFalse(message.hasValidationFlag(PlayerChatValidationFlag.SIGNATURE_VALIDATED));
  }

  @Test
  void signedSessionPacketWithoutSessionDoesNotFabricateCompleteMetadata() throws Exception {
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, new byte[] {1});
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(playerWithSession(null, null),
        packet);

    assertEquals(PlayerChatSignedState.SIGNED, message.getSignedState());
    assertTrue(message.getSignature().isPresent());
    assertFalse(message.getSessionInfo().isPresent());
    assertFalse(message.hasValidationFlag(PlayerChatValidationFlag.KEY_AVAILABLE));
    assertTrue(message.hasValidationFlag(PlayerChatValidationFlag.VALIDATION_UNAVAILABLE));
  }

  @Test
  void sessionReplacementUsesNewestSession() throws Exception {
    IdentifiedKey oldKey = key();
    IdentifiedKey newKey = key();
    InternalTabList tabList = tabList(new RemoteChatSession(SESSION_ID, oldKey));
    ConnectedPlayer player = playerWithTabList(null, tabList);

    PlayerChatMessage first = PlayerChatMessageInfo.sessionMessage(player,
        sessionPacket("first", true, 1L, new byte[] {1}));
    Optional<TabListEntry> newEntry = Optional.of(entry(new RemoteChatSession(new UUID(4, 5),
        newKey)));
    when(tabList.getEntry(PLAYER_ID)).thenReturn(newEntry);
    PlayerChatMessage second = PlayerChatMessageInfo.sessionMessage(player,
        sessionPacket("second", true, 2L, new byte[] {2}));

    assertSame(oldKey.getSignedPublicKey(), first.getSessionInfo().orElseThrow().getPublicKey());
    assertSame(newKey.getSignedPublicKey(), second.getSessionInfo().orElseThrow().getPublicKey());
    assertEquals(new UUID(4, 5), second.getSessionInfo().orElseThrow().getSessionId());
  }

  @Test
  void serverSwitchKeepsSenderIdentityAndUsesCurrentSessionAssociation() throws Exception {
    ConnectedPlayer player = playerWithSession(null, new RemoteChatSession(SESSION_ID, key()));
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(player,
        sessionPacket("body", true, 7L, new byte[] {1}));

    assertSame(player, message.getSender());
    assertEquals(PLAYER_ID, message.getSender().getUniqueId());
    assertEquals(SESSION_ID, message.getSessionInfo().orElseThrow().getSessionId());
  }

  @Test
  void mutableSignatureAndChainDataCannotBeMutatedThroughApi() throws Exception {
    byte[] signature = new byte[] {9, 10, 11};
    BitSet acknowledged = new BitSet();
    acknowledged.set(1);
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, signature);
    set(packet, "lastSeenMessages", new LastSeenMessages(0, acknowledged, (byte) 0, false));
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(playerWithSession(null,
        new RemoteChatSession(SESSION_ID, key())), packet);

    signature[0] = 99;
    message.getSignature().orElseThrow().getSignature()[1] = 88;
    message.getChainInfo().orElseThrow().getAcknowledged().orElseThrow().clear(1);

    assertArrayEquals(new byte[] {9, 10, 11}, message.getSignature().orElseThrow().getSignature());
    assertTrue(message.getChainInfo().orElseThrow().getAcknowledged().orElseThrow().get(1));
  }

  @Test
  void clientboundDecoratedPlayerChatKeepsOriginalBodySeparateFromDecoration() throws Exception {
    assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion.MINECRAFT_1_19_3);
    assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion.MINECRAFT_1_20_5);
    assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion.MINECRAFT_1_21_4);
    assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion.MINECRAFT_1_21_5);
    assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion.MINECRAFT_26_1);
  }

  @Test
  void clientboundPlayerChatSupportStartsAtSessionChatProtocols() {
    assertFalse(ClientboundPlayerChatPacket.supportsProtocol(ProtocolVersion.MINECRAFT_1_19_1));
    assertTrue(ClientboundPlayerChatPacket.supportsProtocol(ProtocolVersion.MINECRAFT_1_19_3));
  }

  @Test
  void decoratedPlayerChatEmissionRequiresCompleteSessionSignature() throws Exception {
    PlayerChatMessage unsigned = PlayerChatMessageInfo.sessionMessage(playerWithSession(null, null),
        sessionPacket("unsigned", false, 0L, new byte[0]));
    PlayerChatMessage keyed = PlayerChatMessageInfo.keyedMessage(playerWithSession(key(), null),
        keyedPacket("keyed", false, EXPIRY, new byte[256], new byte[] {0, 0, 0, 0, 0, 0, 0, 1},
            false, new SignaturePair[0], null));
    PlayerChatMessage shortSignature = PlayerChatMessageInfo.sessionMessage(playerWithSession(null,
        new RemoteChatSession(SESSION_ID, key())), sessionPacket("short", true, 1L,
        new byte[] {1}));

    assertFalse(DecoratedPlayerChatForwarder.canEmitAsDecoratedPlayerChat(unsigned));
    assertFalse(DecoratedPlayerChatForwarder.canEmitAsDecoratedPlayerChat(keyed));
    assertFalse(DecoratedPlayerChatForwarder.canEmitAsDecoratedPlayerChat(shortSignature));
  }

  private static void assertClientboundDecoratedPlayerChatRoundTrips(ProtocolVersion version)
      throws Exception {
    byte[] signature = new byte[256];
    signature[0] = 9;
    PlayerChatMessage message = PlayerChatMessageInfo.sessionMessage(playerWithSession(null,
        new RemoteChatSession(SESSION_ID, key())), sessionPacket("Paper test", true, 7L,
        signature));
    ClientboundPlayerChatPacket packet = new ClientboundPlayerChatPacket(message,
        Component.text("[G][server1] [Air]airgalaxie: Paper test"),
        Component.text("airgalaxie"), version);
    ByteBuf buf = Unpooled.buffer();

    packet.encode(buf, ProtocolUtils.Direction.CLIENTBOUND, version);
    ClientboundPlayerChatPacket decoded = new ClientboundPlayerChatPacket();
    decoded.decode(buf, ProtocolUtils.Direction.CLIENTBOUND, version);

    assertEquals("Paper test", decoded.getMessage());
    assertArrayEquals(signature, decoded.getSignature());
    assertEquals(Component.text("[G][server1] [Air]airgalaxie: Paper test"),
        decoded.getUnsignedContent());
  }

  @Test
  void compatibilitySignedMessageViewExistsWhenMetadataIsComplete() throws Exception {
    SessionPlayerChatPacket packet = sessionPacket("signed body", true, 7L, new byte[] {1});
    SignedMessage signedMessage = PlayerChatMessageInfo.fromSessionPacket(playerWithSession(null,
        new RemoteChatSession(SESSION_ID, key())), packet).getSignedMessage().orElseThrow();

    assertEquals("signed body", signedMessage.getMessage());
    assertEquals(PLAYER_ID, signedMessage.getSignerUuid());
    assertArrayEquals(packet.getSaltBytes(), signedMessage.getSalt());
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

  private static ConnectedPlayer playerWithSession(IdentifiedKey identifiedKey,
      ChatSession chatSession) {
    return playerWithTabList(identifiedKey, tabList(chatSession));
  }

  private static ConnectedPlayer playerWithTabList(IdentifiedKey identifiedKey,
      InternalTabList tabList) {
    ConnectedPlayer player = mock(ConnectedPlayer.class);
    when(player.getIdentifiedKey()).thenReturn(identifiedKey);
    when(player.getUniqueId()).thenReturn(PLAYER_ID);
    when(player.getTabList()).thenReturn(tabList);
    return player;
  }

  private static InternalTabList tabList(ChatSession chatSession) {
    InternalTabList tabList = mock(InternalTabList.class);
    Optional<TabListEntry> entry = chatSession == null ? Optional.empty() : Optional.of(entry(chatSession));
    when(tabList.getEntry(PLAYER_ID)).thenReturn(entry);
    return tabList;
  }

  private static TabListEntry entry(ChatSession chatSession) {
    TabListEntry entry = mock(TabListEntry.class);
    when(entry.getChatSession()).thenReturn(chatSession);
    return entry;
  }

  private static IdentifiedKey key() throws Exception {
    IdentifiedKey key = mock(IdentifiedKey.class);
    PublicKey publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

    when(key.getSignedPublicKey()).thenReturn(publicKey);
    when(key.getExpiryTemporal()).thenReturn(EXPIRY);
    when(key.getSignatureHolder()).thenReturn(PLAYER_ID);
    return key;
  }

  private static SessionPlayerChatPacket sessionPacket(String message, boolean signed, long salt,
      byte[] signature) throws Exception {
    SessionPlayerChatPacket packet = new SessionPlayerChatPacket();
    set(packet, "message", message);
    set(packet, "signed", signed);
    set(packet, "salt", salt);
    set(packet, "signature", signature);
    set(packet, "timestamp", Instant.EPOCH);
    set(packet, "lastSeenMessages", new LastSeenMessages());
    return packet;
  }

  private static KeyedPlayerChatPacket keyedPacket(String message, boolean unsigned,
      Instant expiry, byte[] signature, byte[] salt, boolean signedPreview,
      SignaturePair[] previousMessages, SignaturePair lastMessage) throws Exception {
    KeyedPlayerChatPacket packet = new KeyedPlayerChatPacket();
    set(packet, "message", message);
    set(packet, "unsigned", unsigned);
    set(packet, "expiry", expiry);
    set(packet, "signature", signature);
    set(packet, "salt", salt);
    set(packet, "signedPreview", signedPreview);
    set(packet, "previousMessages", previousMessages);
    set(packet, "lastMessage", lastMessage);
    return packet;
  }

  private static void set(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
