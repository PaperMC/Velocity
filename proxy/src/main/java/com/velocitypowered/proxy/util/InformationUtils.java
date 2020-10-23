package com.velocitypowered.proxy.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ProxyVersion;

import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import joptsimple.internal.Strings;

public enum InformationUtils {
  ;

  /**
   * Retrieves a {@link JsonArray} containing basic information about all
   * running plugins on the {@link ProxyServer} instance.
   *
   * @param proxy the proxy instance to retrieve from
   * @return {@link JsonArray} containing zero or more {@link JsonObject}
   */
  public static JsonArray collectPluginInfo(ProxyServer proxy) {
    List<PluginContainer> allPlugins = ImmutableList.copyOf(
            proxy.getPluginManager().getPlugins());
    JsonArray plugins = new JsonArray();

    for (PluginContainer plugin : allPlugins) {
      PluginDescription desc = plugin.getDescription();
      JsonObject current = new JsonObject();
      current.addProperty("id", desc.getId());
      if (desc.getName().isPresent()) {
        current.addProperty("name", desc.getName().get());
      }
      if (desc.getVersion().isPresent()) {
        current.addProperty("version", desc.getVersion().get());
      }
      if (!desc.getAuthors().isEmpty()) {
        current.addProperty("authors",
                Strings.join(desc.getAuthors(), ","));
      }
      if (desc.getDescription().isPresent()) {
        current.addProperty("description", desc.getDescription().get());
      }
      if (desc.getUrl().isPresent()) {
        current.addProperty("url", desc.getUrl().get());
      }
      if (!desc.getDependencies().isEmpty()) {
        JsonArray dependencies = new JsonArray();
        for (PluginDependency dependency : desc.getDependencies()) {
          dependencies.add(dependency.getId());
        }
        current.add("dependencies", dependencies);
      }
      plugins.add(current);
    }
    return plugins;
  }

  /**
   * Creates a {@link JsonObject} containing information about the
   * current environment the project is run under.
   *
   * @return {@link JsonObject} containing environment info
   */
  public static JsonObject collectEnvironmentInfo() {
    JsonObject envInfo = new JsonObject();
    envInfo.addProperty("operatingSystemType", System.getProperty("os.name"));
    envInfo.addProperty("operatingSystemVersion", System.getProperty("os.version"));
    envInfo.addProperty("operatingSystemArchitecture", System.getProperty("os.arch"));
    envInfo.addProperty("javaVersion", System.getProperty("java.version"));
    envInfo.addProperty("javaVendor", System.getProperty("java.vendor"));
    return envInfo;
  }

  /**
   * Creates a {@link JsonObject} containing most relevant
   * information of the {@link RegisteredServer} for diagnosis.
   *
   * @param server the server to evaluate
   * @return {@link JsonObject} containing server and diagnostic info
   */
  public static JsonObject collectServerInfo(RegisteredServer server) {
    JsonObject info = new JsonObject();
    info.addProperty("currentPlayers", server.getPlayersConnected().size());
    SocketAddress address = server.getServerInfo().getAddress();
    if (address instanceof InetSocketAddress) {
      InetSocketAddress iaddr = (InetSocketAddress) address;
      info.addProperty("socketType", "EventLoop");
      info.addProperty("unresolved", iaddr.isUnresolved());
      // Greetings form Netty 4aa10db9
      info.addProperty("host", iaddr.getHostString());
      info.addProperty("port", iaddr.getPort());
    } else if (address instanceof DomainSocketAddress) {
      DomainSocketAddress daddr = (DomainSocketAddress) address;
      info.addProperty("socketType", "Unix/Epoll");
      info.addProperty("host", daddr.path());
    } else {
      info.addProperty("socketType", "Unknown/Generic");
      info.addProperty("host", address.toString());
    }
    return info;
  }

  /**
   * Creates a {@link JsonObject} containing information about the
   * current environment the project is run under.
   *
   * @param version the proxy instance to retrieve from
   * @return {@link JsonObject} containing environment info
   */
  public static JsonObject collectProxyInfo(ProxyVersion version) {
    return (JsonObject) serializeObject(version, false);
  }

  /**
   * Creates a {@link JsonObject} containing most relevant
   * information of the {@link ProxyConfig} for diagnosis.
   *
   * @param config the config instance to retrieve from
   * @return {@link JsonObject} containing select config values
   */
  public static JsonObject collectProxyConfig(ProxyConfig config) {
    return (JsonObject) serializeObject(config, true);
  }

  private static JsonElement serializeObject(Object toSerialize, boolean withExcludes) {
    return JsonParser.parseString(
            withExcludes ? GSON_WITH_EXCLUDES.toJson(toSerialize) :
            GSON_WITHOUT_EXCLUDES.toJson(toSerialize));
  }

  private static final Gson GSON_WITH_EXCLUDES = new GsonBuilder()
          .setPrettyPrinting()
          .excludeFieldsWithoutExposeAnnotation()
          .create();

  private static final Gson GSON_WITHOUT_EXCLUDES = new GsonBuilder()
          .setPrettyPrinting()
          .create();


}
