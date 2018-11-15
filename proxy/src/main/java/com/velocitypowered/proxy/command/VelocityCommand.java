package com.velocitypowered.proxy.command;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.text.Components;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.event.HoverEvent.Action;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityCommand implements Command {

  private final Map<String, Command> subcommands;

  public VelocityCommand(ProxyServer server) {
    this.subcommands = ImmutableMap.<String, Command>builder()
        .put("version", new Info(server))
        .put("plugins", new Plugins(server))
        .build();
  }

  private void usage(CommandSource source) {
    String availableCommands = subcommands.entrySet().stream()
        .filter(e -> e.getValue().hasPermission(source, new String[0]))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining("|"));
    String commandText = "/velocity <" + availableCommands + ">";
    source.sendMessage(TextComponent.of(commandText, TextColor.RED));
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (args.length == 0) {
      usage(source);
      return;
    }

    Command command = subcommands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      usage(source);
      return;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    command.execute(source, actualArgs);
  }

  @Override
  public List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
    if (currentArgs.length == 0) {
      return ImmutableList.copyOf(subcommands.keySet());
    }

    if (currentArgs.length == 1) {
      return subcommands.entrySet().stream()
          .filter(e -> e.getKey().regionMatches(true, 0, currentArgs[0], 0,
              currentArgs[0].length()))
          .filter(e -> e.getValue().hasPermission(source, new String[0]))
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());
    }

    Command command = subcommands.get(currentArgs[0].toLowerCase(Locale.US));
    if (command == null) {
      return ImmutableList.of();
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(currentArgs, 1, currentArgs.length);
    return command.suggest(source, actualArgs);
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    if (args.length == 0) {
      return true;
    }
    Command command = subcommands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      return true;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    return command.hasPermission(source, actualArgs);
  }

  private static class Info implements Command {

    private final ProxyServer server;

    private Info(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      ProxyVersion version = server.getVersion();

      TextComponent velocity = TextComponent.builder(version.getName() + " ")
          .decoration(TextDecoration.BOLD, true)
          .color(TextColor.DARK_AQUA)
          .append(TextComponent.of(version.getVersion()).decoration(TextDecoration.BOLD, false))
          .build();
      TextComponent copyright = TextComponent
          .of("Copyright 2018 " + version.getVendor() + ". " + version.getName()
              + " is freely licensed under the terms of the MIT License.");
      source.sendMessage(velocity);
      source.sendMessage(copyright);

      if (version.getName().equals("Velocity")) {
        TextComponent velocityWebsite = TextComponent.builder()
            .content("Visit the ")
            .append(TextComponent.builder("Velocity website")
                .color(TextColor.GREEN)
                .clickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.velocitypowered.com"))
                .build())
            .append(TextComponent.of(" or the ").resetStyle())
            .append(TextComponent.builder("Velocity GitHub")
                .color(TextColor.GREEN)
                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://github.com/VelocityPowered/Velocity"))
                .build())
            .build();
        source.sendMessage(velocityWebsite);
      }
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.info") != Tristate.FALSE;
    }
  }

  private static class Plugins implements Command {

    private final ProxyServer server;

    private Plugins(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(TextComponent.of("/velocity plugins", TextColor.RED));
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.getPluginManager().getPlugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(TextComponent.of("No plugins installed.", TextColor.YELLOW));
        return;
      }

      TextComponent.Builder output = TextComponent.builder("Plugins: ")
          .color(TextColor.YELLOW);
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
      TextComponent pluginSelf = TextComponent.of(description.getId(), TextColor.GRAY);
      String pluginInfo = description.getName().orElse(description.getId())
          + description.getVersion().map(v -> " " + v).orElse("");

      TextComponent.Builder hoverText = TextComponent.builder(pluginInfo);

      description.getUrl().ifPresent(url -> {
        hoverText.append(Components.newline());
        hoverText.append(TextComponent.of("Website: " + url));
      });
      if (!description.getAuthors().isEmpty()) {
        hoverText.append(Components.newline());
        if (description.getAuthors().size() == 1) {
          hoverText.append(TextComponent.of("Author: " + description.getAuthors().get(0)));
        } else {
          hoverText.append(TextComponent.of("Authors: " + Joiner.on(", ")
              .join(description.getAuthors())));
        }
      }
      description.getDescription().ifPresent(pdesc -> {
        hoverText.append(Components.newline());
        hoverText.append(Components.newline());
        hoverText.append(TextComponent.of(pdesc));
      });

      return pluginSelf.hoverEvent(new HoverEvent(Action.SHOW_TEXT, hoverText.build()));
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }
}
