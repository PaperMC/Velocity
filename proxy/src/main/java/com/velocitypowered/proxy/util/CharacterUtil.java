/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

/**
 * Utilities for handling characters in the context of Minecraft chat handling.
 */
public final class CharacterUtil {

  /**
   * Checks if a character is allowed.
   *
   * @param c character to check
   * @return true if the character is allowed
   */
  public static boolean isAllowedCharacter(char c) {
    // 167 = §, 127 = DEL
    // https://minecraft.wiki/w/Chat
    return c != 167 && c >= ' ' && c != 127;
  }

  /**
   * It is not possible to send certain characters in the chat like: section symbol, DEL, and all
   * characters below space. Checks if a message contains illegal characters.
   *
   * @param message the message to check
   * @return true if the message contains illegal characters
   */
  public static boolean containsIllegalCharacters(String message) {
    for (int i = 0; i < message.length(); i++) {
      if (!isAllowedCharacter(message.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Escapes control characters before text is written to a log.
   *
   * @param value text to escape
   * @return text without raw control characters
   */
  public static String escapeControlCharacters(String value) {
    StringBuilder result = null;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      String replacement;
      switch (c) {
        case '\b' -> replacement = "\\b";
        case '\t' -> replacement = "\\t";
        case '\n' -> replacement = "\\n";
        case '\f' -> replacement = "\\f";
        case '\r' -> replacement = "\\r";
        default -> {
          if (!Character.isISOControl(c)) {
            if (result != null) {
              result.append(c);
            }
            continue;
          }
          replacement = "\\u" + String.format("%04x", (int) c);
        }
      }
      if (result == null) {
        result = new StringBuilder(value.length() + 8);
        result.append(value, 0, i);
      }
      result.append(replacement);
    }
    return result == null ? value : result.toString();
  }
}
