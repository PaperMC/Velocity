/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
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
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.identity.Identity;
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
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
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
        .put("dump", new Dump(server))
        .build();
  }

  private void usage(CommandSource source) {
    String availableCommands = commands.entrySet().stream()
        .filter(e -> e.getValue().hasPermission(source, new String[0]))
        .map(Map.Entry::getKey)
        .collect(Collectors.joining("|"));
    String commandText = "/velocity <" + availableCommands + ">";
    source.sendMessage(Identity.nil(), Component.text(commandText, NamedTextColor.RED));
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
      return source.evaluatePermission("velocity.command.reload") == Tristate.TRUE;
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
        source.sendMessage(Identity.nil(), Component.text("/velocity version", NamedTextColor.RED));
        return;
      }

      ProxyVersion version = server.version();

      Component velocity = Component.text().content(version.name() + " ")
          .decoration(TextDecoration.BOLD, true)
          .color(VELOCITY_COLOR)
          .append(Component.text(version.version()).decoration(TextDecoration.BOLD, false))
          .build();
      Component copyright = Component
          .translatable("velocity.command.version-copyright",
              Component.text(version.vendor()),
              Component.text(version.name()));
      source.sendMessage(velocity);
      source.sendMessage(copyright);

      if (version.name().equals("Velocity")) {
        TextComponent embellishment = Component.text()
            .append(Component.text().content("velocitypowered.com")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(
                    ClickEvent.openUrl("https://www.velocitypowered.com"))
                .build())
            .append(Component.text(" - "))
            .append(Component.text().content("GitHub")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl(
                    "https://github.com/VelocityPowered/Velocity"))
                .build())
            .build();
        source.sendMessage(Identity.nil(), embellishment);
      }
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.evaluatePermission("velocity.command.info") != Tristate.FALSE;
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
        source.sendMessage(Identity.nil(), Component.text("/velocity plugins", NamedTextColor.RED));
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.pluginManager().plugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(Component.translatable("velocity.command.no-plugins",
            NamedTextColor.YELLOW));
        return;
      }

      TranslatableComponent.Builder output = Component.translatable()
          .key("velocity.command.plugins-list")
          .color(NamedTextColor.YELLOW);
      for (int i = 0; i < pluginCount; i++) {
        PluginContainer plugin = plugins.get(i);
        output.append(componentForPlugin(plugin.description()));
        if (i + 1 < pluginCount) {
          output.append(Component.text(", "));
        }
      }

      source.sendMessage(Identity.nil(), output.build());
    }

    private TextComponent componentForPlugin(PluginDescription description) {
      String pluginInfo = description.name();

      TextComponent.Builder hoverText = Component.text().content(pluginInfo);

      String pluginUrl = description.url();
      if (pluginUrl != null) {
        hoverText.append(Component.newline());
        hoverText.append(Component.translatable(
            "velocity.command.plugin-tooltip-website",
            Component.text(pluginUrl)));
      }
      if (!description.authors().isEmpty()) {
        hoverText.append(Component.newline());
        if (description.authors().size() == 1) {
          hoverText.append(Component.translatable("velocity.command.plugin-tooltip-author",
              Component.text(description.authors().get(0))));
        } else {
          hoverText.append(
              Component.translatable("velocity.command.plugin-tooltip-authors",
                Component.text(String.join(", ", description.authors()))
              )
          );
        }
      }

      String humanDescription = description.description();
      if (humanDescription != null) {
        hoverText.append(Component.newline());
        hoverText.append(Component.newline());
        hoverText.append(Component.text(humanDescription));
      }

      return Component.text(description.id(), NamedTextColor.GRAY)
          .hoverEvent(HoverEvent.showText(hoverText.build()));
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.evaluatePermission("velocity.command.plugins") == Tristate.TRUE;
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
        source.sendMessage(Identity.nil(), Component.text("/velocity dump", NamedTextColor.RED));
        return;
      }

      Collection<RegisteredServer> allServers = ImmutableSet.copyOf(server.registeredServers());
      JsonObject servers = new JsonObject();
      for (RegisteredServer iter : allServers) {
        servers.add(iter.serverInfo().name(),
                InformationUtils.collectServerInfo(iter));
      }
      JsonArray connectOrder = new JsonArray();
      List<String> attemptedConnectionOrder = ImmutableList.copyOf(
              server.configuration().getAttemptConnectionOrder());
      for (String s : attemptedConnectionOrder) {
        connectOrder.add(s);
      }

      JsonObject proxyConfig = InformationUtils.collectProxyConfig(server.configuration());
      proxyConfig.add("servers", servers);
      proxyConfig.add("connectOrder", connectOrder);
      proxyConfig.add("forcedHosts",
              InformationUtils.collectForcedHosts(server.configuration()));

      JsonObject dump = new JsonObject();
      dump.add("versionInfo", InformationUtils.collectProxyInfo(server.version()));
      dump.add("platform", InformationUtils.collectEnvironmentInfo());
      dump.add("config", proxyConfig);
      dump.add("plugins", InformationUtils.collectPluginInfo(server));

      source.sendMessage(Component.translatable("velocity.command.dump-uploading"));
      AsyncHttpClient httpClient = ((VelocityServer) server).getAsyncHttpClient();

      BoundRequestBuilder request =
              httpClient.preparePost("https://dump.velocitypowered.com/documents");
      request.setHeader("Content-Type", "text/plain");
      request.addHeader("User-Agent", server.version().name() + "/"
              + server.version().version());
      request.setBody(
              InformationUtils.toHumanReadableString(dump).getBytes(StandardCharsets.UTF_8));

      ListenableFuture<Response> future = request.execute();
      future.addListener(() -> {
        try {
          Response response = future.get();
          if (response.getStatusCode() != 200) {
            source.sendMessage(Component.translatable("velocity.command.dump-send-error",
                NamedTextColor.RED));
            logger.error("Invalid status code while POST-ing Velocity dump: "
                    + response.getStatusCode());
            logger.error("Headers: \n--------------BEGIN HEADERS--------------\n"
                    + response.getHeaders().toString()
                    + "\n---------------END HEADERS---------------");
            return;
          }
          JsonObject key = InformationUtils.parseString(
                  response.getResponseBody(StandardCharsets.UTF_8));
          if (!key.has("key")) {
            throw new JsonSyntaxException("Missing Dump-Url-response");
          }
          String url = "https://dump.velocitypowered.com/"
                  + key.get("key").getAsString() + ".json";
          source.sendMessage(Component.translatable("velocity.command.dump-success")
                  .append(Component.newline())
                  .append(Component.text(">> " + url)
                          .color(NamedTextColor.GREEN)
                          .clickEvent(ClickEvent.openUrl(url)))
                 .append(Component.newline())
                 .append(Component.translatable("velocity.command.dump-will-expire",
                     NamedTextColor.GRAY)));
        } catch (JsonParseException e) {
          source.sendMessage(Component.translatable("velocity.command.dump-server-error"));
          logger.error("Invalid response from the Velocity servers: " + e.getMessage());
          e.printStackTrace();
        } catch (Exception e) {
          Component message = Component.translatable("velocity.command.dump-send-error")
              .append(Component.newline())
              .color(NamedTextColor.RED);
          if (e.getCause() instanceof UnknownHostException
              || e.getCause() instanceof ConnectException) {
            message = message.append(Component.translatable("velocity.command.dump-offline"));
          }
          source.sendMessage(message);
          logger.error("Failed to complete dump command", Throwables.getRootCause(e));
        }
      }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.evaluatePermission("velocity.command.plugins") == Tristate.TRUE;
    }
  }
}
