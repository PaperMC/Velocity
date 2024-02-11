package com.velocitypowered.proxy.connection.player.configuration;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurationStateHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationStateHandler.class);
  private final ConnectedPlayer player;

  public ConfigurationStateHandler(final ConnectedPlayer player) {
    this.player = player;
  }

  /**
   * Switches the connection to the client into config state.
   */
  public void switchToConfigState() {
    CompletableFuture.runAsync(() -> {
      player.getConnection().write(StartUpdatePacket.INSTANCE);
      player.getConnection().getChannel().pipeline()
              .get(MinecraftEncoder.class).setState(StateRegistry.CONFIG);
      // Make sure we don't send any play packets to the player after update start
      player.getConnection().addPlayPacketQueueHandler();
    }, player.getConnection().eventLoop()).exceptionally((ex) -> {
      LOGGER.error("Error switching player connection to config state:", ex);
      return null;
    });
  }

  // TODO: Finish this
  public void finishConfigState() {
    CompletableFuture.runAsync(() -> {
      player.getConnection().write(FinishedUpdatePacket.INSTANCE);
      player.getConnection().getChannel().pipeline()
              .get(MinecraftEncoder.class).setState(StateRegistry.PLAY);
      // Make sure we don't send any play packets to the player after update start
      player.getConnection().addPlayPacketQueueHandler();
    }, player.getConnection().eventLoop()).exceptionally((ex) -> {
      LOGGER.error("Error switching player connection to config state:", ex);
      return null;
    });
  }

  public void doWhileConfigState(Runnable action) {

  }
}
