/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.command;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VelocityCommands}.
 */
public class VelocityCommandsTests {

  @Test
  void testLiteralKeepsRedirectModifierWithFork() {
    final var target = LiteralArgumentBuilder.<CommandSource>literal("target").build();
    final RedirectModifier<CommandSource> modifier = context -> List.of(context.getSource());
    final var original = LiteralArgumentBuilder.<CommandSource>literal("origin")
        .fork(target, modifier)
        .build();

    final var wrapped = VelocityCommands.wrap(original, new Object());

    assertNotNull(wrapped.getRedirect());
    assertEquals("target", wrapped.getRedirect().getName());
    assertSame(modifier, wrapped.getRedirectModifier());
    assertTrue(wrapped.isFork());
  }

  @Test
  void testArgumentKeepsRedirectModifierWithoutFork() {
    final var target = LiteralArgumentBuilder.<CommandSource>literal("target").build();
    final RedirectModifier<CommandSource> modifier = context -> List.of(context.getSource());
    final var original = RequiredArgumentBuilder.<CommandSource, String>argument("value", word())
        .forward(target, modifier, false)
        .build();

    final var wrapped = VelocityCommands.wrap(original, new Object());

    assertNotNull(wrapped.getRedirect());
    assertEquals("target", wrapped.getRedirect().getName());
    assertSame(modifier, wrapped.getRedirectModifier());
    assertFalse(wrapped.isFork());
  }

  @Test
  void testLiteralExecutesWithRedirectModifier() throws Exception {
    final var modifierCalls = new AtomicInteger();
    final var commandCalls = new AtomicInteger();
    final var target = LiteralArgumentBuilder
        .<CommandSource>literal("target")
        .then(LiteralArgumentBuilder
            .<CommandSource>literal("run")
            .executes(context -> {
              commandCalls.incrementAndGet();
              return 1;
            }))
        .build();
    final RedirectModifier<CommandSource> modifier = context -> {
      modifierCalls.incrementAndGet();
      return List.of(context.getSource(), context.getSource());
    };
    final var original = LiteralArgumentBuilder
        .<CommandSource>literal("origin")
        .fork(target, modifier)
        .build();
    final var dispatcher = new CommandDispatcher<CommandSource>();
    dispatcher.getRoot().addChild(VelocityCommands.wrap(original, new Object()));

    assertEquals(2, dispatcher.execute("origin run", MockCommandSource.INSTANCE));
    assertEquals(1, modifierCalls.get());
    assertEquals(2, commandCalls.get());
  }

  @Test
  void testArgumentExecutesWithRedirectModifier() throws Exception {
    final var modifierCalls = new AtomicInteger();
    final var commandCalls = new AtomicInteger();
    final var target = LiteralArgumentBuilder
        .<CommandSource>literal("target")
        .then(LiteralArgumentBuilder
            .<CommandSource>literal("run")
            .executes(context -> {
              commandCalls.incrementAndGet();
              return 1;
            }))
        .build();
    final RedirectModifier<CommandSource> modifier = context -> {
      modifierCalls.incrementAndGet();
      return List.of(context.getSource(), context.getSource());
    };
    final var original = LiteralArgumentBuilder
        .<CommandSource>literal("origin")
        .then(RequiredArgumentBuilder
            .<CommandSource, String>argument("value", word())
            .forward(target, modifier, false))
        .build();
    final var dispatcher = new CommandDispatcher<CommandSource>();
    dispatcher.getRoot().addChild(VelocityCommands.wrap(original, new Object()));

    assertEquals(2, dispatcher.execute("origin value run", MockCommandSource.INSTANCE));
    assertEquals(1, modifierCalls.get());
    assertEquals(2, commandCalls.get());
  }
}
