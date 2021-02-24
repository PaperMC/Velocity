package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

public class StringArrayArgumentTypeTests {

  private static final StringArrayArgumentType TYPE = StringArrayArgumentType.INSTANCE;

  @Test
  void testParseWord() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Hello");
    assertArrayEquals(new String[] { "Hello" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testParseString() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Hello world!");
    assertArrayEquals(new String[] { "Hello", "world!" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testNoEscaping() throws CommandSyntaxException {
    final StringReader reader = new StringReader("\"My house\" is blue");
    assertArrayEquals(new String[] { "\"My", "house\"", "is", "blue" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testUnbalancedEscapingIsIgnored() throws CommandSyntaxException {
    final StringReader reader = new StringReader("This is a \"sentence");
    assertArrayEquals(new String[] { "This", "is", "a", "\"sentence" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testLeadingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader(" ¡Hola!");
    assertArrayEquals(new String[] { "", "¡Hola!" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleLeadingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("   Anguish Languish");
    assertArrayEquals(new String[] { "", "", "", "Anguish", "Languish" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testTrailingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("This is a test. ");
    assertArrayEquals(new String[] { "This", "is", "a", "test.", "" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleTrailingWhitespace() throws CommandSyntaxException {
    final StringReader reader = new StringReader("Lorem ipsum  ");
    assertArrayEquals(new String[] { "Lorem", "ipsum", "", "" }, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }

  @Test
  void testMultipleWhitespaceCharsArePreserved() throws CommandSyntaxException {
    final StringReader reader = new StringReader(
            " This  is a   message  that shouldn't    be normalized  ");
    assertArrayEquals(new String[] {
      "", "This", "", "is", "a", "", "", "message", "", "that", "shouldn't", "", "", "", "be",
      "normalized", "", ""}, TYPE.parse(reader));
    assertFalse(reader.canRead());
  }
}
