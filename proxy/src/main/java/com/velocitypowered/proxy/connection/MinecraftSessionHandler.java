package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.ChatPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.PlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket;
import com.velocitypowered.proxy.protocol.packet.SetCompressionPacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.protocol.packet.StatusResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.protocol.packet.legacy.LegacyHandshakePacket;
import com.velocitypowered.proxy.protocol.packet.legacy.LegacyPingPacket;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler {

  default boolean beforeHandle() {
    return false;
  }

  default void handleGeneric(Packet packet) {

  }

  default void handleUnknown(ByteBuf buf) {

  }

  default void connected() {

  }

  default void disconnected() {

  }

  default void activated() {

  }

  default void deactivated() {

  }

  default void exception(Throwable throwable) {

  }

  default void writabilityChanged() {

  }

  default void readCompleted() {

  }

  default boolean handle(AvailableCommandsPacket commands) {
    return false;
  }

  default boolean handle(BossBarPacket packet) {
    return false;
  }

  default boolean handle(ChatPacket packet) {
    return false;
  }

  default boolean handle(ClientSettingsPacket packet) {
    return false;
  }

  default boolean handle(DisconnectPacket packet) {
    return false;
  }

  default boolean handle(EncryptionRequestPacket packet) {
    return false;
  }

  default boolean handle(EncryptionResponsePacket packet) {
    return false;
  }

  default boolean handle(HandshakePacket packet) {
    return false;
  }

  default boolean handle(HeaderAndFooterPacket packet) {
    return false;
  }

  default boolean handle(JoinGamePacket packet) {
    return false;
  }

  default boolean handle(KeepAlivePacket packet) {
    return false;
  }

  default boolean handle(LegacyHandshakePacket packet) {
    return false;
  }

  default boolean handle(LegacyPingPacket packet) {
    return false;
  }

  default boolean handle(LoginPluginMessagePacket packet) {
    return false;
  }

  default boolean handle(LoginPluginResponsePacket packet) {
    return false;
  }

  default boolean handle(PluginMessagePacket packet) {
    return false;
  }

  default boolean handle(RespawnPacket packet) {
    return false;
  }

  default boolean handle(ServerLoginPacket packet) {
    return false;
  }

  default boolean handle(ServerLoginSuccessPacket packet) {
    return false;
  }

  default boolean handle(SetCompressionPacket packet) {
    return false;
  }

  default boolean handle(StatusPingPacket packet) {
    return false;
  }

  default boolean handle(StatusRequestPacket packet) {
    return false;
  }

  default boolean handle(StatusResponsePacket packet) {
    return false;
  }

  default boolean handle(TabCompleteRequestPacket packet) {
    return false;
  }

  default boolean handle(TabCompleteResponsePacket packet) {
    return false;
  }

  default boolean handle(TitlePacket packet) {
    return false;
  }

  default boolean handle(PlayerListItemPacket packet) {
    return false;
  }

  default boolean handle(ResourcePackRequestPacket packet) {
    return false;
  }

  default boolean handle(ResourcePackResponsePacket packet) {
    return false;
  }
}
