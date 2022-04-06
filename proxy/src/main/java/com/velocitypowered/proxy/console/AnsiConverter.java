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

package com.velocitypowered.proxy.console;

import java.util.regex.Pattern;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecrell.terminalconsole.TerminalConsoleAppender;

final class AnsiConverter {
  private static final Pattern RGB_PATTERN = Pattern.compile(
      LegacyComponentSerializer.SECTION_CHAR + "#([0-9a-fA-F]){6}");
  private static final String RGB_ANSI = "\u001B[38;2;%d;%d;%dm";
  private static final String RESET = LegacyComponentSerializer.SECTION_CHAR + "r";

  static String convert(final String input) {
    return TerminalConsoleAppender.isAnsiSupported()
      ? convertHexColors(input)
      : stripHexColors(input);
  }

  static String convertHexColors(final String input) {
    return RGB_PATTERN.matcher(input).replaceAll(result -> {
      final int hex = Integer.decode(result.group().substring(1));
      final int red = hex >> 16 & 0xFF;
      final int green = hex >> 8 & 0xFF;
      final int blue = hex & 0xFF;
      return String.format(RGB_ANSI, red, green, blue);
    }) + RESET;
  }

  static String stripHexColors(final String input) {
    return RGB_PATTERN.matcher(input).replaceAll("");
  }
}
