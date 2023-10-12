/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import static com.velocitypowered.api.permission.PermissionFunction.ALWAYS_TRUE;

import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.platform.facet.FacetPointers.Type;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

/**
 * Implements the Velocity console, including sending commands and being the recipient
 * of messages from plugins.
 */
public final class VelocityConsole extends SimpleTerminalConsole implements ConsoleCommandSource {

  private static final Logger logger = LogManager.getLogger(VelocityConsole.class);
  private static final ComponentLogger componentLogger = ComponentLogger
          .logger(VelocityConsole.class);

  private final VelocityServer server;
  private PermissionFunction permissionFunction = ALWAYS_TRUE;
  private final @NotNull Pointers pointers = ConsoleCommandSource.super.pointers().toBuilder()
      .withDynamic(PermissionChecker.POINTER, this::getPermissionChecker)
      .withDynamic(Identity.LOCALE, () -> ClosestLocaleMatcher.INSTANCE
          .lookupClosest(Locale.getDefault()))
      .withStatic(FacetPointers.TYPE, Type.CONSOLE)
      .build();

  public VelocityConsole(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message,
      @NonNull MessageType messageType) {
    componentLogger.info(message);
  }

  @Override
  public @NonNull Tristate getPermissionValue(@NonNull String permission) {
    return this.permissionFunction.getPermissionValue(permission);
  }

  /**
   * Sets up {@code System.out} and {@code System.err} to redirect to log4j.
   */
  public void setupStreams() {
    System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
    System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
  }

  /**
   * Sets up permissions for the console.
   */
  public void setupPermissions() {
    PermissionsSetupEvent event = new PermissionsSetupEvent(this, s -> ALWAYS_TRUE);
    // we can safely block here, this is before any listeners fire
    this.permissionFunction = this.server.getEventManager().fire(event).join().createFunction(this);
    if (this.permissionFunction == null) {
      logger.error(
          "A plugin permission provider {} provided an invalid permission function"
              + " for the console. This is a bug in the plugin, not in Velocity. Falling"
              + " back to the default permission function.",
          event.getProvider().getClass().getName());
      this.permissionFunction = ALWAYS_TRUE;
    }
  }

  @Override
  protected LineReader buildReader(LineReaderBuilder builder) {
    return super.buildReader(builder
        .appName("Velocity")
        .completer((reader, parsedLine, list) -> {
          try {
            List<String> offers = this.server.getCommandManager()
                .offerSuggestions(this, parsedLine.line())
                .join(); // Console doesn't get harmed much by this...
            for (String offer : offers) {
              list.add(new Candidate(offer));
            }
          } catch (Exception e) {
            logger.error("An error occurred while trying to perform tab completion.", e);
          }
        })
    );
  }

  @Override
  protected boolean isRunning() {
    return !this.server.isShutdown();
  }

  @Override
  protected void runCommand(String command) {
    try {
      if (!this.server.getCommandManager().executeAsync(this, command).join()) {
        sendMessage(Component.translatable("velocity.command.command-does-not-exist",
            NamedTextColor.RED));
      }
    } catch (Exception e) {
      logger.error("An error occurred while running this command.", e);
    }
  }

  @Override
  protected void shutdown() {
    this.server.shutdown(true);
  }

  @Override
  public @NotNull Pointers pointers() {
    return pointers;
  }
}
