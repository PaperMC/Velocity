package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.command.CommandSource;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;

abstract class CommandTestSuite {

  protected VelocityCommandManager manager;
  protected final CommandSource source = MockCommandSource.INSTANCE;

  @BeforeEach
  void setUp() {
    this.manager = CommandManagerTests.newManager();
  }

  final void assertHandled(final String input) {
    assertTrue(manager.executeAsync(source, input).join());
  }

  final void assertForwarded(final String input) {
    assertFalse(manager.executeAsync(source, input).join());
  }

  final void assertSuggestions(final String input, final String... expectedSuggestions) {
    final var actual = manager.offerSuggestions(source, input).join();
    assertEquals(Arrays.asList(expectedSuggestions), actual);
  }
}
