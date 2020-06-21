package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import io.netty.buffer.ByteBuf;

public interface MinecraftSessionHandler {

  default boolean beforeHandle() {
    return false;
  }

  default void handleGeneric(MinecraftPacket packet) {

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

  default boolean handle(AvailableCommands commands) {
    return false;
  }

  default boolean handle(BossBar packet) {
    return false;
  }

  default boolean handle(Chat packet) {
    return false;
  }

  default boolean handle(ClientSettings packet) {
    return false;
  }

  default boolean handle(Disconnect packet) {
    return false;
  }

  default boolean handle(EncryptionRequest packet) {
    return false;
  }

  default boolean handle(EncryptionResponse packet) {
    return false;
  }

  default boolean handle(Handshake packet) {
    return false;
  }

  default boolean handle(HeaderAndFooter packet) {
    return false;
  }

  default boolean handle(JoinGame packet) {
    return false;
  }

  default boolean handle(KeepAlive packet) {
    return false;
  }

  default boolean handle(LegacyHandshake packet) {
    return false;
  }

  default boolean handle(LegacyPing packet) {
    return false;
  }

  default boolean handle(LoginPluginMessage packet) {
    return false;
  }

  default boolean handle(LoginPluginResponse packet) {
    return false;
  }

  default boolean handle(PluginMessage packet) {
    return false;
  }

  default boolean handle(Respawn packet) {
    return false;
  }

  default boolean handle(ServerLogin packet) {
    return false;
  }

  default boolean handle(ServerLoginSuccess packet) {
    return false;
  }

  default boolean handle(SetCompression packet) {
    return false;
  }

  default boolean handle(StatusPing packet) {
    return false;
  }

  default boolean handle(StatusRequest packet) {
    return false;
  }

  default boolean handle(StatusResponse packet) {
    return false;
  }

  default boolean handle(TabCompleteRequest packet) {
    return false;
  }

  default boolean handle(TabCompleteResponse packet) {
    return false;
  }

  default boolean handle(TitlePacket packet) {
    return false;
  }

  default boolean handle(PlayerListItem packet) {
    return false;
  }

  default boolean handle(ResourcePackRequest packet) {
    return false;
  }

  default boolean handle(ResourcePackResponse packet) {
    return false;
  }
}
