package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.velocitypowered.api.command.CommandSource;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

abstract class CommandTestSuite {

  protected VelocityCommandManager manager;
  protected final CommandSource source = MockCommandSource.INSTANCE;

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(CommandManagerTests.EVENT_MANAGER);
  }

  final void assertNotForwarded(final String cmdLine) {
    assertTrue(manager.executeAsync(source, cmdLine).join());
  }

  final void assertForwarded(final String cmdLine) {
    assertFalse(manager.executeAsync(source, cmdLine).join());
  }

  final void assertSuggestions(final String cmdLine, final String... expectedSuggestions) {
    final List<String> actual = manager.offerSuggestions(source, cmdLine).join();
    final List<String> expectedAsList = Arrays.asList(expectedSuggestions);
    assertEquals(expectedAsList, actual);
  }
}
