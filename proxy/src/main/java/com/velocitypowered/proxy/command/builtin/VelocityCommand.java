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

package com.velocitypowered.proxy.command.builtin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.util.InformationUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Implements the {@code /velocity} command and friends.
 */
public class VelocityCommand implements SimpleCommand {

  private interface SubCommand {

    void execute(final CommandSource source, final String @NonNull [] args);

    default List<String> suggest(final CommandSource source, final String @NonNull [] currentArgs) {
      return ImmutableList.of();
    }

    boolean hasPermission(final CommandSource source, final String @NonNull [] args);
  }

  private final Map<String, SubCommand> commands;

  /**
   * Initializes the command object for /velocity.
   *
   * @param server the Velocity server
   */
  public VelocityCommand(VelocityServer server) {
    this.commands = ImmutableMap.<String, SubCommand>builder()
        .put("version", new Info(server))
        .put("plugins", new Plugins(server))
        .put("reload", new Reload(server))
        .put("dump", new Dump(server))
        .put("heap", new Heap())
        .build();
  }

  private void usage(CommandSource source) {
    String availableCommands = commands.entrySet().stream()
        .filter(e -> e.getValue().hasPermission(source, new String[0]))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining("|"));
    String commandText = "/velocity <" + availableCommands + ">";
    source.sendMessage(Component.text(commandText, NamedTextColor.RED));
  }

  @Override
  public void execute(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      usage(source);
      return;
    }

    SubCommand command = commands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      usage(source);
      return;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    command.execute(source, actualArgs);
  }

  @Override
  public List<String> suggest(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] currentArgs = invocation.arguments();

    if (currentArgs.length == 0) {
      return commands.entrySet().stream()
          .filter(e -> e.getValue().hasPermission(source, new String[0]))
          .map(Map.Entry::getKey)
          .collect(ImmutableList.toImmutableList());
    }

    if (currentArgs.length == 1) {
      return commands.entrySet().stream()
          .filter(e -> e.getKey().regionMatches(true, 0, currentArgs[0], 0,
              currentArgs[0].length()))
          .filter(e -> e.getValue().hasPermission(source, new String[0]))
          .map(Map.Entry::getKey)
          .collect(ImmutableList.toImmutableList());
    }

    SubCommand command = commands.get(currentArgs[0].toLowerCase(Locale.US));
    if (command == null) {
      return ImmutableList.of();
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(currentArgs, 1, currentArgs.length);
    return command.suggest(source, actualArgs);
  }

  @Override
  public boolean hasPermission(final SimpleCommand.Invocation invocation) {
    final CommandSource source = invocation.source();
    final String[] args = invocation.arguments();

    if (args.length == 0) {
      return commands.values().stream().anyMatch(e -> e.hasPermission(source, args));
    }
    SubCommand command = commands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      return true;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    return command.hasPermission(source, actualArgs);
  }

  private static class Reload implements SubCommand {

    private static final Logger logger = LogManager.getLogger(Reload.class);
    private final VelocityServer server;

    private Reload(VelocityServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      try {
        if (server.reloadConfiguration()) {
          source.sendMessage(Component.translatable("velocity.command.reload-success",
              NamedTextColor.GREEN));
        } else {
          source.sendMessage(Component.translatable("velocity.command.reload-failure",
              NamedTextColor.RED));
        }
      } catch (Exception e) {
        logger.error("Unable to reload configuration", e);
        source.sendMessage(Component.translatable("velocity.command.reload-failure",
            NamedTextColor.RED));
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.reload") == Tristate.TRUE;
    }
  }

  private static class Info implements SubCommand {

    private static final TextColor VELOCITY_COLOR = TextColor.fromHexString("#09add3");
    private final ProxyServer server;

    private Info(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(Component.text("/velocity version", NamedTextColor.RED));
        return;
      }

      ProxyVersion version = server.getVersion();

      Component velocity = Component.text().content(version.getName() + " ")
          .decoration(TextDecoration.BOLD, true)
          .color(VELOCITY_COLOR)
          .append(Component.text(version.getVersion()).decoration(TextDecoration.BOLD, false))
          .build();
      Component copyright = Component
          .translatable("velocity.command.version-copyright",
              Component.text(version.getVendor()),
              Component.text(version.getName()));
      source.sendMessage(velocity);
      source.sendMessage(copyright);

      if (version.getName().equals("Velocity")) {
        TextComponent embellishment = Component.text()
            .append(Component.text().content("velocitypowered.com")
                .color(NamedTextColor.GREEN)
                .clickEvent(
                    ClickEvent.openUrl("https://velocitypowered.com"))
                .build())
            .append(Component.text(" - "))
            .append(Component.text().content("GitHub")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl(
                    "https://github.com/PaperMC/Velocity"))
                .build())
            .build();
        source.sendMessage(embellishment);
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.info") != Tristate.FALSE;
    }
  }

  private static class Plugins implements SubCommand {

    private final ProxyServer server;

    private Plugins(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(Component.text("/velocity plugins", NamedTextColor.RED));
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.getPluginManager().getPlugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(Component.translatable("velocity.command.no-plugins",
            NamedTextColor.YELLOW));
        return;
      }

      TextComponent.Builder listBuilder = Component.text();
      for (int i = 0; i < pluginCount; i++) {
        PluginContainer plugin = plugins.get(i);
        listBuilder.append(componentForPlugin(plugin.getDescription()));
        if (i + 1 < pluginCount) {
          listBuilder.append(Component.text(", "));
        }
      }

      TranslatableComponent.Builder output = Component.translatable()
          .key("velocity.command.plugins-list")
          .color(NamedTextColor.YELLOW)
          .args(listBuilder.build());
      source.sendMessage(output);
    }

    private TextComponent componentForPlugin(PluginDescription description) {
      String pluginInfo = description.getName().orElse(description.getId())
          + description.getVersion().map(v -> " " + v).orElse("");

      TextComponent.Builder hoverText = Component.text().content(pluginInfo);

      description.getUrl().ifPresent(url -> {
        hoverText.append(Component.newline());
        hoverText.append(Component.translatable(
            "velocity.command.plugin-tooltip-website",
            Component.text(url)));
      });
      if (!description.getAuthors().isEmpty()) {
        hoverText.append(Component.newline());
        if (description.getAuthors().size() == 1) {
          hoverText.append(Component.translatable("velocity.command.plugin-tooltip-author",
              Component.text(description.getAuthors().get(0))));
        } else {
          hoverText.append(
              Component.translatable("velocity.command.plugin-tooltip-author",
                  Component.text(String.join(", ", description.getAuthors()))
              )
          );
        }
      }
      description.getDescription().ifPresent(pdesc -> {
        hoverText.append(Component.newline());
        hoverText.append(Component.newline());
        hoverText.append(Component.text(pdesc));
      });

      return Component.text(description.getId(), NamedTextColor.GRAY)
          .hoverEvent(HoverEvent.showText(hoverText.build()));
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }

  private static class Dump implements SubCommand {

    private static final Logger logger = LogManager.getLogger(Dump.class);
    private final ProxyServer server;

    private Dump(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(Component.text("/velocity dump", NamedTextColor.RED));
        return;
      }

      Collection<RegisteredServer> allServers = ImmutableSet.copyOf(server.getAllServers());
      JsonObject servers = new JsonObject();
      for (RegisteredServer iter : allServers) {
        servers.add(iter.getServerInfo().getName(),
            InformationUtils.collectServerInfo(iter));
      }
      JsonArray connectOrder = new JsonArray();
      List<String> attemptedConnectionOrder = ImmutableList.copyOf(
          server.getConfiguration().getAttemptConnectionOrder());
      for (String s : attemptedConnectionOrder) {
        connectOrder.add(s);
      }

      JsonObject proxyConfig = InformationUtils.collectProxyConfig(server.getConfiguration());
      proxyConfig.add("servers", servers);
      proxyConfig.add("connectOrder", connectOrder);
      proxyConfig.add("forcedHosts",
          InformationUtils.collectForcedHosts(server.getConfiguration()));

      JsonObject dump = new JsonObject();
      dump.add("versionInfo", InformationUtils.collectProxyInfo(server.getVersion()));
      dump.add("platform", InformationUtils.collectEnvironmentInfo());
      dump.add("config", proxyConfig);
      dump.add("plugins", InformationUtils.collectPluginInfo(server));

      Path dumpPath = Path.of("velocity-dump-"
          + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())
          + ".json");
      try (BufferedWriter bw = Files.newBufferedWriter(
          dumpPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
        bw.write(InformationUtils.toHumanReadableString(dump));

        source.sendMessage(Component.text(
            "An anonymised report containing useful information about "
                + "this proxy has been saved at " + dumpPath.toAbsolutePath(),
            NamedTextColor.GREEN));
      } catch (IOException e) {
        logger.error("Failed to complete dump command, "
            + "the executor was interrupted: " + e.getMessage());
        e.printStackTrace();
        source.sendMessage(Component.text(
            "We could not save the anonymized dump. Check the console for more details.",
            NamedTextColor.RED)
        );
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }

  /**
   * Heap SubCommand.
   */
  public static class Heap implements SubCommand {
    private static final Logger logger = LogManager.getLogger(Heap.class);
    private MethodHandle heapGenerator;
    private Consumer<CommandSource> heapConsumer;
    private final Path dir = Path.of("./dumps");

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      try {
        if (Files.notExists(dir)) {
          Files.createDirectories(dir);
        }

        // A single lookup of the heap dump generator method is performed on execution
        // to avoid assigning variables unnecessarily in case the user never executes the command
        if (heapGenerator == null || heapConsumer == null) {
          javax.management.MBeanServer server = ManagementFactory.getPlatformMBeanServer();
          MethodHandles.Lookup lookup = MethodHandles.lookup();
          SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
          MethodType type;
          try {
            Class<?> clazz = Class.forName("openj9.lang.management.OpenJ9DiagnosticsMXBean");
            type = MethodType.methodType(String.class, String.class, String.class);

            this.heapGenerator = lookup.findVirtual(clazz, "triggerDumpToFile", type);
            this.heapConsumer = (src) -> {
              String name = "heap-dump-" + format.format(new Date());
              Path file = dir.resolve(name + ".phd");
              try {
                Object openj9Mbean = ManagementFactory.newPlatformMXBeanProxy(
                    server, "openj9.lang.management:type=OpenJ9Diagnostics", clazz);
                heapGenerator.invoke(openj9Mbean, "heap", file.toString());
              } catch (Throwable e) {
                // This should not occur
                throw new RuntimeException(e);
              }
              src.sendMessage(Component.text("Heap dump saved to " + file, NamedTextColor.GREEN));
            };
          } catch (ClassNotFoundException e) {
            Class<?> clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            type = MethodType.methodType(void.class, String.class, boolean.class);

            this.heapGenerator = lookup.findVirtual(clazz, "dumpHeap", type);
            this.heapConsumer = (src) -> {
              String name = "heap-dump-" + format.format(new Date());
              Path file = dir.resolve(name + ".hprof");
              try {
                Object hotspotMbean = ManagementFactory.newPlatformMXBeanProxy(
                    server, "com.sun.management:type=HotSpotDiagnostic", clazz);
                this.heapGenerator.invoke(hotspotMbean, file.toString(), true);
              } catch (Throwable e1) {
                // This should not occur
                throw new RuntimeException(e);
              }
              src.sendMessage(Component.text("Heap dump saved to " + file, NamedTextColor.GREEN));
            };
          }
        }

        this.heapConsumer.accept(source);
      } catch (Throwable t) {
        source.sendMessage(Component.text("Failed to write heap dump, see server log for details",
            NamedTextColor.RED));
        logger.error("Could not write heap", t);
      }
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.heap") == Tristate.TRUE;
    }

  }
}
