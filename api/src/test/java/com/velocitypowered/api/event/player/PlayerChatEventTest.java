/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import java.lang.reflect.Proxy;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerChatEventTest {

  @Test
  void oldConstructorKeepsPlaintextBehavior() {
    PlayerChatEvent event = new PlayerChatEvent(dummyPlayer(), "hello");

    assertEquals("hello", event.getMessage());
    assertSame(PlayerChatEvent.ChatResult.allowed(), event.getResult());
    assertEquals(PlayerChatEvent.SignedState.UNSIGNED, event.getMessageInfo().getSignedState());
    assertFalse(event.getMessageInfo().getSignedMessage().isPresent());
  }

  @Test
  void messageInfoExposesOriginalSignedMessage() throws Exception {
    SignedMessage signedMessage = signedMessage("original");
    PlayerChatEvent event = new PlayerChatEvent(dummyPlayer(), "original",
        PlayerChatEvent.MessageInfo.signed(signedMessage));

    event.setResult(PlayerChatEvent.ChatResult.message("changed"));

    assertEquals("original", event.getMessage());
    assertEquals(PlayerChatEvent.SignedState.SIGNED, event.getMessageInfo().getSignedState());
    assertTrue(event.getMessageInfo().getSignedMessage().isPresent());
    assertSame(signedMessage, event.getMessageInfo().getSignedMessage().orElseThrow());
    assertEquals("original", event.getMessageInfo().getSignedMessage().orElseThrow().getMessage());
  }

  private static Player dummyPlayer() {
    return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
        (proxy, method, args) -> {
          if (method.getName().equals("toString")) {
            return "dummy";
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private static SignedMessage signedMessage(String message) throws Exception {
    PublicKey key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
    return new SignedMessage() {
      @Override
      public String getMessage() {
        return message;
      }

      @Override
      public UUID getSignerUuid() {
        return new UUID(0, 1);
      }

      @Override
      public boolean isPreviewSigned() {
        return false;
      }

      @Override
      public PublicKey getSigner() {
        return key;
      }

      @Override
      public Instant getExpiryTemporal() {
        return Instant.EPOCH;
      }

      @Override
      public byte[] getSignature() {
        return new byte[] {1};
      }
    };
  }
}
