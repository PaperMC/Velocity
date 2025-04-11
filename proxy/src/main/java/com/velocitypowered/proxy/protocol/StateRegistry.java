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
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_5;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_5;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_7_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION;
import static com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction.CLIENTBOUND;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction.SERVERBOUND;

import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.BundleDelimiterPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundCookieRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundStoreCookiePacket;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledgedPacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.PingIdentifyPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.RemovePlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.RemoveResourcePackPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.ServerDataPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket;
import com.velocitypowered.proxy.protocol.packet.ServerboundCookieResponsePacket;
import com.velocitypowered.proxy.protocol.packet.SetCompressionPacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.protocol.packet.StatusResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TransferPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatAcknowledgementPacket;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerChatCompletionPacket;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.chat.session.UnsignedPlayerCommandPacket;
import com.velocitypowered.proxy.protocol.packet.config.ActiveFeaturesPacket;
import com.velocitypowered.proxy.protocol.packet.config.ClientboundCustomReportDetailsPacket;
import com.velocitypowered.proxy.protocol.packet.config.ClientboundServerLinksPacket;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.KnownPacksPacket;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySyncPacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdatePacket;
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

/**
 * Registry of all Minecraft protocol states and the packets for each state.
 */
public enum StateRegistry {

  HANDSHAKE {
    {
      serverbound.register(HandshakePacket.class, HandshakePacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
    }
  },
  STATUS {
    {
      serverbound.register(
          StatusRequestPacket.class, () -> StatusRequestPacket.INSTANCE,
          map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(StatusPingPacket.class, StatusPingPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));

      clientbound.register(
          StatusResponsePacket.class, StatusResponsePacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(StatusPingPacket.class, StatusPingPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
    }
  },
  CONFIG {
    {
      serverbound.register(
          ClientSettingsPacket.class, ClientSettingsPacket::new,
          map(0x00, MINECRAFT_1_20_2, false));
      serverbound.register(
          ServerboundCookieResponsePacket.class, ServerboundCookieResponsePacket::new,
          map(0x01, MINECRAFT_1_20_5, false));
      serverbound.register(
          PluginMessagePacket.class, PluginMessagePacket::new,
          map(0x01, MINECRAFT_1_20_2, false),
          map(0x02, MINECRAFT_1_20_5, false));
      serverbound.register(
          FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE,
          map(0x02, MINECRAFT_1_20_2, false),
          map(0x03, MINECRAFT_1_20_5, false));
      serverbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
          map(0x03, MINECRAFT_1_20_2, false),
          map(0x04, MINECRAFT_1_20_5, false));
      serverbound.register(
          PingIdentifyPacket.class, PingIdentifyPacket::new,
          map(0x04, MINECRAFT_1_20_2, false),
          map(0x05, MINECRAFT_1_20_5, false));
      serverbound.register(
          ResourcePackResponsePacket.class,
          ResourcePackResponsePacket::new,
          map(0x05, MINECRAFT_1_20_2, false),
          map(0x06, MINECRAFT_1_20_5, false));
      serverbound.register(
          KnownPacksPacket.class,
          KnownPacksPacket::new,
          map(0x07, MINECRAFT_1_20_5, false));

      clientbound.register(
          ClientboundCookieRequestPacket.class, ClientboundCookieRequestPacket::new,
          map(0x00, MINECRAFT_1_20_5, false));
      clientbound.register(
          PluginMessagePacket.class, PluginMessagePacket::new,
          map(0x00, MINECRAFT_1_20_2, false),
          map(0x01, MINECRAFT_1_20_5, false));
      clientbound.register(
          DisconnectPacket.class, () -> new DisconnectPacket(this),
          map(0x01, MINECRAFT_1_20_2, false),
          map(0x02, MINECRAFT_1_20_5, false));
      clientbound.register(
          FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE,
          map(0x02, MINECRAFT_1_20_2, false),
          map(0x03, MINECRAFT_1_20_5, false));
      clientbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
          map(0x03, MINECRAFT_1_20_2, false),
          map(0x04, MINECRAFT_1_20_5, false));
      clientbound.register(
          PingIdentifyPacket.class, PingIdentifyPacket::new,
          map(0x04, MINECRAFT_1_20_2, false),
          map(0x05, MINECRAFT_1_20_5, false));
      clientbound.register(
          RegistrySyncPacket.class, RegistrySyncPacket::new,
          map(0x05, MINECRAFT_1_20_2, false),
          map(0x07, MINECRAFT_1_20_5, false));
      clientbound.register(
          RemoveResourcePackPacket.class, RemoveResourcePackPacket::new,
          map(0x06, MINECRAFT_1_20_3, false),
          map(0x08, MINECRAFT_1_20_5, false));
      clientbound.register(ResourcePackRequestPacket.class, ResourcePackRequestPacket::new,
          map(0x06, MINECRAFT_1_20_2, false),
          map(0x07, MINECRAFT_1_20_3, false),
          map(0x09, MINECRAFT_1_20_5, false));
      clientbound.register(
          ClientboundStoreCookiePacket.class, ClientboundStoreCookiePacket::new,
          map(0x0A, MINECRAFT_1_20_5, false));
      clientbound.register(TransferPacket.class, TransferPacket::new,
          map(0x0B, MINECRAFT_1_20_5, false));
      clientbound.register(ActiveFeaturesPacket.class, ActiveFeaturesPacket::new,
          map(0x07, MINECRAFT_1_20_2, false),
          map(0x08, MINECRAFT_1_20_3, false),
          map(0x0C, MINECRAFT_1_20_5, false));
      clientbound.register(TagsUpdatePacket.class, TagsUpdatePacket::new,
          map(0x08, MINECRAFT_1_20_2, false),
          map(0x09, MINECRAFT_1_20_3, false),
          map(0x0D, MINECRAFT_1_20_5, false));
      clientbound.register(KnownPacksPacket.class, KnownPacksPacket::new,
          map(0x0E, MINECRAFT_1_20_5, false));
      clientbound.register(ClientboundCustomReportDetailsPacket.class, ClientboundCustomReportDetailsPacket::new,
          map(0x0F, MINECRAFT_1_21, false));
      clientbound.register(ClientboundServerLinksPacket.class, ClientboundServerLinksPacket::new,
          map(0x10, MINECRAFT_1_21, false));
    }
  },
  PLAY {
    {
      serverbound.fallback = false;
      clientbound.fallback = false;

      serverbound.register(TabCompleteRequestPacket.class, TabCompleteRequestPacket::new,
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
          map(0x0A, MINECRAFT_1_20_2, false),
          map(0x0B, MINECRAFT_1_20_5, false),
          map(0x0D, MINECRAFT_1_21_2, false));
      serverbound.register(
          LegacyChatPacket.class,
          LegacyChatPacket::new,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x03, MINECRAFT_1_14, MINECRAFT_1_18_2, false));
      serverbound.register(
          ChatAcknowledgementPacket.class,
          ChatAcknowledgementPacket::new,
          map(0x03, MINECRAFT_1_19_3, false),
          map(0x04, MINECRAFT_1_21_2, false));
      serverbound.register(KeyedPlayerCommandPacket.class, KeyedPlayerCommandPacket::new,
          map(0x03, MINECRAFT_1_19, false),
          map(0x04, MINECRAFT_1_19_1, MINECRAFT_1_19_1, false));
      serverbound.register(KeyedPlayerChatPacket.class, KeyedPlayerChatPacket::new,
          map(0x04, MINECRAFT_1_19, false),
          map(0x05, MINECRAFT_1_19_1, MINECRAFT_1_19_1, false));
      serverbound.register(SessionPlayerCommandPacket.class, SessionPlayerCommandPacket::new,
          map(0x04, MINECRAFT_1_19_3, false),
          map(0x05, MINECRAFT_1_20_5, false),
          map(0x06, MINECRAFT_1_21_2, false));
      serverbound.register(UnsignedPlayerCommandPacket.class, UnsignedPlayerCommandPacket::new,
          map(0x04, MINECRAFT_1_20_5, false),
          map(0x05, MINECRAFT_1_21_2, false));
      serverbound.register(
          SessionPlayerChatPacket.class,
          SessionPlayerChatPacket::new,
          map(0x05, MINECRAFT_1_19_3, false),
          map(0x06, MINECRAFT_1_20_5, false),
          map(0x07, MINECRAFT_1_21_2, false));
      serverbound.register(
          ClientSettingsPacket.class,
          ClientSettingsPacket::new,
          map(0x15, MINECRAFT_1_7_2, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_14, false),
          map(0x07, MINECRAFT_1_19, false),
          map(0x08, MINECRAFT_1_19_1, false),
          map(0x07, MINECRAFT_1_19_3, false),
          map(0x08, MINECRAFT_1_19_4, false),
          map(0x09, MINECRAFT_1_20_2, false),
          map(0x0A, MINECRAFT_1_20_5, false),
          map(0x0C, MINECRAFT_1_21_2, false));
      serverbound.register(
          ServerboundCookieResponsePacket.class, ServerboundCookieResponsePacket::new,
          map(0x11, MINECRAFT_1_20_5, false),
          map(0x13, MINECRAFT_1_21_2, false));
      serverbound.register(
          PluginMessagePacket.class,
          PluginMessagePacket::new,
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
          map(0x0F, MINECRAFT_1_20_2, false),
          map(0x10, MINECRAFT_1_20_3, false),
          map(0x12, MINECRAFT_1_20_5, false),
          map(0x14, MINECRAFT_1_21_2, false));
      serverbound.register(
          KeepAlivePacket.class,
          KeepAlivePacket::new,
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
          map(0x14, MINECRAFT_1_20_2, false),
          map(0x15, MINECRAFT_1_20_3, false),
          map(0x18, MINECRAFT_1_20_5, false),
          map(0x1A, MINECRAFT_1_21_2, false));
      serverbound.register(
          ResourcePackResponsePacket.class,
          ResourcePackResponsePacket::new,
          map(0x19, MINECRAFT_1_8, false),
          map(0x16, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x1D, MINECRAFT_1_13, false),
          map(0x1F, MINECRAFT_1_14, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x21, MINECRAFT_1_16_2, false),
          map(0x23, MINECRAFT_1_19, false),
          map(0x24, MINECRAFT_1_19_1, false),
          map(0x27, MINECRAFT_1_20_2, false),
          map(0x28, MINECRAFT_1_20_3, false),
          map(0x2B, MINECRAFT_1_20_5, false),
          map(0x2D, MINECRAFT_1_21_2, false),
          map(0x2F, MINECRAFT_1_21_4, false));
      serverbound.register(
          FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE,
          map(0x0B, MINECRAFT_1_20_2, false),
          map(0x0C, MINECRAFT_1_20_5, false),
          map(0x0E, MINECRAFT_1_21_2, false));

      clientbound.register(
          BossBarPacket.class,
          BossBarPacket::new,
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0D, MINECRAFT_1_15, false),
          map(0x0C, MINECRAFT_1_16, false),
          map(0x0D, MINECRAFT_1_17, false),
          map(0x0A, MINECRAFT_1_19, false),
          map(0x0B, MINECRAFT_1_19_4, false),
          map(0x0A, MINECRAFT_1_20_2, false),
          map(0x09, MINECRAFT_1_21_5, false));
      clientbound.register(
          LegacyChatPacket.class,
          LegacyChatPacket::new,
          map(0x02, MINECRAFT_1_7_2, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0F, MINECRAFT_1_15, true),
          map(0x0E, MINECRAFT_1_16, true),
          map(0x0F, MINECRAFT_1_17, MINECRAFT_1_18_2, true));
      clientbound.register(TabCompleteResponsePacket.class, TabCompleteResponsePacket::new,
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
          map(0x10, MINECRAFT_1_20_2, false),
          map(0x0F, MINECRAFT_1_21_5, false));
      clientbound.register(
          AvailableCommandsPacket.class,
          AvailableCommandsPacket::new,
          map(0x11, MINECRAFT_1_13, false),
          map(0x12, MINECRAFT_1_15, false),
          map(0x11, MINECRAFT_1_16, false),
          map(0x10, MINECRAFT_1_16_2, false),
          map(0x12, MINECRAFT_1_17, false),
          map(0x0F, MINECRAFT_1_19, false),
          map(0x0E, MINECRAFT_1_19_3, false),
          map(0x10, MINECRAFT_1_19_4, false),
          map(0x11, MINECRAFT_1_20_2, false),
          map(0x10, MINECRAFT_1_21_5, false));
      clientbound.register(
          ClientboundCookieRequestPacket.class, ClientboundCookieRequestPacket::new,
          map(0x16, MINECRAFT_1_20_5, false),
          map(0x15, MINECRAFT_1_21_5, false));
      clientbound.register(
          PluginMessagePacket.class,
          PluginMessagePacket::new,
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
          map(0x18, MINECRAFT_1_20_2, false),
          map(0x19, MINECRAFT_1_20_5, false),
          map(0x18, MINECRAFT_1_21_5, false));
      clientbound.register(
          DisconnectPacket.class,
          () -> new DisconnectPacket(this),
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
          map(0x1B, MINECRAFT_1_20_2, false),
          map(0x1D, MINECRAFT_1_20_5, false),
          map(0x1C, MINECRAFT_1_21_5, false));
      clientbound.register(
          KeepAlivePacket.class,
          KeepAlivePacket::new,
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
          map(0x24, MINECRAFT_1_20_2, false),
          map(0x26, MINECRAFT_1_20_5, false),
          map(0x27, MINECRAFT_1_21_2, false),
          map(0x26, MINECRAFT_1_21_5, false));
      clientbound.register(
          JoinGamePacket.class,
          JoinGamePacket::new,
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
          map(0x29, MINECRAFT_1_20_2, false),
          map(0x2B, MINECRAFT_1_20_5, false),
          map(0x2C, MINECRAFT_1_21_2, false),
          map(0x2B, MINECRAFT_1_21_5, false));
      clientbound.register(
          RespawnPacket.class,
          RespawnPacket::new,
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
          map(0x43, MINECRAFT_1_20_2, true),
          map(0x45, MINECRAFT_1_20_3, true),
          map(0x47, MINECRAFT_1_20_5, true),
          map(0x4C, MINECRAFT_1_21_2, true),
          map(0x4B, MINECRAFT_1_21_5, true));
      clientbound.register(
          RemoveResourcePackPacket.class,
          RemoveResourcePackPacket::new,
          map(0x43, MINECRAFT_1_20_3, false),
          map(0x45, MINECRAFT_1_20_5, false),
          map(0x4A, MINECRAFT_1_21_2, false),
          map(0x49, MINECRAFT_1_21_5, false));
      clientbound.register(
          ResourcePackRequestPacket.class,
          ResourcePackRequestPacket::new,
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
          map(0x42, MINECRAFT_1_20_2, false),
          map(0x44, MINECRAFT_1_20_3, false),
          map(0x46, MINECRAFT_1_20_5, false),
          map(0x4B, MINECRAFT_1_21_2, false),
          map(0x4A, MINECRAFT_1_21_5, false));
      clientbound.register(
          HeaderAndFooterPacket.class,
          HeaderAndFooterPacket::new,
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
          map(0x68, MINECRAFT_1_20_2, true),
          map(0x6A, MINECRAFT_1_20_3, true),
          map(0x6D, MINECRAFT_1_20_5, true),
          map(0x74, MINECRAFT_1_21_2, true),
          map(0x73, MINECRAFT_1_21_5, true));
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
          map(0x5F, MINECRAFT_1_20_2, true),
          map(0x61, MINECRAFT_1_20_3, true),
          map(0x63, MINECRAFT_1_20_5, true),
          map(0x6A, MINECRAFT_1_21_2, true),
          map(0x69, MINECRAFT_1_21_5, true));
      clientbound.register(
          TitleTextPacket.class,
          TitleTextPacket::new,
          map(0x59, MINECRAFT_1_17, true),
          map(0x5A, MINECRAFT_1_18, true),
          map(0x5D, MINECRAFT_1_19_1, true),
          map(0x5B, MINECRAFT_1_19_3, true),
          map(0x5F, MINECRAFT_1_19_4, true),
          map(0x61, MINECRAFT_1_20_2, true),
          map(0x63, MINECRAFT_1_20_3, true),
          map(0x65, MINECRAFT_1_20_5, true),
          map(0x6C, MINECRAFT_1_21_2, true),
          map(0x6B, MINECRAFT_1_21_5, true));
      clientbound.register(
          TitleActionbarPacket.class,
          TitleActionbarPacket::new,
          map(0x41, MINECRAFT_1_17, true),
          map(0x40, MINECRAFT_1_19, true),
          map(0x43, MINECRAFT_1_19_1, true),
          map(0x42, MINECRAFT_1_19_3, true),
          map(0x46, MINECRAFT_1_19_4, true),
          map(0x48, MINECRAFT_1_20_2, true),
          map(0x4A, MINECRAFT_1_20_3, true),
          map(0x4C, MINECRAFT_1_20_5, true),
          map(0x51, MINECRAFT_1_21_2, true),
          map(0x50, MINECRAFT_1_21_5, true));
      clientbound.register(
          TitleTimesPacket.class,
          TitleTimesPacket::new,
          map(0x5A, MINECRAFT_1_17, true),
          map(0x5B, MINECRAFT_1_18, true),
          map(0x5E, MINECRAFT_1_19_1, true),
          map(0x5C, MINECRAFT_1_19_3, true),
          map(0x60, MINECRAFT_1_19_4, true),
          map(0x62, MINECRAFT_1_20_2, true),
          map(0x64, MINECRAFT_1_20_3, true),
          map(0x66, MINECRAFT_1_20_5, true),
          map(0x6D, MINECRAFT_1_21_2, true),
          map(0x6C, MINECRAFT_1_21_5, true));
      clientbound.register(
          TitleClearPacket.class,
          TitleClearPacket::new,
          map(0x10, MINECRAFT_1_17, true),
          map(0x0D, MINECRAFT_1_19, true),
          map(0x0C, MINECRAFT_1_19_3, true),
          map(0x0E, MINECRAFT_1_19_4, true),
          map(0x0F, MINECRAFT_1_20_2, true),
          map(0x0E, MINECRAFT_1_21_5, true));
      clientbound.register(
          LegacyPlayerListItemPacket.class,
          LegacyPlayerListItemPacket::new,
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
      clientbound.register(RemovePlayerInfoPacket.class, RemovePlayerInfoPacket::new,
          map(0x35, MINECRAFT_1_19_3, false),
          map(0x39, MINECRAFT_1_19_4, false),
          map(0x3B, MINECRAFT_1_20_2, false),
          map(0x3D, MINECRAFT_1_20_5, false),
          map(0x3F, MINECRAFT_1_21_2, false),
          map(0x3E, MINECRAFT_1_21_5, false));
      clientbound.register(
          UpsertPlayerInfoPacket.class,
          UpsertPlayerInfoPacket::new,
          map(0x36, MINECRAFT_1_19_3, false),
          map(0x3A, MINECRAFT_1_19_4, false),
          map(0x3C, MINECRAFT_1_20_2, false),
          map(0x3E, MINECRAFT_1_20_5, false),
          map(0x40, MINECRAFT_1_21_2, false),
          map(0x3F, MINECRAFT_1_21_5, false));
      clientbound.register(
          ClientboundStoreCookiePacket.class, ClientboundStoreCookiePacket::new,
          map(0x6B, MINECRAFT_1_20_5, false),
          map(0x72, MINECRAFT_1_21_2, false),
          map(0x71, MINECRAFT_1_21_5, false));
      clientbound.register(
          SystemChatPacket.class,
          SystemChatPacket::new,
          map(0x5F, MINECRAFT_1_19, true),
          map(0x62, MINECRAFT_1_19_1, true),
          map(0x60, MINECRAFT_1_19_3, true),
          map(0x64, MINECRAFT_1_19_4, true),
          map(0x67, MINECRAFT_1_20_2, true),
          map(0x69, MINECRAFT_1_20_3, true),
          map(0x6C, MINECRAFT_1_20_5, true),
          map(0x73, MINECRAFT_1_21_2, true),
          map(0x72, MINECRAFT_1_21_5, true));
      clientbound.register(
          PlayerChatCompletionPacket.class,
          PlayerChatCompletionPacket::new,
          map(0x15, MINECRAFT_1_19_1, true),
          map(0x14, MINECRAFT_1_19_3, true),
          map(0x16, MINECRAFT_1_19_4, true),
          map(0x17, MINECRAFT_1_20_2, true),
          map(0x18, MINECRAFT_1_20_5, true),
          map(0x17, MINECRAFT_1_21_5, true));
      clientbound.register(
          ServerDataPacket.class,
          ServerDataPacket::new,
          map(0x3F, MINECRAFT_1_19, false),
          map(0x42, MINECRAFT_1_19_1, false),
          map(0x41, MINECRAFT_1_19_3, false),
          map(0x45, MINECRAFT_1_19_4, false),
          map(0x47, MINECRAFT_1_20_2, false),
          map(0x49, MINECRAFT_1_20_3, false),
          map(0x4B, MINECRAFT_1_20_5, false),
          map(0x50, MINECRAFT_1_21_2, false),
          map(0x4F, MINECRAFT_1_21_5, false));
      clientbound.register(
          StartUpdatePacket.class,
          () -> StartUpdatePacket.INSTANCE,
          map(0x65, MINECRAFT_1_20_2, false),
          map(0x67, MINECRAFT_1_20_3, false),
          map(0x69, MINECRAFT_1_20_5, false),
          map(0x70, MINECRAFT_1_21_2, false),
          map(0x6F, MINECRAFT_1_21_5, false));
      clientbound.register(
          BundleDelimiterPacket.class,
          () -> BundleDelimiterPacket.INSTANCE,
          map(0x00, MINECRAFT_1_19_4, false));
      clientbound.register(
          TransferPacket.class,
          TransferPacket::new,
          map(0x73, MINECRAFT_1_20_5, false),
          map(0x7A, MINECRAFT_1_21_2, false));
      clientbound.register(
          ClientboundCustomReportDetailsPacket.class,
          ClientboundCustomReportDetailsPacket::new,
          map(0x7A, MINECRAFT_1_21, false),
          map(0x81, MINECRAFT_1_21_2, false));
      clientbound.register(
          ClientboundServerLinksPacket.class,
          ClientboundServerLinksPacket::new,
          map(0x7B, MINECRAFT_1_21, false),
          map(0x82, MINECRAFT_1_21_2, false));
    }
  },
  LOGIN {
    {
      serverbound.register(ServerLoginPacket.class,
          ServerLoginPacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(
          EncryptionResponsePacket.class, EncryptionResponsePacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
      serverbound.register(
          LoginPluginResponsePacket.class, LoginPluginResponsePacket::new,
          map(0x02, MINECRAFT_1_13, false));
      serverbound.register(
          LoginAcknowledgedPacket.class, LoginAcknowledgedPacket::new,
          map(0x03, MINECRAFT_1_20_2, false));
      serverbound.register(
          ServerboundCookieResponsePacket.class, ServerboundCookieResponsePacket::new,
          map(0x04, MINECRAFT_1_20_5, false));

      clientbound.register(
          DisconnectPacket.class, () -> new DisconnectPacket(this),
          map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(
          EncryptionRequestPacket.class, EncryptionRequestPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
      clientbound.register(
          ServerLoginSuccessPacket.class, ServerLoginSuccessPacket::new,
              map(0x02, MINECRAFT_1_7_2, false));
      clientbound.register(
          SetCompressionPacket.class, SetCompressionPacket::new,
          map(0x03, MINECRAFT_1_8, false));
      clientbound.register(
          LoginPluginMessagePacket.class,
          LoginPluginMessagePacket::new,
          map(0x04, MINECRAFT_1_13, false));
      clientbound.register(
          ClientboundCookieRequestPacket.class, ClientboundCookieRequestPacket::new,
          map(0x05, MINECRAFT_1_20_5, false));
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  public static final int TRANSFER_ID = 3;
  protected final PacketRegistry clientbound = new PacketRegistry(CLIENTBOUND, this);
  protected final PacketRegistry serverbound = new PacketRegistry(SERVERBOUND, this);

  public StateRegistry.PacketRegistry.ProtocolRegistry getProtocolRegistry(Direction direction,
      ProtocolVersion version) {
    return (direction == SERVERBOUND ? serverbound : clientbound).getProtocolRegistry(version);
  }

  /**
   * Gets the API representation of the StateRegistry.
   *
   * @return the API representation
   */
  public ProtocolState toProtocolState() {
    return switch (this) {
      case HANDSHAKE -> ProtocolState.HANDSHAKE;
      case STATUS -> ProtocolState.STATUS;
      case LOGIN -> ProtocolState.LOGIN;
      case CONFIG -> ProtocolState.CONFIGURATION;
      case PLAY -> ProtocolState.PLAY;
    };
  }

  /**
   * Packet registry.
   */
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
          if (from.greaterThan(lastValid)) {
            throw new IllegalArgumentException(
                "Last mapping version cannot be higher than highest mapping version");
          }
        }
        ProtocolVersion to = current == next ? lastValid != null
            ? lastValid : getLast(SUPPORTED_VERSIONS) : next.protocolVersion;

        ProtocolVersion lastInList = lastValid != null ? lastValid : getLast(SUPPORTED_VERSIONS);

        if (from.noLessThan(to) && from != lastInList) {
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

    /**
     * Protocol registry.
     */
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

  /**
   * Packet mapping.
   */
  public static final class PacketMapping {

    private final int id;
    private final ProtocolVersion protocolVersion;
    private final boolean encodeOnly;
    private final @Nullable ProtocolVersion lastValidProtocolVersion;

    PacketMapping(int id, ProtocolVersion protocolVersion,
                  @Nullable ProtocolVersion lastValidProtocolVersion,
                  boolean packetDecoding) {
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