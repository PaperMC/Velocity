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

package com.velocitypowered.proxy.event;

import static com.velocitypowered.proxy.testutil.FakePluginManager.PLUGIN_A;
import static com.velocitypowered.proxy.testutil.FakePluginManager.PLUGIN_B;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.proxy.testutil.FakePluginManager;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests event listener registration.
 */
public class RegistrationTest {

  private EventManager eventManager;

  @BeforeEach
  public final void setup() {
    resetEventManager();
  }

  private void resetEventManager() {
    eventManager = createEventManager(new FakePluginManager());
  }

  protected EventManager createEventManager(PluginManager pluginManager) {
    return new VelocityEventManager(pluginManager);
  }

  // Must be public in order to generate a method calling it
  private static class SimpleEvent {

    int value;
  }

  private static class SimpleSubclassedEvent extends SimpleEvent {

  }

  private static class HandlerListener implements EventHandler<SimpleEvent> {

    @Override
    public void execute(SimpleEvent event) {
      event.value++;
    }
  }

  private static class AnnotatedListener {

    @Subscribe
    public void increment(SimpleEvent event) {
      event.value++;
    }
  }

  private interface EventGenerator {

    void assertFiredEventValue(int value);
  }

  private interface TestFunction {

    void runTest(boolean annotated, EventGenerator generator);
  }

  private Stream<DynamicNode> composeTests(String name, TestFunction testFunction) {
    Set<DynamicNode> tests = new HashSet<>();
    boolean[] trueAndFalse = new boolean[]{true, false};
    for (boolean annotated : trueAndFalse) {
      for (boolean subclassed : trueAndFalse) {

        EventGenerator generator = (value) -> {
          SimpleEvent simpleEvent = (subclassed) ? new SimpleSubclassedEvent() : new SimpleEvent();
          SimpleEvent shouldBeSameEvent = eventManager.fire(simpleEvent).join();
          assertSame(simpleEvent, shouldBeSameEvent);
          assertEquals(value, simpleEvent.value);
        };
        tests.add(DynamicTest.dynamicTest(
            name + ". Annotated : " + annotated + ", Subclassed: " + subclassed, () -> {
              try {
                testFunction.runTest(annotated, generator);
              } finally {
                resetEventManager();
              }
            }));
      }
    }
    return tests.stream();
  }

  @TestFactory
  Stream<DynamicNode> simpleRegisterAndUnregister() {
    return composeTests("simpleRegisterAndUnregister", (annotated, generator) -> {
      if (annotated) {
        eventManager.register(PLUGIN_A, new AnnotatedListener());
      } else {
        eventManager.register(PLUGIN_A, SimpleEvent.class, new HandlerListener());
      }
      generator.assertFiredEventValue(1);
      eventManager.unregisterListeners(PLUGIN_A);
      generator.assertFiredEventValue(0);
      assertDoesNotThrow(() -> eventManager.unregisterListeners(PLUGIN_A),
          "Extra unregister is a no-op");
    });
  }

  @TestFactory
  Stream<DynamicNode> doubleRegisterListener() {
    return composeTests("doubleRegisterListener", (annotated, generator) -> {
      if (annotated) {
        Object annotatedListener = new AnnotatedListener();
        eventManager.register(PLUGIN_A, annotatedListener);
        eventManager.register(PLUGIN_A, annotatedListener);
      } else {
        EventHandler<SimpleEvent> handler = new HandlerListener();
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
      }
      generator.assertFiredEventValue(2);
    });
  }

  @TestFactory
  Stream<DynamicNode> doubleRegisterListenerDifferentPlugins() {
    return composeTests("doubleRegisterListenerDifferentPlugins", (annotated, generator) -> {
      if (annotated) {
        Object annotatedListener = new AnnotatedListener();
        eventManager.register(PLUGIN_A, annotatedListener);
        eventManager.register(PLUGIN_B, annotatedListener);
      } else {
        EventHandler<SimpleEvent> handler = new HandlerListener();
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
        eventManager.register(PLUGIN_B, SimpleEvent.class, handler);
      }
      generator.assertFiredEventValue(2);
    });
  }

  @TestFactory
  Stream<DynamicNode> doubleRegisterListenerThenUnregister() {
    return composeTests("doubleRegisterListenerThenUnregister", (annotated, generator) -> {
      if (annotated) {
        Object annotatedListener = new AnnotatedListener();
        eventManager.register(PLUGIN_A, annotatedListener);
        eventManager.register(PLUGIN_A, annotatedListener);
        eventManager.unregisterListener(PLUGIN_A, annotatedListener);
      } else {
        EventHandler<SimpleEvent> handler = new HandlerListener();
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
        eventManager.unregister(PLUGIN_A, handler);
      }
      generator.assertFiredEventValue(0);
    });
  }

  @TestFactory
  Stream<DynamicNode> doubleUnregisterListener() {
    return composeTests("doubleUnregisterListener", (annotated, generator) -> {
      if (annotated) {
        Object annotatedListener = new AnnotatedListener();
        eventManager.register(PLUGIN_A, annotatedListener);
        eventManager.unregisterListener(PLUGIN_A, annotatedListener);
        assertDoesNotThrow(() -> eventManager.unregisterListener(PLUGIN_A, annotatedListener),
            "Extra unregister is a no-op");
      } else {
        EventHandler<SimpleEvent> handler = new HandlerListener();
        eventManager.register(PLUGIN_A, SimpleEvent.class, handler);
        eventManager.unregister(PLUGIN_A, handler);
        assertDoesNotThrow(() -> eventManager.unregister(PLUGIN_A, handler),
            "Extra unregister is a no-op");
      }
      generator.assertFiredEventValue(0);
    });
  }

}
