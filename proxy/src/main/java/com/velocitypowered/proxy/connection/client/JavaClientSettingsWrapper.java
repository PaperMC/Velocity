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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.proxy.player.java.JavaClientSettings;
import com.velocitypowered.api.proxy.player.java.SkinParts;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundClientSettingsPacket;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

public class JavaClientSettingsWrapper implements JavaClientSettings {

  static final JavaClientSettings DEFAULT = new JavaClientSettingsWrapper(
      new ServerboundClientSettingsPacket("en_US", (byte) 10, 0, true, (short) 127, 1));

  private final ServerboundClientSettingsPacket settings;
  private final SkinParts parts;
  private @Nullable Locale locale;

  JavaClientSettingsWrapper(ServerboundClientSettingsPacket settings) {
    this.settings = settings;
    this.parts = new SkinParts((byte) settings.getSkinParts());
  }

  @Override
  public Locale locale() {
    if (locale == null) {
      locale = Locale.forLanguageTag(settings.getLocale().replaceAll("_", "-"));
    }
    return locale;
  }

  @Override
  public byte viewDistance() {
    return settings.getViewDistance();
  }

  @Override
  public ChatMode chatMode() {
    int chat = settings.getChatVisibility();
    if (chat < 0 || chat > 2) {
      return ChatMode.SHOWN;
    }
    return ChatMode.values()[chat];
  }

  @Override
  public boolean hasChatColors() {
    return settings.isChatColors();
  }

  @Override
  public SkinParts skinParts() {
    return parts;
  }

  @Override
  public MainHand mainHand() {
    return settings.getMainHand() == 1 ? MainHand.RIGHT : MainHand.LEFT;
  }


}
