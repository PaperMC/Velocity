/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.SkinParts;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import java.util.Locale;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wraps the settings received in the Client Settings packet.
 */
public class ClientSettingsWrapper implements PlayerSettings {

  static final PlayerSettings DEFAULT = new ClientSettingsWrapper(
      new ClientSettingsPacket("en_us", (byte) 2, 0, true, (short) 0, 1, false, false, 0));

  private final ClientSettingsPacket settings;
  private final SkinParts parts;
  private @Nullable Locale locale;

  ClientSettingsWrapper(ClientSettingsPacket settings) {
    this.settings = settings;
    this.parts = new SkinParts((byte) settings.getSkinParts());
  }

  @Override
  public Locale getLocale() {
    if (locale == null) {
      locale = Locale.forLanguageTag(settings.getLocale().replaceAll("_", "-"));
    }
    return locale;
  }

  @Override
  public byte getViewDistance() {
    return settings.getViewDistance();
  }

  @Override
  public ChatMode getChatMode() {
    return switch (settings.getChatVisibility()) {
      case 1 -> ChatMode.COMMANDS_ONLY;
      case 2 -> ChatMode.HIDDEN;
      default -> ChatMode.SHOWN;
    };
  }

  @Override
  public boolean hasChatColors() {
    return settings.isChatColors();
  }

  @Override
  public SkinParts getSkinParts() {
    return parts;
  }

  @Override
  public MainHand getMainHand() {
    return settings.getMainHand() == 1 ? MainHand.RIGHT : MainHand.LEFT;
  }

  @Override
  public boolean isClientListingAllowed() {
    return settings.isClientListingAllowed();
  }

  @Override
  public boolean isTextFilteringEnabled() {
    return settings.isTextFilteringEnabled();
  }

  @Override
  public ParticleStatus getParticleStatus() {
    return switch (settings.getParticleStatus()) {
      case 1 -> ParticleStatus.DECREASED;
      case 2 -> ParticleStatus.MINIMAL;
      default -> ParticleStatus.ALL;
    };
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientSettingsWrapper that = (ClientSettingsWrapper) o;
    return Objects.equals(settings, that.settings) && Objects.equals(parts, that.parts)
        && Objects.equals(locale, that.locale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settings, parts, locale);
  }
}
