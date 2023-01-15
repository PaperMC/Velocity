/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.network.TransportType;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * Helper class for {@code /velocity dump}.
 */
public enum InformationUtils {
  ;

  /**
   * Retrieves a {@link JsonArray} containing basic information about all running plugins on the
   * {@link ProxyServer} instance.
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
        JsonArray authorsArray = new JsonArray();
        for (String author : desc.getAuthors()) {
          authorsArray.add(author);
        }
        current.add("authors", authorsArray);
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
   * Creates a {@link JsonObject} containing information about the current environment the project
   * is run under.
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

    JsonObject listenerInfo = new JsonObject();
    listenerInfo.addProperty("listenerType", TransportType.bestType().toString());
    listenerInfo.addProperty("compression", Natives.compress.getLoadedVariant());
    listenerInfo.addProperty("encryption", Natives.cipher.getLoadedVariant());

    envInfo.add("listener", listenerInfo);

    return envInfo;
  }

  /**
   * Creates a {@link JsonObject} containing information about the forced hosts of the
   * {@link ProxyConfig} instance.
   *
   * @return {@link JsonArray} containing forced hosts
   */
  public static JsonObject collectForcedHosts(ProxyConfig config) {
    JsonObject forcedHosts = new JsonObject();
    Map<String, List<String>> allForcedHosts = ImmutableMap.copyOf(
        config.getForcedHosts());
    for (Map.Entry<String, List<String>> entry : allForcedHosts.entrySet()) {
      JsonArray host = new JsonArray();
      for (int i = 0; i < entry.getValue().size(); i++) {
        host.add(entry.getValue().get(i));
      }
      forcedHosts.add(entry.getKey(), host);
    }
    return forcedHosts;
  }

  /**
   * Anonymises or redacts a given {@link InetAddress} public address bits.
   *
   * @param address The address to redact
   * @return {@link String} address with public parts redacted
   */
  public static String anonymizeInetAddress(InetAddress address) {
    if (address instanceof Inet4Address) {
      Inet4Address v4 = (Inet4Address) address;
      if (v4.isAnyLocalAddress() || v4.isLoopbackAddress()
          || v4.isLinkLocalAddress()
          || v4.isSiteLocalAddress()) {
        return address.getHostAddress();
      } else {
        byte[] addr = v4.getAddress();
        return (addr[0] & 0xff) + "." + (addr[1] & 0xff) + ".XXX.XXX";
      }
    } else if (address instanceof Inet6Address) {
      Inet6Address v6 = (Inet6Address) address;
      if (v6.isAnyLocalAddress() || v6.isLoopbackAddress()
          || v6.isSiteLocalAddress()
          || v6.isSiteLocalAddress()) {
        return address.getHostAddress();
      } else {
        String[] bits = v6.getHostAddress().split(":");
        String ret = "";
        boolean flag = false;
        for (int iter = 0; iter < bits.length; iter++) {
          if (flag) {
            ret += ":X";
            continue;
          }
          if (!bits[iter].equals("0")) {
            if (iter == 0) {
              ret = bits[iter];
            } else {
              ret = "::" + bits[iter];
            }
            flag = true;
          }
        }
        return ret;
      }
    } else {
      return address.getHostAddress();
    }
  }

  /**
   * Creates a {@link JsonObject} containing most relevant information of the
   * {@link RegisteredServer} for diagnosis.
   *
   * @param server the server to evaluate
   * @return {@link JsonObject} containing server and diagnostic info
   */
  public static JsonObject collectServerInfo(RegisteredServer server) {
    JsonObject info = new JsonObject();
    info.addProperty("currentPlayers", server.getPlayersConnected().size());
    InetSocketAddress iaddr = server.getServerInfo().getAddress();
    if (iaddr.isUnresolved()) {
      // Greetings form Netty 4aa10db9
      info.addProperty("host", iaddr.getHostString());
    } else {
      info.addProperty("host", anonymizeInetAddress(iaddr.getAddress()));
    }
    info.addProperty("port", iaddr.getPort());
    return info;
  }

  /**
   * Creates a {@link JsonObject} containing information about the current environment the project
   * is run under.
   *
   * @param version the proxy instance to retrieve from
   * @return {@link JsonObject} containing environment info
   */
  public static JsonObject collectProxyInfo(ProxyVersion version) {
    return (JsonObject) serializeObject(version, false);
  }

  /**
   * Creates a {@link JsonObject} containing most relevant information of the {@link ProxyConfig}
   * for diagnosis.
   *
   * @param config the config instance to retrieve from
   * @return {@link JsonObject} containing select config values
   */
  public static JsonObject collectProxyConfig(ProxyConfig config) {
    return (JsonObject) serializeObject(config, true);
  }

  /**
   * Creates a human-readable String from a {@link JsonElement}.
   *
   * @param json the {@link JsonElement} object
   * @return the human-readable String
   */
  public static String toHumanReadableString(JsonElement json) {
    return GSON_WITHOUT_EXCLUDES.toJson(json);
  }

  /**
   * Creates a {@link JsonObject} from a String.
   *
   * @param toParse the String to parse
   * @return {@link JsonObject} object
   */
  public static JsonObject parseString(String toParse) {
    return GSON_WITHOUT_EXCLUDES.fromJson(toParse, JsonObject.class);
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
