/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.BossBar;
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
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledged;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PingIdentify;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.ServerData;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgement;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerChatCompletion;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChat;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.config.ActiveFeatures;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySync;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdate;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdate;
import com.velocitypowered.proxy.protocol.packet.title.LegacyTitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleActionbarPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleClearPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleSubtitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleTextPacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleTimesPacket;
import io.netty.buffer.ByteBuf;

/**
 * Interface for dispatching received Minecraft packets.
 */
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

  default boolean handle(LegacyChat packet) {
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

  default boolean handle(LegacyTitlePacket packet) {
    return false;
  }

  default boolean handle(TitleTextPacket packet) {
    return false;
  }

  default boolean handle(TitleSubtitlePacket packet) {
    return false;
  }

  default boolean handle(TitleActionbarPacket packet) {
    return false;
  }

  default boolean handle(TitleTimesPacket packet) {
    return false;
  }

  default boolean handle(TitleClearPacket packet) {
    return false;
  }

  default boolean handle(LegacyPlayerListItem packet) {
    return false;
  }

  default boolean handle(ResourcePackRequest packet) {
    return false;
  }

  default boolean handle(ResourcePackResponse packet) {
    return false;
  }

  default boolean handle(KeyedPlayerChat packet) {
    return false;
  }

  default boolean handle(SessionPlayerChat packet) {
    return false;
  }

  default boolean handle(SystemChat packet) {
    return false;
  }

  default boolean handle(KeyedPlayerCommand packet) {
    return false;
  }

  default boolean handle(SessionPlayerCommand packet) {
    return false;
  }

  default boolean handle(PlayerChatCompletion packet) {
    return false;
  }

  default boolean handle(ServerData serverData) {
    return false;
  }

  default boolean handle(RemovePlayerInfo packet) {
    return false;
  }

  default boolean handle(UpsertPlayerInfo packet) {
    return false;
  }

  default boolean handle(LoginAcknowledged packet) {
    return false;
  }

  default boolean handle(ActiveFeatures packet) {
    return false;
  }

  default boolean handle(FinishedUpdate packet) {
    return false;
  }

  default boolean handle(RegistrySync packet) {
    return false;
  }

  default boolean handle(TagsUpdate packet) {
    return false;
  }

  default boolean handle(StartUpdate packet) {
    return false;
  }

  default boolean handle(PingIdentify pingIdentify) {
    return false;
  }

  default boolean handle(ChatAcknowledgement chatAcknowledgement) {
    return false;
  }
}
