/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public interface ChatTracker {

  default void pushHeader(HeaderData headerData) {}

  default @Nullable Component pushPlayerMessage(SignedChatMessage message) {
    return null;
  }

  static final ChatTracker DUMMY_TRACKER = new ChatTracker() {};
  static final Component OUT_OF_ORDER_CHAT = Component.translatable("multiplayer.disconnect.out_of_order_chat");
  static final Component UNSIGNED_CHAT = Component.translatable("multiplayer.disconnect.unsigned_chat");
  static final Component VALIDATION_FAILED = Component.translatable("multiplayer.disconnect.chat_validation_failed");

  public static ChatTracker forKey(@Nullable IdentifiedKey key) {
    if (key == null) {
      return DUMMY_TRACKER;
    }
    switch (key.getKeyRevision()) {
      case LINKED_V2:
        return new ChainedChatTracker(key);
      case GENERIC_V1:
        return new SequenceChatTracker();
      default:
        throw new IllegalArgumentException("Unknown key revision");
    }
  }

  public static class SequenceChatTracker implements ChatTracker {

    private AtomicReference<Instant> lastTimestamp = new AtomicReference<>(Instant.EPOCH);

    private SequenceChatTracker() {}

    @Override
    public @Nullable Component pushPlayerMessage(SignedChatMessage message) {
      Preconditions.checkNotNull(message);
      final Instant nextTimestamp = message.getExpiryTemporal();
      Instant previous;
      do {
        previous = lastTimestamp.get();
        if (previous.isBefore(nextTimestamp)) {
          return OUT_OF_ORDER_CHAT;
        }
      } while (lastTimestamp.compareAndSet(previous, nextTimestamp));
      return null;
    }
  }

  public static class ChainedChatTracker implements ChatTracker {
    // This list tracks the pending last seen data
    private List<HeaderData> passingData = new ArrayList<>();
    private SignedChatMessage lastMessage;
    private final Object internalLock = new Object();
    private final IdentifiedKey key;

    private ChainedChatTracker(IdentifiedKey key) {
      this.key = key;
    }

    public @Nullable Component pushPlayerMessage(SignedChatMessage message) {
      Preconditions.checkNotNull(message);
      // message lastseen timestamp
      synchronized (internalLock) {
        // Check duplicate profiles
        final SignaturePair[] updatedSeen = message.getSeenSignatures();
        int sizeGood = Arrays.stream(updatedSeen).map(SignaturePair::getSigner)
                .collect(Collectors.toCollection(HashSet::new)).size();
        if (sizeGood != updatedSeen.length) {
          return VALIDATION_FAILED;
        }

        if (lastMessage != null) {
          // Verify sequence
          if (lastMessage.getExpiryTemporal().isBefore(message.getExpiryTemporal())) {
            return OUT_OF_ORDER_CHAT;
          }
          // Verify chain
          SignaturePair previousSignature = lastMessage.getPreviousSignature();
          if (previousSignature == null || !Arrays.equals(previousSignature.getSignature(),
                  message.getSignature()) || !previousSignature.getSigner().equals(key.getSignatureHolder())) {
            return UNSIGNED_CHAT;
          }

          // This next part is vastly different from Mojangs logic.
          // Though I *hope* I understood most of it. -Five

          // At least itself needs to be on here
          if (updatedSeen.length > 0) {
            // Retrieve tracked data
            List<SignaturePair> knownData = getPairData(passingData);
            // Retrieve range (Note: Order in reverse for client)
            int firstIndex = knownData.indexOf(updatedSeen[updatedSeen.length-1]);
            int lastIndex = knownData.indexOf(updatedSeen[0]);
            // Range check
            if (firstIndex < 0 || lastIndex < 0 || knownData.size() <= lastIndex || lastIndex > firstIndex) {
              return VALIDATION_FAILED;
            }
            // Filter the newest messages from the list
            List<SignaturePair> verifiable = uniqueLastPairReverse(knownData.subList(firstIndex, lastIndex));
            // Check if messages were removed
            if (verifiable.size() != updatedSeen.length) {
              return VALIDATION_FAILED;
            }
            // Actual validation
            for (int s = 0; s < verifiable.size(); s++) {
              if (!verifiable.get(s).equals(updatedSeen[s])) {
                return VALIDATION_FAILED;
              }
            }
            // re-set seen data
            passingData = passingData.subList(0, lastIndex);

          } else {
            return VALIDATION_FAILED;
          }
        }

        lastMessage = message;
      }
      return null;
    }

    private static List<SignaturePair> getPairData(List<HeaderData> data){
      List<SignaturePair> ret = new ArrayList<>();
      for (HeaderData d : data) {
        ret.add(d.getHeader());
      }
      return ret;
    }

    private static List<SignaturePair> uniqueLastPairReverse(List<SignaturePair> data){
      List<SignaturePair> ret = new ArrayList<>();
      Set<UUID> seen = new HashSet<UUID>();
      for (int i = data.size() - 1; i >= 0; i--) {
        SignaturePair s = data.get(i);
        if (seen.add(s.getSigner())) {
          ret.add(s);
        }
      }
      return ret;
    }

    public void pushServerHeader(HeaderData toPush) {
      synchronized (internalLock) {
        passingData.add(toPush);
      }
    }
  }

}
