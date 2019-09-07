package com.velocitypowered.proxy.command;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class BuiltinCommandUtil {

  private BuiltinCommandUtil() {
    throw new AssertionError();
  }

  static List<RegisteredServer> sortedServerList(ProxyServer proxy) {
    List<RegisteredServer> servers = new ArrayList<>(proxy.getAllServers());
    servers.sort(Comparator.comparing(RegisteredServer::getServerInfo));
    return Collections.unmodifiableList(servers);
  }
}
