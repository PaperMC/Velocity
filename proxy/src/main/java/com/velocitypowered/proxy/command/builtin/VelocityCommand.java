package com.velocitypowered.proxy.command.builtin;

import com.google.common.base.Joiner;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
          source.sendMessage(Identity.nil(), Component.text(
              "Configuration reloaded.", NamedTextColor.GREEN));
        } else {
          source.sendMessage(Identity.nil(), Component.text(
              "Unable to reload your configuration. Check the console for more details.",
              NamedTextColor.RED));
        }
      } catch (Exception e) {
        logger.error("Unable to reload configuration", e);
        source.sendMessage(Identity.nil(), Component.text(
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

      ProxyVersion version = server.getVersion();

      TextComponent velocity = Component.text().content(version.getName() + " ")
          .decoration(TextDecoration.BOLD, true)
          .color(VELOCITY_COLOR)
          .append(Component.text(version.getVersion()).decoration(TextDecoration.BOLD, false))
          .build();
      TextComponent copyright = Component
          .text("Copyright 2018-2021 " + version.getVendor() + ". " + version.getName()
              + " is freely licensed under the terms of the MIT License.");
      source.sendMessage(Identity.nil(), velocity);
      source.sendMessage(Identity.nil(), copyright);

      if (version.getName().equals("Velocity")) {
        TextComponent velocityWebsite = Component.text()
            .content("Visit the ")
            .append(Component.text().content("Velocity website")
                .color(NamedTextColor.GREEN)
                .clickEvent(
                    ClickEvent.openUrl("https://www.velocitypowered.com"))
                .build())
            .append(Component.text(" or the "))
            .append(Component.text().content("Velocity GitHub")
                .color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl(
                    "https://github.com/VelocityPowered/Velocity"))
                .build())
            .build();
        source.sendMessage(Identity.nil(), velocityWebsite);
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
        source.sendMessage(Identity.nil(), Component.text("/velocity plugins", NamedTextColor.RED));
        return;
      }

      List<PluginContainer> plugins = ImmutableList.copyOf(server.getPluginManager().getPlugins());
      int pluginCount = plugins.size();

      if (pluginCount == 0) {
        source.sendMessage(Identity.nil(), Component.text(
            "No plugins installed.", NamedTextColor.YELLOW));
        return;
      }

      TextComponent.Builder output = Component.text().content("Plugins: ")
          .color(NamedTextColor.YELLOW);
      for (int i = 0; i < pluginCount; i++) {
        PluginContainer plugin = plugins.get(i);
        output.append(componentForPlugin(plugin.getDescription()));
        if (i + 1 < pluginCount) {
          output.append(Component.text(", "));
        }
      }

      source.sendMessage(Identity.nil(), output.build());
    }

    private TextComponent componentForPlugin(PluginDescription description) {
      String pluginInfo = description.getName().orElse(description.getId())
          + description.getVersion().map(v -> " " + v).orElse("");

      TextComponent.Builder hoverText = Component.text().content(pluginInfo);

      description.getUrl().ifPresent(url -> {
        hoverText.append(Component.newline());
        hoverText.append(Component.text("Website: " + url));
      });
      if (!description.getAuthors().isEmpty()) {
        hoverText.append(Component.newline());
        if (description.getAuthors().size() == 1) {
          hoverText.append(Component.text("Author: " + description.getAuthors().get(0)));
        } else {
          hoverText.append(Component.text("Authors: " + Joiner.on(", ")
              .join(description.getAuthors())));
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
        source.sendMessage(Identity.nil(), Component.text("/velocity dump", NamedTextColor.RED));
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
      for (int i = 0; i < attemptedConnectionOrder.size(); i++) {
        connectOrder.add(attemptedConnectionOrder.get(i));
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

      source.sendMessage(Component.text().content("Uploading gathered information...").build());
      AsyncHttpClient httpClient = ((VelocityServer) server).getAsyncHttpClient();

      BoundRequestBuilder request =
              httpClient.preparePost("https://dump.velocitypowered.com/documents");
      request.setHeader("Content-Type", "text/plain");
      request.addHeader("User-Agent", server.getVersion().getName() + "/"
              + server.getVersion().getVersion());
      request.setBody(
              InformationUtils.toHumanReadableString(dump).getBytes(StandardCharsets.UTF_8));

      ListenableFuture<Response> future = request.execute();
      future.addListener(() -> {
        try {
          Response response = future.get();
          if (response.getStatusCode() != 200) {
            source.sendMessage(Component.text()
                    .content("An error occurred while communicating with the Velocity servers. "
                            + "The servers may be temporarily unavailable or there is an issue "
                            + "with your network settings. You can find more information in the "
                            + "log or console of your Velocity server.")
                    .color(NamedTextColor.RED).build());
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
          source.sendMessage(Component.text()
                  .content("Created an anonymised report containing useful information about "
                          + "this proxy. If a developer requested it, you may share the "
                          + "following link with them:")
                  .append(Component.newline())
                  .append(Component.text(">> " + url)
                          .color(NamedTextColor.GREEN)
                          .clickEvent(ClickEvent.openUrl(url)))
                 .append(Component.newline())
                 .append(Component.text("Note: This link is only valid for a few days")
                          .color(NamedTextColor.GRAY)
                 ).build());
        } catch (InterruptedException e) {
          source.sendMessage(Component.text()
                  .content("Could not complete the request, the command was interrupted."
                          + "Please refer to the proxy-log or console for more information.")
                  .color(NamedTextColor.RED).build());
          logger.error("Failed to complete dump command, "
                  + "the executor was interrupted: " + e.getMessage());
          e.printStackTrace();
        } catch (ExecutionException e) {
          TextComponent.Builder message = Component.text()
                  .content("An error occurred while attempting to upload the gathered "
                          + "information to the Velocity servers.")
                  .append(Component.newline())
                  .color(NamedTextColor.RED);
          if (e.getCause() instanceof UnknownHostException
              || e.getCause() instanceof ConnectException) {
            message.append(Component.text(
                    "Likely cause: Invalid system DNS settings or no internet connection"));
          }
          source.sendMessage(message
                  .append(Component.newline()
                  .append(Component.text(
                          "Error details can be found in the proxy log / console"))
                  ).build());

          logger.error("Failed to complete dump command, "
                  + "the executor encountered an Exception: " + e.getCause().getMessage());
          e.getCause().printStackTrace();
        } catch (JsonParseException e) {
          source.sendMessage(Component.text()
                  .content("An error occurred on the Velocity-servers and the dump could not "
                          + "be completed. Please contact the Velocity staff about this problem. "
                          + "If you do, provide the details about this error from the Velocity "
                          + "console or server log.")
                  .color(NamedTextColor.RED).build());
          logger.error("Invalid response from the Velocity servers: " + e.getMessage());
          e.printStackTrace();
        }
      }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
      return source.getPermissionValue("velocity.command.plugins") == Tristate.TRUE;
    }
  }
}
