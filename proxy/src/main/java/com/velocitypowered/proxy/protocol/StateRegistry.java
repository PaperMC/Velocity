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

package com.velocitypowered.proxy.protocol;

import static com.google.common.collect.Iterables.getLast;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_17;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_18;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_18_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_3;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_7_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION;
import static com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction.CLIENTBOUND;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction.SERVERBOUND;

import com.velocitypowered.api.network.ProtocolVersion;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Registry of all Minecraft protocol states and the packets for each state. */
public enum StateRegistry {

  HANDSHAKE {
    {
      serverbound.register(Handshake.class, Handshake::new,
          map(0x00, MINECRAFT_1_7_2, false));
    }
  },
  STATUS {
    {
      serverbound.register(
          StatusRequest.class, () -> StatusRequest.INSTANCE, map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(StatusPing.class, StatusPing::new, map(0x01, MINECRAFT_1_7_2, false));

      clientbound.register(
          StatusResponse.class, StatusResponse::new, map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(StatusPing.class, StatusPing::new, map(0x01, MINECRAFT_1_7_2, false));
    }
  },
  CONFIG {
    {
      serverbound.register(
          ClientSettings.class, ClientSettings::new, map(0x00, MINECRAFT_1_20_2, false));
      serverbound.register(
          PluginMessage.class, PluginMessage::new, map(0x01, MINECRAFT_1_20_2, false));
      serverbound.register(
          FinishedUpdate.class, FinishedUpdate::new, map(0x02, MINECRAFT_1_20_2, false));
      serverbound.register(KeepAlive.class, KeepAlive::new, map(0x03, MINECRAFT_1_20_2, false));
      serverbound.register(
          PingIdentify.class, PingIdentify::new, map(0x04, MINECRAFT_1_20_2, false));
      serverbound.register(
          ResourcePackResponse.class,
          ResourcePackResponse::new,
          map(0x05, MINECRAFT_1_20_2, false));

      clientbound.register(
          PluginMessage.class, PluginMessage::new, map(0x00, MINECRAFT_1_20_2, false));
      clientbound.register(Disconnect.class, Disconnect::new, map(0x01, MINECRAFT_1_20_2, false));
      clientbound.register(
          FinishedUpdate.class, FinishedUpdate::new, map(0x02, MINECRAFT_1_20_2, false));
      clientbound.register(KeepAlive.class, KeepAlive::new, map(0x03, MINECRAFT_1_20_2, false));
      clientbound.register(
          PingIdentify.class, PingIdentify::new, map(0x04, MINECRAFT_1_20_2, false));
      clientbound.register(
          RegistrySync.class, RegistrySync::new, map(0x05, MINECRAFT_1_20_2, false));
      clientbound.register(
          ResourcePackRequest.class, ResourcePackRequest::new, map(0x06, MINECRAFT_1_20_2, false));
      clientbound.register(
          ActiveFeatures.class, ActiveFeatures::new, map(0x07, MINECRAFT_1_20_2, false));
      clientbound.register(TagsUpdate.class, TagsUpdate::new, map(0x08, MINECRAFT_1_20_2, false));
    }
  },
  PLAY {
    {
      serverbound.fallback = false;
      clientbound.fallback = false;

      serverbound.register(TabCompleteRequest.class, TabCompleteRequest::new,
          map(0x14, MINECRAFT_1_7_2, false),
          map(0x01, MINECRAFT_1_9, false),
          map(0x02, MINECRAFT_1_12, false),
          map(0x01, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_13, false),
          map(0x06, MINECRAFT_1_14, false),
          map(0x08, MINECRAFT_1_19, false),
          map(0x09, MINECRAFT_1_19_1, false),
          map(0x08, MINECRAFT_1_19_3, false),
          map(0x09, MINECRAFT_1_19_4, false),
          map(0x0A, MINECRAFT_1_20_2, false));
      serverbound.register(
          LegacyChat.class,
          LegacyChat::new,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x03, MINECRAFT_1_14, MINECRAFT_1_18_2, false));
      serverbound.register(
              ChatAcknowledgement.class,
              ChatAcknowledgement::new,
              map(0x03, MINECRAFT_1_19_3, false));
      serverbound.register(KeyedPlayerCommand.class, KeyedPlayerCommand::new,
          map(0x03, MINECRAFT_1_19, false),
          map(0x04, MINECRAFT_1_19_1, MINECRAFT_1_19_1, false));
      serverbound.register(KeyedPlayerChat.class, KeyedPlayerChat::new,
          map(0x04, MINECRAFT_1_19, false),
          map(0x05, MINECRAFT_1_19_1, MINECRAFT_1_19_1, false));
      serverbound.register(SessionPlayerCommand.class, SessionPlayerCommand::new,
          map(0x04, MINECRAFT_1_19_3, false));
      serverbound.register(
          SessionPlayerChat.class,
          SessionPlayerChat::new,
          map(0x05, MINECRAFT_1_19_3, MINECRAFT_1_20_2, false));
      serverbound.register(
          ClientSettings.class,
          ClientSettings::new,
          map(0x15, MINECRAFT_1_7_2, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_14, false),
          map(0x07, MINECRAFT_1_19, false),
          map(0x08, MINECRAFT_1_19_1, false),
          map(0x07, MINECRAFT_1_19_3, false),
          map(0x08, MINECRAFT_1_19_4, false),
          map(0x09, MINECRAFT_1_20_2, false));
      serverbound.register(
          PluginMessage.class,
          PluginMessage::new,
          map(0x17, MINECRAFT_1_7_2, false),
          map(0x09, MINECRAFT_1_9, false),
          map(0x0A, MINECRAFT_1_12, false),
          map(0x09, MINECRAFT_1_12_1, false),
          map(0x0A, MINECRAFT_1_13, false),
          map(0x0B, MINECRAFT_1_14, false),
          map(0x0A, MINECRAFT_1_17, false),
          map(0x0C, MINECRAFT_1_19, false),
          map(0x0D, MINECRAFT_1_19_1, false),
          map(0x0C, MINECRAFT_1_19_3, false),
          map(0x0D, MINECRAFT_1_19_4, false),
          map(0x0F, MINECRAFT_1_20_2, false));
      serverbound.register(
          KeepAlive.class,
          KeepAlive::new,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x0B, MINECRAFT_1_9, false),
          map(0x0C, MINECRAFT_1_12, false),
          map(0x0B, MINECRAFT_1_12_1, false),
          map(0x0E, MINECRAFT_1_13, false),
          map(0x0F, MINECRAFT_1_14, false),
          map(0x10, MINECRAFT_1_16, false),
          map(0x0F, MINECRAFT_1_17, false),
          map(0x11, MINECRAFT_1_19, false),
          map(0x12, MINECRAFT_1_19_1, false),
          map(0x11, MINECRAFT_1_19_3, false),
          map(0x12, MINECRAFT_1_19_4, false),
          map(0x14, MINECRAFT_1_20_2, false));
      serverbound.register(
          ResourcePackResponse.class,
          ResourcePackResponse::new,
          map(0x19, MINECRAFT_1_8, false),
          map(0x16, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x1D, MINECRAFT_1_13, false),
          map(0x1F, MINECRAFT_1_14, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x21, MINECRAFT_1_16_2, false),
          map(0x23, MINECRAFT_1_19, false),
          map(0x24, MINECRAFT_1_19_1, false),
          map(0x27, MINECRAFT_1_20_2, false));
      serverbound.register(
          FinishedUpdate.class, FinishedUpdate::new, map(0x0B, MINECRAFT_1_20_2, false));

      clientbound.register(
          BossBar.class,
          BossBar::new,
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0D, MINECRAFT_1_15, false),
          map(0x0C, MINECRAFT_1_16, false),
          map(0x0D, MINECRAFT_1_17, false),
          map(0x0A, MINECRAFT_1_19, false),
          map(0x0B, MINECRAFT_1_19_4, false),
          map(0x0A, MINECRAFT_1_20_2, false));
      clientbound.register(
          LegacyChat.class,
          LegacyChat::new,
          map(0x02, MINECRAFT_1_7_2, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0F, MINECRAFT_1_15, true),
          map(0x0E, MINECRAFT_1_16, true),
          map(0x0F, MINECRAFT_1_17, MINECRAFT_1_18_2, true));
      clientbound.register(TabCompleteResponse.class, TabCompleteResponse::new,
          map(0x3A, MINECRAFT_1_7_2, false),
          map(0x0E, MINECRAFT_1_9, false),
          map(0x10, MINECRAFT_1_13, false),
          map(0x11, MINECRAFT_1_15, false),
          map(0x10, MINECRAFT_1_16, false),
          map(0x0F, MINECRAFT_1_16_2, false),
          map(0x11, MINECRAFT_1_17, false),
          map(0x0E, MINECRAFT_1_19, false),
          map(0x0D, MINECRAFT_1_19_3, false),
          map(0x0F, MINECRAFT_1_19_4, false),
          map(0x10, MINECRAFT_1_20_2, false));
      clientbound.register(
          AvailableCommands.class,
          AvailableCommands::new,
          map(0x11, MINECRAFT_1_13, false),
          map(0x12, MINECRAFT_1_15, false),
          map(0x11, MINECRAFT_1_16, false),
          map(0x10, MINECRAFT_1_16_2, false),
          map(0x12, MINECRAFT_1_17, false),
          map(0x0F, MINECRAFT_1_19, false),
          map(0x0E, MINECRAFT_1_19_3, false),
          map(0x10, MINECRAFT_1_19_4, false),
          map(0x11, MINECRAFT_1_20_2, false));
      clientbound.register(
          PluginMessage.class,
          PluginMessage::new,
          map(0x3F, MINECRAFT_1_7_2, false),
          map(0x18, MINECRAFT_1_9, false),
          map(0x19, MINECRAFT_1_13, false),
          map(0x18, MINECRAFT_1_14, false),
          map(0x19, MINECRAFT_1_15, false),
          map(0x18, MINECRAFT_1_16, false),
          map(0x17, MINECRAFT_1_16_2, false),
          map(0x18, MINECRAFT_1_17, false),
          map(0x15, MINECRAFT_1_19, false),
          map(0x16, MINECRAFT_1_19_1, false),
          map(0x15, MINECRAFT_1_19_3, false),
          map(0x17, MINECRAFT_1_19_4, false),
          map(0x18, MINECRAFT_1_20_2, false));
      clientbound.register(
          Disconnect.class,
          Disconnect::new,
          map(0x40, MINECRAFT_1_7_2, false),
          map(0x1A, MINECRAFT_1_9, false),
          map(0x1B, MINECRAFT_1_13, false),
          map(0x1A, MINECRAFT_1_14, false),
          map(0x1B, MINECRAFT_1_15, false),
          map(0x1A, MINECRAFT_1_16, false),
          map(0x19, MINECRAFT_1_16_2, false),
          map(0x1A, MINECRAFT_1_17, false),
          map(0x17, MINECRAFT_1_19, false),
          map(0x19, MINECRAFT_1_19_1, false),
          map(0x17, MINECRAFT_1_19_3, false),
          map(0x1A, MINECRAFT_1_19_4, false),
          map(0x1B, MINECRAFT_1_20_2, false));
      clientbound.register(
          KeepAlive.class,
          KeepAlive::new,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x1F, MINECRAFT_1_9, false),
          map(0x21, MINECRAFT_1_13, false),
          map(0x20, MINECRAFT_1_14, false),
          map(0x21, MINECRAFT_1_15, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x1F, MINECRAFT_1_16_2, false),
          map(0x21, MINECRAFT_1_17, false),
          map(0x1E, MINECRAFT_1_19, false),
          map(0x20, MINECRAFT_1_19_1, false),
          map(0x1F, MINECRAFT_1_19_3, false),
          map(0x23, MINECRAFT_1_19_4, false),
          map(0x24, MINECRAFT_1_20_2, false));
      clientbound.register(
          JoinGame.class,
          JoinGame::new,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x23, MINECRAFT_1_9, false),
          map(0x25, MINECRAFT_1_13, false),
          map(0x25, MINECRAFT_1_14, false),
          map(0x26, MINECRAFT_1_15, false),
          map(0x25, MINECRAFT_1_16, false),
          map(0x24, MINECRAFT_1_16_2, false),
          map(0x26, MINECRAFT_1_17, false),
          map(0x23, MINECRAFT_1_19, false),
          map(0x25, MINECRAFT_1_19_1, false),
          map(0x24, MINECRAFT_1_19_3, false),
          map(0x28, MINECRAFT_1_19_4, false),
          map(0x29, MINECRAFT_1_20_2, false));
      clientbound.register(
          Respawn.class,
          Respawn::new,
          map(0x07, MINECRAFT_1_7_2, true),
          map(0x33, MINECRAFT_1_9, true),
          map(0x34, MINECRAFT_1_12, true),
          map(0x35, MINECRAFT_1_12_1, true),
          map(0x38, MINECRAFT_1_13, true),
          map(0x3A, MINECRAFT_1_14, true),
          map(0x3B, MINECRAFT_1_15, true),
          map(0x3A, MINECRAFT_1_16, true),
          map(0x39, MINECRAFT_1_16_2, true),
          map(0x3D, MINECRAFT_1_17, true),
          map(0x3B, MINECRAFT_1_19, true),
          map(0x3E, MINECRAFT_1_19_1, true),
          map(0x3D, MINECRAFT_1_19_3, true),
          map(0x41, MINECRAFT_1_19_4, true),
          map(0x43, MINECRAFT_1_20_2, true));
      clientbound.register(
          ResourcePackRequest.class,
          ResourcePackRequest::new,
          map(0x48, MINECRAFT_1_8, false),
          map(0x32, MINECRAFT_1_9, false),
          map(0x33, MINECRAFT_1_12, false),
          map(0x34, MINECRAFT_1_12_1, false),
          map(0x37, MINECRAFT_1_13, false),
          map(0x39, MINECRAFT_1_14, false),
          map(0x3A, MINECRAFT_1_15, false),
          map(0x39, MINECRAFT_1_16, false),
          map(0x38, MINECRAFT_1_16_2, false),
          map(0x3C, MINECRAFT_1_17, false),
          map(0x3A, MINECRAFT_1_19, false),
          map(0x3D, MINECRAFT_1_19_1, false),
          map(0x3C, MINECRAFT_1_19_3, false),
          map(0x40, MINECRAFT_1_19_4, false),
          map(0x42, MINECRAFT_1_20_2, false));
      clientbound.register(
          HeaderAndFooter.class,
          HeaderAndFooter::new,
          map(0x47, MINECRAFT_1_8, true),
          map(0x48, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_9_4, true),
          map(0x49, MINECRAFT_1_12, true),
          map(0x4A, MINECRAFT_1_12_1, true),
          map(0x4E, MINECRAFT_1_13, true),
          map(0x53, MINECRAFT_1_14, true),
          map(0x54, MINECRAFT_1_15, true),
          map(0x53, MINECRAFT_1_16, true),
          map(0x5E, MINECRAFT_1_17, true),
          map(0x5F, MINECRAFT_1_18, true),
          map(0x60, MINECRAFT_1_19, true),
          map(0x63, MINECRAFT_1_19_1, true),
          map(0x61, MINECRAFT_1_19_3, true),
          map(0x65, MINECRAFT_1_19_4, true),
          map(0x68, MINECRAFT_1_20_2, true));
      clientbound.register(
          LegacyTitlePacket.class,
          LegacyTitlePacket::new,
          map(0x45, MINECRAFT_1_8, true),
          map(0x45, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_12, true),
          map(0x48, MINECRAFT_1_12_1, true),
          map(0x4B, MINECRAFT_1_13, true),
          map(0x4F, MINECRAFT_1_14, true),
          map(0x50, MINECRAFT_1_15, true),
          map(0x4F, MINECRAFT_1_16, MINECRAFT_1_16_4, true));
      clientbound.register(TitleSubtitlePacket.class, TitleSubtitlePacket::new,
          map(0x57, MINECRAFT_1_17, true),
          map(0x58, MINECRAFT_1_18, true),
          map(0x5B, MINECRAFT_1_19_1, true),
          map(0x59, MINECRAFT_1_19_3, true),
          map(0x5D, MINECRAFT_1_19_4, true),
          map(0x5F, MINECRAFT_1_20_2, true));
      clientbound.register(
          TitleTextPacket.class,
          TitleTextPacket::new,
          map(0x59, MINECRAFT_1_17, true),
          map(0x5A, MINECRAFT_1_18, true),
          map(0x5D, MINECRAFT_1_19_1, true),
          map(0x5B, MINECRAFT_1_19_3, true),
          map(0x5F, MINECRAFT_1_19_4, true),
          map(0x61, MINECRAFT_1_20_2, true));
      clientbound.register(
          TitleActionbarPacket.class,
          TitleActionbarPacket::new,
          map(0x41, MINECRAFT_1_17, true),
          map(0x40, MINECRAFT_1_19, true),
          map(0x43, MINECRAFT_1_19_1, true),
          map(0x42, MINECRAFT_1_19_3, true),
          map(0x46, MINECRAFT_1_19_4, true),
          map(0x48, MINECRAFT_1_20_2, true));
      clientbound.register(
          TitleTimesPacket.class,
          TitleTimesPacket::new,
          map(0x5A, MINECRAFT_1_17, true),
          map(0x5B, MINECRAFT_1_18, true),
          map(0x5E, MINECRAFT_1_19_1, true),
          map(0x5C, MINECRAFT_1_19_3, true),
          map(0x60, MINECRAFT_1_19_4, true),
          map(0x62, MINECRAFT_1_20_2, true));
      clientbound.register(
          TitleClearPacket.class,
          TitleClearPacket::new,
          map(0x10, MINECRAFT_1_17, true),
          map(0x0D, MINECRAFT_1_19, true),
          map(0x0C, MINECRAFT_1_19_3, true),
          map(0x0E, MINECRAFT_1_19_4, true),
          map(0x0F, MINECRAFT_1_20_2, true));
      clientbound.register(
          LegacyPlayerListItem.class,
          LegacyPlayerListItem::new,
          map(0x38, MINECRAFT_1_7_2, false),
          map(0x2D, MINECRAFT_1_9, false),
          map(0x2E, MINECRAFT_1_12_1, false),
          map(0x30, MINECRAFT_1_13, false),
          map(0x33, MINECRAFT_1_14, false),
          map(0x34, MINECRAFT_1_15, false),
          map(0x33, MINECRAFT_1_16, false),
          map(0x32, MINECRAFT_1_16_2, false),
          map(0x36, MINECRAFT_1_17, false),
          map(0x34, MINECRAFT_1_19, false),
          map(0x37, MINECRAFT_1_19_1, MINECRAFT_1_19_1, false));
      clientbound.register(RemovePlayerInfo.class, RemovePlayerInfo::new,
          map(0x35, MINECRAFT_1_19_3, false),
          map(0x39, MINECRAFT_1_19_4, false),
          map(0x3B, MINECRAFT_1_20_2, false));
      clientbound.register(
          UpsertPlayerInfo.class,
          UpsertPlayerInfo::new,
          map(0x36, MINECRAFT_1_19_3, false),
          map(0x3A, MINECRAFT_1_19_4, false),
          map(0x3C, MINECRAFT_1_20_2, false));
      clientbound.register(
          SystemChat.class,
          SystemChat::new,
          map(0x5F, MINECRAFT_1_19, true),
          map(0x62, MINECRAFT_1_19_1, true),
          map(0x60, MINECRAFT_1_19_3, true),
          map(0x64, MINECRAFT_1_19_4, true),
          map(0x67, MINECRAFT_1_20_2, true));
      clientbound.register(
          PlayerChatCompletion.class,
          PlayerChatCompletion::new,
          map(0x15, MINECRAFT_1_19_1, true),
          map(0x14, MINECRAFT_1_19_3, true),
          map(0x16, MINECRAFT_1_19_4, true),
          map(0x17, MINECRAFT_1_20_2, true));
      clientbound.register(
          ServerData.class,
          ServerData::new,
          map(0x3F, MINECRAFT_1_19, false),
          map(0x42, MINECRAFT_1_19_1, false),
          map(0x41, MINECRAFT_1_19_3, false),
          map(0x45, MINECRAFT_1_19_4, false),
          map(0x47, MINECRAFT_1_20_2, false));
      clientbound.register(StartUpdate.class, StartUpdate::new, map(0x65, MINECRAFT_1_20_2, false));
    }
  },
  LOGIN {
    {
      serverbound.register(ServerLogin.class, ServerLogin::new, map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(
          EncryptionResponse.class, EncryptionResponse::new, map(0x01, MINECRAFT_1_7_2, false));
      serverbound.register(
          LoginPluginResponse.class, LoginPluginResponse::new, map(0x02, MINECRAFT_1_13, false));
      serverbound.register(
          LoginAcknowledged.class, LoginAcknowledged::new, map(0x03, MINECRAFT_1_20_2, false));

      clientbound.register(Disconnect.class, Disconnect::new, map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(
          EncryptionRequest.class, EncryptionRequest::new, map(0x01, MINECRAFT_1_7_2, false));
      clientbound.register(
          ServerLoginSuccess.class, ServerLoginSuccess::new, map(0x02, MINECRAFT_1_7_2, false));
      clientbound.register(
          SetCompression.class, SetCompression::new, map(0x03, MINECRAFT_1_8, false));
      clientbound.register(
          LoginPluginMessage.class, LoginPluginMessage::new, map(0x04, MINECRAFT_1_13, false));
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  protected final PacketRegistry clientbound = new PacketRegistry(CLIENTBOUND, this);
  protected final PacketRegistry serverbound = new PacketRegistry(SERVERBOUND, this);

  public StateRegistry.PacketRegistry.ProtocolRegistry getProtocolRegistry(Direction direction,
      ProtocolVersion version) {
    return (direction == SERVERBOUND ? serverbound : clientbound).getProtocolRegistry(version);
  }

  /** Packet registry. */
  public static class PacketRegistry {

    private final Direction direction;
    private final StateRegistry registry;
    private final Map<ProtocolVersion, ProtocolRegistry> versions;
    private boolean fallback = true;

    PacketRegistry(Direction direction, StateRegistry registry) {
      this.direction = direction;
      this.registry = registry;

      Map<ProtocolVersion, ProtocolRegistry> mutableVersions = new EnumMap<>(ProtocolVersion.class);
      for (ProtocolVersion version : ProtocolVersion.values()) {
        if (!version.isLegacy() && !version.isUnknown()) {
          mutableVersions.put(version, new ProtocolRegistry(version));
        }
      }

      this.versions = Collections.unmodifiableMap(mutableVersions);
    }

    ProtocolRegistry getProtocolRegistry(final ProtocolVersion version) {
      ProtocolRegistry registry = versions.get(version);
      if (registry == null) {
        if (fallback) {
          return getProtocolRegistry(MINIMUM_VERSION);
        }
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> packetSupplier,
        PacketMapping... mappings) {
      if (mappings.length == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (int i = 0; i < mappings.length; i++) {
        PacketMapping current = mappings[i];
        PacketMapping next = (i + 1 < mappings.length) ? mappings[i + 1] : current;

        ProtocolVersion from = current.protocolVersion;
        ProtocolVersion lastValid = current.lastValidProtocolVersion;
        if (lastValid != null) {
          if (next != current) {
            throw new IllegalArgumentException("Cannot add a mapping after last valid mapping");
          }
          if (from.compareTo(lastValid) > 0) {
            throw new IllegalArgumentException(
                "Last mapping version cannot be higher than highest mapping version");
          }
        }
        ProtocolVersion to = current == next ? lastValid != null
            ? lastValid : getLast(SUPPORTED_VERSIONS) : next.protocolVersion;

        ProtocolVersion lastInList = lastValid != null ? lastValid : getLast(SUPPORTED_VERSIONS);

        if (from.compareTo(to) >= 0 && from != lastInList) {
          throw new IllegalArgumentException(String.format(
              "Next mapping version (%s) should be lower then current (%s)", to, from));
        }

        for (ProtocolVersion protocol : EnumSet.range(from, to)) {
          if (protocol == to && next != current) {
            break;
          }
          ProtocolRegistry registry = this.versions.get(protocol);
          if (registry == null) {
            throw new IllegalArgumentException(
                "Unknown protocol version " + current.protocolVersion);
          }

          if (registry.packetIdToSupplier.containsKey(current.id)) {
            throw new IllegalArgumentException(
                "Can not register class "
                    + clazz.getSimpleName()
                    + " with id "
                    + current.id
                    + " for "
                    + registry.version
                    + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(
                clazz.getSimpleName() + " is already registered for version " + registry.version);
          }

          if (!current.encodeOnly) {
            registry.packetIdToSupplier.put(current.id, packetSupplier);
          }
          registry.packetClassToId.put(clazz, current.id);
        }
      }
    }

    /** Protocol registry. */
    public class ProtocolRegistry {

      public final ProtocolVersion version;
      final IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier =
          new IntObjectHashMap<>(16, 0.5f);
      final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId =
          new Object2IntOpenHashMap<>(16, 0.5f);

      ProtocolRegistry(final ProtocolVersion version) {
        this.version = version;
        this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
      }

      /**
       * Attempts to create a packet from the specified {@code id}.
       *
       * @param id the packet ID
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable MinecraftPacket createPacket(final int id) {
        final Supplier<? extends MinecraftPacket> supplier = this.packetIdToSupplier.get(id);
        if (supplier == null) {
          return null;
        }
        return supplier.get();
      }

      /**
       * Attempts to look up the packet ID for an {@code packet}.
       *
       * @param packet the packet to look up
       * @return the packet ID
       * @throws IllegalArgumentException if the packet ID is not found
       */
      public int getPacketId(final MinecraftPacket packet) {
        final int id = this.packetClassToId.getInt(packet.getClass());
        if (id == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(String.format(
              "Unable to find id for packet of type %s in %s protocol %s phase %s",
              packet.getClass().getName(), PacketRegistry.this.direction,
                  this.version, PacketRegistry.this.registry
          ));
        }
        return id;
      }

      /**
       * Checks if the registry contains a packet with the specified {@code id}.
       *
       * @param packet the packet to check
       * @return {@code true} if the packet is registered, {@code false} otherwise
       */
      public boolean containsPacket(final MinecraftPacket packet) {
        return this.packetClassToId.containsKey(packet.getClass());
      }
    }
  }

  /** Packet mapping. */
  public static final class PacketMapping {

    private final int id;
    private final ProtocolVersion protocolVersion;
    private final boolean encodeOnly;
    private final @Nullable ProtocolVersion lastValidProtocolVersion;

    PacketMapping(int id, ProtocolVersion protocolVersion,
        ProtocolVersion lastValidProtocolVersion, boolean packetDecoding) {
      this.id = id;
      this.protocolVersion = protocolVersion;
      this.lastValidProtocolVersion = lastValidProtocolVersion;
      this.encodeOnly = packetDecoding;
    }

    @Override
    public String toString() {
      return "PacketMapping{"
          + "id="
          + id
          + ", protocolVersion="
          + protocolVersion
          + ", encodeOnly="
          + encodeOnly
          + '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PacketMapping that = (PacketMapping) o;
      return id == that.id
          && protocolVersion == that.protocolVersion
          && encodeOnly == that.encodeOnly;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, protocolVersion, encodeOnly);
    }
  }

  /**
   * Creates a PacketMapping using the provided arguments.
   *
   * @param id         Packet Id
   * @param version    Protocol version
   * @param encodeOnly When true packet decoding will be disabled
   * @return PacketMapping with the provided arguments
   */
  @SuppressFBWarnings({"UPM_UNCALLED_PRIVATE_METHOD"})
  private static PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
    return map(id, version, null, encodeOnly);
  }

  /**
   * Creates a PacketMapping using the provided arguments.
   *
   * @param id                       Packet Id
   * @param version                  Protocol version
   * @param encodeOnly               When true packet decoding will be disabled
   * @param lastValidProtocolVersion Last version this Mapping is valid at
   * @return PacketMapping with the provided arguments
   */
  private static PacketMapping map(int id, ProtocolVersion version,
      ProtocolVersion lastValidProtocolVersion, boolean encodeOnly) {
    return new PacketMapping(id, version, lastValidProtocolVersion, encodeOnly);
  }

}
