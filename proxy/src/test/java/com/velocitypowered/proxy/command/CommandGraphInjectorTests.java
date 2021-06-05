package com.velocitypowered.proxy.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandGraphInjectorTests {

  private CommandDispatcher<Object> dispatcher;
  private Lock lock;
  private CommandGraphInjector<Object> injector;

  @BeforeEach
  void setUp() {
    this.dispatcher = new CommandDispatcher<>();
    this.lock = new ReentrantLock();
    this.injector = new CommandGraphInjector<>(this.dispatcher, this.lock);
  }

  // TODO

  @Test
  void testInjectInvocableCommand() {
    // Preserves arguments node and hints
  }

  @Test
  void testFiltersImpermissibleAlias() {

  }

  @Test
  void testInjectsBrigadierCommand() {

  }

  @Test
  void testFiltersImpermissibleBrigadierCommandChildren() {

  }

  @Test
  void testInjectFiltersBrigadierCommandRedirects() {

  }

  @Test
  void testInjectOverridesAliasInDestination() {

  }
}
