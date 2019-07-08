package com.velocitypowered.proxy.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.text.TextJoiner;
import com.velocitypowered.proxy.text.translation.Translatable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityCommand implements Command {

  private static final Translatable USAGE = Translatable
      .of("velocity.command.usage");

  private final Map<String, Command> subcommands;

  /**
   * Initializes the command object for /velocity.
   * @param server the Velocity server
   */
  public VelocityCommand(VelocityServer server) {
    this.subcommands = ImmutableMap.<String, Command>builder()
        .put("version", new Info(server))
        .put("plugins", new Plugins(server))
        .put("reload", new Reload(server))
        .build();
  }

  private void usage(CommandSource source) {
    String availableCommands = subcommands.entrySet().stream()
        .filter(e -> e.getValue().hasPermission(source, new String[0]))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining("|"));
    String commandText = "/velocity <" + availableCommands + ">";
    source.sendMessage(USAGE.with(commandText));
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
      return subcommands.values().stream().anyMatch(e -> e.hasPermission(source, args));
    }
    Command command = subcommands.get(args[0].toLowerCase(Locale.US));
    if (command == null) {
      return true;
    }
    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(args, 1, args.length);
    return command.hasPermission(source, actualArgs);
  }

  private static class Reload implements Command {

    private static final Component SUCCESS = TranslatableComponent
        .of("velocity.command.reload.success");
    private static final Component FAILURE = TranslatableComponent
        .of("velocity.command.reload.failure");

    private static final Logger logger = LogManager.getLogger(Reload.class);
    private final VelocityServer server;

    private Reload(VelocityServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      try {
        if (server.reloadConfiguration()) {
          source.sendMessage(SUCCESS);
        } else {
          source.sendMessage(FAILURE);
        }
      } catch (Exception e) {
        logger.error("Unable to reload configuration", e);
        source.sendMessage(FAILURE);
      }
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.reload") == Tristate.TRUE;
    }
  }

  private static class Info implements Command {

    private static final Component USAGE = TranslatableComponent
        .of("velocity.command.info.usage");
    private static final Translatable VELOCITY = Translatable
        .of("velocity.command.info.velocity");
    private static final Translatable COPYRIGHT = Translatable
        .of("velocity.command.info.copyright");
    private static final Component WEBSITE = TranslatableComponent
        .of("velocity.command.info.website");

    private final ProxyServer server;

    private Info(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(USAGE);
        return;
      }

      ProxyVersion version = server.getVersion();

      source.sendMessage(VELOCITY.with(version.getName(), version.getVersion()));
      source.sendMessage(COPYRIGHT.with(version.getVendor(), version.getName()));

      if (version.getName().equals("Velocity")) {
        source.sendMessage(WEBSITE);
      }
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.info") != Tristate.FALSE;
    }
  }

  private static class Plugins implements Command {

    private static final Component NO_PLUGINS_INSTALLED = TranslatableComponent
        .of("velocity.command.plugins.no-plugins-installed");
    private static final Component USAGE = TranslatableComponent
        .of("velocity.command.plugins.usage");
    private static final Translatable PLUGIN_LIST = Translatable
        .of("velocity.command.plugins.plugin-list");
    private static final Translatable PLUGIN = Translatable
        .of("velocity.command.plugins.plugin-list.entry");
    private static final TextJoiner PLUGIN_JOINER = TextJoiner.on(TranslatableComponent
        .of("velocity.command.plugins.plugin-list.entry.separator"));
    private static final Translatable PLUGIN_URL = Translatable
        .of("velocity.command.plugins.plugin-list.entry.url");
    private static final Translatable PLUGIN_AUTHOR = Translatable
        .of("velocity.command.plugins.plugin-list.entry.author");
    private static final Translatable PLUGIN_AUTHORS = Translatable
        .of("velocity.command.plugins.plugin-list.entry.authors");
    private static final TextJoiner PLUGIN_AUTHOR_JOINER = TextJoiner
        .on("velocity.command.plugins.plugin-list.entry.authors.separator");
    private static final Translatable PLUGIN_DESCRIPTION = Translatable
        .of("velocity.command.plugins.plugin-list.entry.description");

    private final ProxyServer server;

    private Plugins(ProxyServer server) {
      this.server = server;
    }

    @Override
    public void execute(CommandSource source, String @NonNull [] args) {
      if (args.length != 0) {
        source.sendMessage(USAGE);
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.getPluginManager().getPlugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(NO_PLUGINS_INSTALLED);
        return;
      }

      Component pluginsComponent = PLUGIN_JOINER.join(plugins.stream()
          .map(plugin -> componentForPlugin(plugin.getDescription())));
      Component outputComponent = PLUGIN_LIST.with(pluginsComponent);

      source.sendMessage(outputComponent);
    }

    private Component componentForPlugin(PluginDescription description) {
      String id = description.getId();
      String name = description.getName().orElse(id);
      String version = description.getVersion().orElse("");

      Component urlComponent = description.getUrl()
          .<Component>map(PLUGIN_URL::with)
          .orElse(TextComponent.empty());
      Component authorsComponent = TextComponent.empty();
      Component descriptionComponent = description.getDescription()
          .<Component>map(PLUGIN_DESCRIPTION::with)
          .orElse(TextComponent.empty());

      List<String> authors = description.getAuthors();
      if (!authors.isEmpty()) {
        if (authors.size() == 1) {
          authorsComponent = PLUGIN_AUTHOR.with(authors.get(0));
        } else {
          authorsComponent = PLUGIN_AUTHORS.with(PLUGIN_AUTHOR_JOINER.join(authors));
        }
      }

      return PLUGIN.with(id, name, version, urlComponent, authorsComponent, descriptionComponent);
    }

    @Override
    public boolean hasPermission(CommandSource source, String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }
}
