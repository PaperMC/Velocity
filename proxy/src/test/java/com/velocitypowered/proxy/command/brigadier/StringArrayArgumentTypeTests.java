/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.command.brigadier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StringArrayArgumentType}.
 */
public class StringArrayArgumentTypeTests {

  private static final StringArrayArgumentType TYPE = StringArrayArgumentType.INSTANCE;

  @Test
  void testEmptyString() throws CommandSyntaxException {
    final StringReader reader = new StringReader("");
    assertArrayEquals(new String[0], TYPE.parse(reader));
  }

  @Test
  void testParseWord() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Hello");
    assertArrayEquals(new String[]{"Hello"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testParseString() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Hello world!");
    assertArrayEquals(new String[]{"Hello", "world!"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testNoEscaping() throws CommandSyntaxException {
    final StringReader reader = new StringReader("\"My house\" is blue");
    assertArrayEquals(new String[]{"\"My", "house\"", "is", "blue"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testUnbalancedEscapingIsIgnored() throws CommandSyntaxException {
    final StringReader reader = new StringReader("This is a \"sentence");
    assertArrayEquals(new String[]{"This", "is", "a", "\"sentence"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testLeadingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader(" ¡Hola!");
    assertArrayEquals(new String[]{"", "¡Hola!"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleLeadingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("   Anguish Languish");
    assertArrayEquals(new String[]{"", "", "", "Anguish", "Languish"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testTrailingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("This is a test. ");
    assertArrayEquals(new String[]{"This", "is", "a", "test.", ""}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleTrailingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Lorem ipsum  ");
    assertArrayEquals(new String[]{"Lorem", "ipsum", "", ""}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleWhitespaceCharsArePreserved() throws CommandSyntaxException {
    final StringReader reader = new StringReader(
        " This  is a   message  that shouldn't    be normalized  ");
    assertArrayEquals(new String[]{
        "", "This", "", "is", "a", "", "", "message", "", "that", "shouldn't", "", "", "", "be",
        "normalized", "", ""}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testRespectsCursor() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Hello beautiful world");
    reader.setCursor(6);

    assertArrayEquals(new String[]{"beautiful", "world"}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }
}
