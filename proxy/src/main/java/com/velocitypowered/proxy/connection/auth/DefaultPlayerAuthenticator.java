package com.velocitypowered.proxy.connection.auth;

import com.google.gson.JsonParseException;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.AuthAttemptEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.generateServerId;

public class DefaultPlayerAuthenticator {

  private static final Logger logger = LogManager.getLogger(DefaultPlayerAuthenticator.class);

  private static final String MOJANG_HASJOINED_URL = System.getProperty(
      "mojang.sessionserver",
      "https://sessionserver.mojang.com/session/minecraft/hasJoined"
  ).concat("?username=%s&serverId=%s");

  private final VelocityServer server;
  private final HttpClient httpClient;

  public DefaultPlayerAuthenticator(VelocityServer server) {
    this.server = server;
    this.httpClient = server.createHttpClient();
  }

  @Subscribe(priority = 1) // low priority to allow plugins to override this
  public void onAuthAttempt(AuthAttemptEvent event, Continuation continuation) {
    if (event.getResult() != null || !event.isOnlineMode() || event.getSharedSecret() == null) {
      continuation.resume();
      return;
    }

    String username = event.getAttemptedUsername();
    String playerIp = event.getConnection().getRemoteAddress().getHostString();

    HttpRequest httpRequest = buildHttpRequest(event.getSharedSecret(),
        server.getServerKeyPair().getPublic(), username, playerIp);

    httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .whenCompleteAsync((res, err) -> {
          AuthAttemptEvent.AuthResult result = processHttpResponse(res, err, username, playerIp);
          event.setResult(result);
          continuation.resume();
        });
  }

  @Subscribe
  public void onShutdown(ProxyShutdownEvent event) throws Exception {
    if (httpClient instanceof AutoCloseable c) {
      c.close();
    }
  }

  private HttpRequest buildHttpRequest(byte[] sharedSecret, PublicKey serverKey, String username,
                                       String playerIp) {
    String serverId = generateServerId(sharedSecret, serverKey);
    String url = String.format(MOJANG_HASJOINED_URL,
        urlFormParameterEscaper().escape(username), serverId);

    if (server.getConfiguration().shouldPreventClientProxyConnections()) {
      url += "&ip=" + urlFormParameterEscaper().escape(playerIp);
    }

    String agent = server.getVersion().getName() + "/" + server.getVersion().getVersion();
    return HttpRequest.newBuilder()
        .setHeader("User-Agent", agent)
        .uri(URI.create(url))
        .build();
  }

  private AuthAttemptEvent.AuthResult processHttpResponse(HttpResponse<String> res, Throwable err,
                                                          String username, String playerIp) {
    if (err != null) {
      logger.error("Failed to authenticate player", err);
      return new AuthAttemptEvent.FailureResult(
          Component.translatable("multiplayer.disconnect.authservers_down"));
    }

    if (res.statusCode() == 200) {
      GameProfile profile;
      try {
        profile = GENERAL_GSON.fromJson(res.body(), GameProfile.class);
      } catch (JsonParseException e) {
        logger.warn("Failed to parse Mojang session server response for {} ({})",
            username, playerIp, e);
        return new AuthAttemptEvent.FailureResult(
            Component.translatable("multiplayer.disconnect.authservers_down"));
      }

      return new AuthAttemptEvent.SuccessResult(profile);
    } else if (res.statusCode() == 204) {
      // apparently an offline-mode user logged onto this online-mode proxy
      return new AuthAttemptEvent.FailureResult(
          Component.translatable("velocity.error.online-mode-only", NamedTextColor.RED));
    } else {
      // Something else went wrong
      logger.warn("Recieved an unexpected error code {} from Mojang session server for {} ({})",
          res.statusCode(), username, playerIp);
      return new AuthAttemptEvent.FailureResult(
          Component.translatable("multiplayer.disconnect.authservers_down"));
    }
  }

}
