package com.velocitypowered.proxy.command;

import static org.junit.jupiter.api.Assertions.*;

import com.velocitypowered.api.command.CommandSource;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;

abstract class CommandTestSuite {

  protected VelocityCommandManager manager;
  protected final CommandSource dummySource = MockCommandSource.INSTANCE;

  @BeforeEach
  void setUp() {
    this.manager = new VelocityCommandManager(CommandManagerTests.EVENT_MANAGER);
  }

  final void assertExecuted(final String cmdLine) {
    assertTrue(manager.executeAsync(dummySource, cmdLine).join());
  }

  final void assertNotExecuted(final String cmdLine) {
    assertFalse(manager.executeAsync(dummySource, cmdLine).join());
  }

  final void assertSuggestions(final String cmdLine, final String... expectedSuggestions) {
    final List<String> actual = manager.offerSuggestions(dummySource, cmdLine).join();
    final List<String> expectedAsList = Arrays.asList(expectedSuggestions);
    assertEquals(expectedAsList, actual);
  }

  /*abstract void testNoArgsExecution();

  abstract void testSingleArgExecution();

  abstract void testMultipleArgsExecution();

  abstract void testAliasSuggestion();

  abstract void testNoSuggestions();

  abstract void testSingleSuggestion();

  abstract void testMultipleSuggestions();*/

  // TODO Permission checks
  // TODO Assert suggestions do permission-checking
}
