package com.velocitypowered.proxy.command.builtin;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

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
        .build();
  }

  private void usage(CommandSource source) {
    String availableCommands = commands.entrySet().stream()
        .filter(e -> e.getValue().hasPermission(source, new String[0]))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining("|"));
    String commandText = "/velocity <" + availableCommands + ">";
    source.sendMessage(TextComponent.of(commandText, NamedTextColor.RED));
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
          source.sendMessage(TextComponent.of("Configuration reloaded.", NamedTextColor.GREEN));
        } else {
          source.sendMessage(TextComponent.of(
              "Unable to reload your configuration. Check the console for more details.",
              NamedTextColor.RED));
        }
      } catch (Exception e) {
        logger.error("Unable to reload configuration", e);
        source.sendMessage(TextComponent.of(
            "Unable to reload your configuration. Check the console for more details.",
            NamedTextColor.RED));
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.reload") == Tristate.TRUE;
    }
  }

  private static class Info implements SubCommand {

    private final ProxyServer server;

    private Info(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(TextComponent.of("/velocity version", NamedTextColor.RED));
        return;
      }

      ProxyVersion version = server.getVersion();

      TextComponent velocity = TextComponent.builder(version.getName() + " ")
          .decoration(TextDecoration.BOLD, true)
          .color(NamedTextColor.DARK_AQUA)
          .append(TextComponent.of(version.getVersion()).decoration(TextDecoration.BOLD, false))
          .build();
      TextComponent copyright = TextComponent
          .of("Copyright 2018-2020 " + version.getVendor() + ". " + version.getName()
              + " is freely licensed under the terms of the MIT License.");
      source.sendMessage(velocity);
      source.sendMessage(copyright);

      if (version.getName().equals("Velocity")) {
        TextComponent velocityWebsite = TextComponent.builder()
            .content("Visit the ")
            .append(TextComponent.builder("Velocity website")
                .color(NamedTextColor.GREEN)
                .clickEvent(
                    ClickEvent.openUrl("https://www.velocitypowered.com"))
                .build())
            .append(TextComponent.of(" or the "))
            .append(TextComponent.builder("Velocity GitHub")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl(
                    "https://github.com/VelocityPowered/Velocity"))
                .build())
            .build();
        source.sendMessage(velocityWebsite);
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
        source.sendMessage(TextComponent.of("/velocity plugins", NamedTextColor.RED));
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.getPluginManager().getPlugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(TextComponent.of("No plugins installed.", NamedTextColor.YELLOW));
        return;
      }

      TextComponent.Builder output = TextComponent.builder("Plugins: ")
          .color(NamedTextColor.YELLOW);
      for (int i = 0; i < pluginCount; i++) {
        PluginContainer plugin = plugins.get(i);
        output.append(componentForPlugin(plugin.getDescription()));
        if (i + 1 < pluginCount) {
          output.append(TextComponent.of(", "));
        }
      }

      source.sendMessage(output.build());
    }

    private TextComponent componentForPlugin(PluginDescription description) {
      String pluginInfo = description.getName().orElse(description.getId())
          + description.getVersion().map(v -> " " + v).orElse("");

      TextComponent.Builder hoverText = TextComponent.builder(pluginInfo);

      description.getUrl().ifPresent(url -> {
        hoverText.append(TextComponent.newline());
        hoverText.append(TextComponent.of("Website: " + url));
      });
      if (!description.getAuthors().isEmpty()) {
        hoverText.append(TextComponent.newline());
        if (description.getAuthors().size() == 1) {
          hoverText.append(TextComponent.of("Author: " + description.getAuthors().get(0)));
        } else {
          hoverText.append(TextComponent.of("Authors: " + Joiner.on(", ")
              .join(description.getAuthors())));
        }
      }
      description.getDescription().ifPresent(pdesc -> {
        hoverText.append(TextComponent.newline());
        hoverText.append(TextComponent.newline());
        hoverText.append(TextComponent.of(pdesc));
      });

      return TextComponent.of(description.getId(), NamedTextColor.GRAY)
          .hoverEvent(HoverEvent.showText(hoverText.build()));
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }
}
