package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.proxy.network.packet.clientbound.ClientboundAvailableCommandsPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundBossBarPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundChatPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundHeaderAndFooterPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundJoinGamePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundLoginPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPlayerListItemPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundResourcePackRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundRespawnPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundSetCompressionPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTabCompleteResponsePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTitlePacket;
import com.velocitypowered.proxy.network.packet.legacy.LegacyHandshakePacket;
import com.velocitypowered.proxy.network.packet.legacy.LegacyPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundChatPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundClientSettingsPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundEncryptionResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundLoginPluginResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundResourcePackResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundTabCompleteRequestPacket;
import com.velocitypowered.proxy.network.packet.shared.PluginMessagePacket;

public interface PacketHandler {
  default boolean handle(PluginMessagePacket packet) {
    return false;
  }

  /*
   * Clientbound
   */

  default boolean handle(ClientboundAvailableCommandsPacket commands) {
    return false;
  }

  default boolean handle(ClientboundBossBarPacket packet) {
    return false;
  }

  default boolean handle(ClientboundChatPacket packet) {
    return false;
  }

  default boolean handle(ClientboundDisconnectPacket packet) {
    return false;
  }

  default boolean handle(ClientboundEncryptionRequestPacket packet) {
    return false;
  }

  default boolean handle(ClientboundHeaderAndFooterPacket packet) {
    return false;
  }

  default boolean handle(ClientboundJoinGamePacket packet) {
    return false;
  }

  default boolean handle(ClientboundKeepAlivePacket packet) {
    return false;
  }

  default boolean handle(ClientboundLoginPluginMessagePacket packet) {
    return false;
  }

  default boolean handle(ClientboundPlayerListItemPacket packet) {
    return false;
  }

  default boolean handle(ClientboundResourcePackRequestPacket packet) {
    return false;
  }

  default boolean handle(ClientboundRespawnPacket packet) {
    return false;
  }

  default boolean handle(ClientboundServerLoginSuccessPacket packet) {
    return false;
  }

  default boolean handle(ClientboundSetCompressionPacket packet) {
    return false;
  }

  default boolean handle(ClientboundStatusPingPacket packet) {
    return false;
  }

  default boolean handle(ClientboundStatusResponsePacket packet) {
    return false;
  }

  default boolean handle(ClientboundTabCompleteResponsePacket packet) {
    return false;
  }

  default boolean handle(ClientboundTitlePacket packet) {
    return false;
  }

  /*
   * Serverbound
   */

  default boolean handle(ServerboundChatPacket packet) {
    return false;
  }

  default boolean handle(ServerboundClientSettingsPacket packet) {
    return false;
  }

  default boolean handle(ServerboundEncryptionResponsePacket packet) {
    return false;
  }

  default boolean handle(ServerboundHandshakePacket packet) {
    return false;
  }

  default boolean handle(ServerboundKeepAlivePacket packet) {
    return false;
  }

  default boolean handle(ServerboundLoginPluginResponsePacket packet) {
    return false;
  }

  default boolean handle(ServerboundResourcePackResponsePacket packet) {
    return false;
  }

  default boolean handle(ServerboundServerLoginPacket packet) {
    return false;
  }

  default boolean handle(ServerboundStatusPingPacket packet) {
    return false;
  }

  default boolean handle(ServerboundStatusRequestPacket packet) {
    return false;
  }

  default boolean handle(ServerboundTabCompleteRequestPacket packet) {
    return false;
  }

  /*
   * Legacy
   */

  default boolean handle(LegacyHandshakePacket packet) {
    return false;
  }

  default boolean handle(LegacyPingPacket packet) {
    return false;
  }
}
