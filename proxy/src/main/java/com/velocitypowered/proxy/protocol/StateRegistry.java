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
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registry of all Minecraft protocol states and the packets for each state.
 */
public enum StateRegistry {

  HANDSHAKE {
    {
      serverbound(HandshakePacket.class, HandshakePacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
      });
    }
  },
  STATUS {
    {
      serverbound(StatusRequestPacket.class, () -> StatusRequestPacket.INSTANCE, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
      });
      serverbound(StatusPingPacket.class, StatusPingPacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
      });

      clientbound(StatusResponsePacket.class, StatusResponsePacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
      });
      clientbound(StatusPingPacket.class, StatusPingPacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
      });
    }
  },
  CONFIG {
    {
      serverbound(ClientSettingsPacket.class, ClientSettingsPacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_20_2);
      });
      serverbound(ServerboundCookieResponsePacket.class, ServerboundCookieResponsePacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_20_5);
      });
      serverbound(PluginMessagePacket.class, PluginMessagePacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_20_2);
        m.readWrite(0x02, MINECRAFT_1_20_5);
      });
      serverbound(FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE, m -> {
        m.readWrite(0x02, MINECRAFT_1_20_2);
        m.readWrite(0x03, MINECRAFT_1_20_5);
      });
      serverbound(KeepAlivePacket.class, KeepAlivePacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_20_2);
        m.readWrite(0x04, MINECRAFT_1_20_5);
      });
      serverbound(PingIdentifyPacket.class, PingIdentifyPacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_20_2);
        m.readWrite(0x05, MINECRAFT_1_20_5);
      });
      serverbound(ResourcePackResponsePacket.class, ResourcePackResponsePacket::new, m -> {
        m.readWrite(0x05, MINECRAFT_1_20_2);
        m.readWrite(0x06, MINECRAFT_1_20_5);
      });
      serverbound(KnownPacksPacket.class, KnownPacksPacket::new, m -> {
        m.readWrite(0x07, MINECRAFT_1_20_5);
      });

      clientbound(ClientboundCookieRequestPacket.class, ClientboundCookieRequestPacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_20_5);
      });
      clientbound(PluginMessagePacket.class, PluginMessagePacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_20_2);
        m.readWrite(0x01, MINECRAFT_1_20_5);
      });
      clientbound(DisconnectPacket.class, () -> new DisconnectPacket(this), m -> {
        m.readWrite(0x01, MINECRAFT_1_20_2);
        m.readWrite(0x02, MINECRAFT_1_20_5);
      });
      clientbound(FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE, m -> {
        m.readWrite(0x02, MINECRAFT_1_20_2);
        m.readWrite(0x03, MINECRAFT_1_20_5);
      });
      clientbound(KeepAlivePacket.class, KeepAlivePacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_20_2);
        m.readWrite(0x04, MINECRAFT_1_20_5);
      });
      clientbound(PingIdentifyPacket.class, PingIdentifyPacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_20_2);
        m.readWrite(0x05, MINECRAFT_1_20_5);
      });
      clientbound(RegistrySyncPacket.class, RegistrySyncPacket::new, m -> {
        m.readWrite(0x05, MINECRAFT_1_20_2);
        m.readWrite(0x07, MINECRAFT_1_20_5);
      });
      clientbound(RemoveResourcePackPacket.class, RemoveResourcePackPacket::new, m -> {
        m.readWrite(0x06, MINECRAFT_1_20_3);
        m.readWrite(0x08, MINECRAFT_1_20_5);
      });
      clientbound(ResourcePackRequestPacket.class, ResourcePackRequestPacket::new, m -> {
        m.readWrite(0x06, MINECRAFT_1_20_2);
        m.readWrite(0x07, MINECRAFT_1_20_3);
        m.readWrite(0x09, MINECRAFT_1_20_5);
      });
      clientbound(TransferPacket.class, TransferPacket::new, m -> {
        m.readWrite(0x0B, MINECRAFT_1_20_5);
      });
      clientbound(ActiveFeaturesPacket.class, ActiveFeaturesPacket::new, m -> {
        m.readWrite(0x07, MINECRAFT_1_20_2);
        m.readWrite(0x08, MINECRAFT_1_20_3);
        m.readWrite(0x0C, MINECRAFT_1_20_5);
      });
      clientbound(TagsUpdatePacket.class, TagsUpdatePacket::new, m -> {
        m.readWrite(0x08, MINECRAFT_1_20_2);
        m.readWrite(0x09, MINECRAFT_1_20_3);
        m.readWrite(0x0D, MINECRAFT_1_20_5);
      });
      clientbound(KnownPacksPacket.class, KnownPacksPacket::new, m -> {
        m.readWrite(0x0E, MINECRAFT_1_20_5);
      });
    }
  },
  PLAY {
    {
      serverbound.fallback = false;
      clientbound.fallback = false;

      serverbound(TabCompleteRequestPacket.class, TabCompleteRequestPacket::new, m -> {
        m.readWrite(0x14, MINECRAFT_1_7_2);
        m.readWrite(0x01, MINECRAFT_1_9);
        m.readWrite(0x02, MINECRAFT_1_12);
        m.readWrite(0x01, MINECRAFT_1_12_1);
        m.readWrite(0x05, MINECRAFT_1_13);
        m.readWrite(0x06, MINECRAFT_1_14);
        m.readWrite(0x08, MINECRAFT_1_19);
        m.readWrite(0x09, MINECRAFT_1_19_1);
        m.readWrite(0x08, MINECRAFT_1_19_3);
        m.readWrite(0x09, MINECRAFT_1_19_4);
        m.readWrite(0x0A, MINECRAFT_1_20_2);
        m.readWrite(0x0B, MINECRAFT_1_20_5);
      });
      serverbound(LegacyChatPacket.class, LegacyChatPacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
        m.readWrite(0x02, MINECRAFT_1_9);
        m.readWrite(0x03, MINECRAFT_1_12);
        m.readWrite(0x02, MINECRAFT_1_12_1);
        m.readWrite(0x03, MINECRAFT_1_14, MINECRAFT_1_18_2);
      });
      serverbound(ChatAcknowledgementPacket.class, ChatAcknowledgementPacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_19_3);
      });
      serverbound(KeyedPlayerCommandPacket.class, KeyedPlayerCommandPacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_19);
        m.readWrite(0x04, MINECRAFT_1_19_1, MINECRAFT_1_19_1);
      });
      serverbound(KeyedPlayerChatPacket.class, KeyedPlayerChatPacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_19);
        m.readWrite(0x05, MINECRAFT_1_19_1, MINECRAFT_1_19_1);
      });
      serverbound(SessionPlayerCommandPacket.class, SessionPlayerCommandPacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_19_3);
        m.readWrite(0x05, MINECRAFT_1_20_5);
      });
      serverbound(UnsignedPlayerCommandPacket.class, UnsignedPlayerCommandPacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_20_5);
      });
      serverbound(SessionPlayerChatPacket.class, SessionPlayerChatPacket::new, m -> {
        m.readWrite(0x05, MINECRAFT_1_19_3);
        m.readWrite(0x06, MINECRAFT_1_20_5);
      });
      serverbound(ClientSettingsPacket.class, ClientSettingsPacket::new, m -> {
        m.readWrite(0x15, MINECRAFT_1_7_2);
        m.readWrite(0x04, MINECRAFT_1_9);
        m.readWrite(0x05, MINECRAFT_1_12);
        m.readWrite(0x04, MINECRAFT_1_12_1);
        m.readWrite(0x05, MINECRAFT_1_14);
        m.readWrite(0x07, MINECRAFT_1_19);
        m.readWrite(0x08, MINECRAFT_1_19_1);
        m.readWrite(0x07, MINECRAFT_1_19_3);
        m.readWrite(0x08, MINECRAFT_1_19_4);
        m.readWrite(0x09, MINECRAFT_1_20_2);
        m.readWrite(0x0A, MINECRAFT_1_20_5);
      });
      serverbound(PluginMessagePacket.class, PluginMessagePacket::new, m -> {
        m.readWrite(0x17, MINECRAFT_1_7_2);
        m.readWrite(0x09, MINECRAFT_1_9);
        m.readWrite(0x0A, MINECRAFT_1_12);
        m.readWrite(0x09, MINECRAFT_1_12_1);
        m.readWrite(0x0A, MINECRAFT_1_13);
        m.readWrite(0x0B, MINECRAFT_1_14);
        m.readWrite(0x0A, MINECRAFT_1_17);
        m.readWrite(0x0C, MINECRAFT_1_19);
        m.readWrite(0x0D, MINECRAFT_1_19_1);
        m.readWrite(0x0C, MINECRAFT_1_19_3);
        m.readWrite(0x0D, MINECRAFT_1_19_4);
        m.readWrite(0x0F, MINECRAFT_1_20_2);
        m.readWrite(0x10, MINECRAFT_1_20_3);
        m.readWrite(0x12, MINECRAFT_1_20_5);
      });
      serverbound(KeepAlivePacket.class, KeepAlivePacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
        m.readWrite(0x0B, MINECRAFT_1_9);
        m.readWrite(0x0C, MINECRAFT_1_12);
        m.readWrite(0x0B, MINECRAFT_1_12_1);
        m.readWrite(0x0E, MINECRAFT_1_13);
        m.readWrite(0x0F, MINECRAFT_1_14);
        m.readWrite(0x10, MINECRAFT_1_16);
        m.readWrite(0x0F, MINECRAFT_1_17);
        m.readWrite(0x11, MINECRAFT_1_19);
        m.readWrite(0x12, MINECRAFT_1_19_1);
        m.readWrite(0x11, MINECRAFT_1_19_3);
        m.readWrite(0x12, MINECRAFT_1_19_4);
        m.readWrite(0x14, MINECRAFT_1_20_2);
        m.readWrite(0x15, MINECRAFT_1_20_3);
        m.readWrite(0x18, MINECRAFT_1_20_5);
      });
      serverbound(ResourcePackResponsePacket.class, ResourcePackResponsePacket::new, m -> {
        m.readWrite(0x19, MINECRAFT_1_8);
        m.readWrite(0x16, MINECRAFT_1_9);
        m.readWrite(0x18, MINECRAFT_1_12);
        m.readWrite(0x1D, MINECRAFT_1_13);
        m.readWrite(0x1F, MINECRAFT_1_14);
        m.readWrite(0x20, MINECRAFT_1_16);
        m.readWrite(0x21, MINECRAFT_1_16_2);
        m.readWrite(0x23, MINECRAFT_1_19);
        m.readWrite(0x24, MINECRAFT_1_19_1);
        m.readWrite(0x27, MINECRAFT_1_20_2);
        m.readWrite(0x28, MINECRAFT_1_20_3);
        m.readWrite(0x2B, MINECRAFT_1_20_5);
      });
      serverbound(FinishedUpdatePacket.class, () -> FinishedUpdatePacket.INSTANCE, m -> {
        m.readWrite(0x0B, MINECRAFT_1_20_2);
        m.readWrite(0x0C, MINECRAFT_1_20_5);
      });

      clientbound(BossBarPacket.class, BossBarPacket::new, m -> {
        m.readWrite(0x0C, MINECRAFT_1_9);
        m.readWrite(0x0D, MINECRAFT_1_15);
        m.readWrite(0x0C, MINECRAFT_1_16);
        m.readWrite(0x0D, MINECRAFT_1_17);
        m.readWrite(0x0A, MINECRAFT_1_19);
        m.readWrite(0x0B, MINECRAFT_1_19_4);
        m.readWrite(0x0A, MINECRAFT_1_20_2);
      });
      clientbound(LegacyChatPacket.class, LegacyChatPacket::new, m -> {
        m.writeOnly(0x02, MINECRAFT_1_7_2);
        m.writeOnly(0x0F, MINECRAFT_1_9);
        m.writeOnly(0x0E, MINECRAFT_1_13);
        m.writeOnly(0x0F, MINECRAFT_1_15);
        m.writeOnly(0x0E, MINECRAFT_1_16);
        m.writeOnly(0x0F, MINECRAFT_1_17, MINECRAFT_1_18_2);
      });
      clientbound(TabCompleteResponsePacket.class, TabCompleteResponsePacket::new, m -> {
        m.readWrite(0x3A, MINECRAFT_1_7_2);
        m.readWrite(0x0E, MINECRAFT_1_9);
        m.readWrite(0x10, MINECRAFT_1_13);
        m.readWrite(0x11, MINECRAFT_1_15);
        m.readWrite(0x10, MINECRAFT_1_16);
        m.readWrite(0x0F, MINECRAFT_1_16_2);
        m.readWrite(0x11, MINECRAFT_1_17);
        m.readWrite(0x0E, MINECRAFT_1_19);
        m.readWrite(0x0D, MINECRAFT_1_19_3);
        m.readWrite(0x0F, MINECRAFT_1_19_4);
        m.readWrite(0x10, MINECRAFT_1_20_2);
      });
      clientbound(AvailableCommandsPacket.class, AvailableCommandsPacket::new, m -> {
        m.readWrite(0x11, MINECRAFT_1_13);
        m.readWrite(0x12, MINECRAFT_1_15);
        m.readWrite(0x11, MINECRAFT_1_16);
        m.readWrite(0x10, MINECRAFT_1_16_2);
        m.readWrite(0x12, MINECRAFT_1_17);
        m.readWrite(0x0F, MINECRAFT_1_19);
        m.readWrite(0x0E, MINECRAFT_1_19_3);
        m.readWrite(0x10, MINECRAFT_1_19_4);
        m.readWrite(0x11, MINECRAFT_1_20_2);
      });
      clientbound(PluginMessagePacket.class, PluginMessagePacket::new, m -> {
        m.readWrite(0x3F, MINECRAFT_1_7_2);
        m.readWrite(0x18, MINECRAFT_1_9);
        m.readWrite(0x19, MINECRAFT_1_13);
        m.readWrite(0x18, MINECRAFT_1_14);
        m.readWrite(0x19, MINECRAFT_1_15);
        m.readWrite(0x18, MINECRAFT_1_16);
        m.readWrite(0x17, MINECRAFT_1_16_2);
        m.readWrite(0x18, MINECRAFT_1_17);
        m.readWrite(0x15, MINECRAFT_1_19);
        m.readWrite(0x16, MINECRAFT_1_19_1);
        m.readWrite(0x15, MINECRAFT_1_19_3);
        m.readWrite(0x17, MINECRAFT_1_19_4);
        m.readWrite(0x18, MINECRAFT_1_20_2);
        m.readWrite(0x19, MINECRAFT_1_20_5);
      });
      clientbound(DisconnectPacket.class, () -> new DisconnectPacket(this), m -> {
        m.readWrite(0x40, MINECRAFT_1_7_2);
        m.readWrite(0x1A, MINECRAFT_1_9);
        m.readWrite(0x1B, MINECRAFT_1_13);
        m.readWrite(0x1A, MINECRAFT_1_14);
        m.readWrite(0x1B, MINECRAFT_1_15);
        m.readWrite(0x1A, MINECRAFT_1_16);
        m.readWrite(0x19, MINECRAFT_1_16_2);
        m.readWrite(0x1A, MINECRAFT_1_17);
        m.readWrite(0x17, MINECRAFT_1_19);
        m.readWrite(0x19, MINECRAFT_1_19_1);
        m.readWrite(0x17, MINECRAFT_1_19_3);
        m.readWrite(0x1A, MINECRAFT_1_19_4);
        m.readWrite(0x1B, MINECRAFT_1_20_2);
        m.readWrite(0x1D, MINECRAFT_1_20_5);
      });
      clientbound(KeepAlivePacket.class, KeepAlivePacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
        m.readWrite(0x1F, MINECRAFT_1_9);
        m.readWrite(0x21, MINECRAFT_1_13);
        m.readWrite(0x20, MINECRAFT_1_14);
        m.readWrite(0x21, MINECRAFT_1_15);
        m.readWrite(0x20, MINECRAFT_1_16);
        m.readWrite(0x1F, MINECRAFT_1_16_2);
        m.readWrite(0x21, MINECRAFT_1_17);
        m.readWrite(0x1E, MINECRAFT_1_19);
        m.readWrite(0x20, MINECRAFT_1_19_1);
        m.readWrite(0x1F, MINECRAFT_1_19_3);
        m.readWrite(0x23, MINECRAFT_1_19_4);
        m.readWrite(0x24, MINECRAFT_1_20_2);
        m.readWrite(0x26, MINECRAFT_1_20_5);
      });
      clientbound(JoinGamePacket.class, JoinGamePacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
        m.readWrite(0x23, MINECRAFT_1_9);
        m.readWrite(0x25, MINECRAFT_1_13);
        m.readWrite(0x25, MINECRAFT_1_14);
        m.readWrite(0x26, MINECRAFT_1_15);
        m.readWrite(0x25, MINECRAFT_1_16);
        m.readWrite(0x24, MINECRAFT_1_16_2);
        m.readWrite(0x26, MINECRAFT_1_17);
        m.readWrite(0x23, MINECRAFT_1_19);
        m.readWrite(0x25, MINECRAFT_1_19_1);
        m.readWrite(0x24, MINECRAFT_1_19_3);
        m.readWrite(0x28, MINECRAFT_1_19_4);
        m.readWrite(0x29, MINECRAFT_1_20_2);
        m.readWrite(0x2B, MINECRAFT_1_20_5);
      });
      clientbound(RespawnPacket.class, RespawnPacket::new, m -> {
        m.writeOnly(0x07, MINECRAFT_1_7_2);
        m.writeOnly(0x33, MINECRAFT_1_9);
        m.writeOnly(0x34, MINECRAFT_1_12);
        m.writeOnly(0x35, MINECRAFT_1_12_1);
        m.writeOnly(0x38, MINECRAFT_1_13);
        m.writeOnly(0x3A, MINECRAFT_1_14);
        m.writeOnly(0x3B, MINECRAFT_1_15);
        m.writeOnly(0x3A, MINECRAFT_1_16);
        m.writeOnly(0x39, MINECRAFT_1_16_2);
        m.writeOnly(0x3D, MINECRAFT_1_17);
        m.writeOnly(0x3B, MINECRAFT_1_19);
        m.writeOnly(0x3E, MINECRAFT_1_19_1);
        m.writeOnly(0x3D, MINECRAFT_1_19_3);
        m.writeOnly(0x41, MINECRAFT_1_19_4);
        m.writeOnly(0x43, MINECRAFT_1_20_2);
        m.writeOnly(0x45, MINECRAFT_1_20_3);
        m.writeOnly(0x47, MINECRAFT_1_20_5);
      });
      clientbound(RemoveResourcePackPacket.class, RemoveResourcePackPacket::new, m -> {
        m.readWrite(0x43, MINECRAFT_1_20_3);
        m.readWrite(0x45, MINECRAFT_1_20_5);
      });
      clientbound(ResourcePackRequestPacket.class, ResourcePackRequestPacket::new, m -> {
        m.readWrite(0x48, MINECRAFT_1_8);
        m.readWrite(0x32, MINECRAFT_1_9);
        m.readWrite(0x33, MINECRAFT_1_12);
        m.readWrite(0x34, MINECRAFT_1_12_1);
        m.readWrite(0x37, MINECRAFT_1_13);
        m.readWrite(0x39, MINECRAFT_1_14);
        m.readWrite(0x3A, MINECRAFT_1_15);
        m.readWrite(0x39, MINECRAFT_1_16);
        m.readWrite(0x38, MINECRAFT_1_16_2);
        m.readWrite(0x3C, MINECRAFT_1_17);
        m.readWrite(0x3A, MINECRAFT_1_19);
        m.readWrite(0x3D, MINECRAFT_1_19_1);
        m.readWrite(0x3C, MINECRAFT_1_19_3);
        m.readWrite(0x40, MINECRAFT_1_19_4);
        m.readWrite(0x42, MINECRAFT_1_20_2);
        m.readWrite(0x44, MINECRAFT_1_20_3);
        m.readWrite(0x46, MINECRAFT_1_20_5);
      });
      clientbound(HeaderAndFooterPacket.class, HeaderAndFooterPacket::new, m -> {
        m.writeOnly(0x47, MINECRAFT_1_8);
        m.writeOnly(0x48, MINECRAFT_1_9);
        m.writeOnly(0x47, MINECRAFT_1_9_4);
        m.writeOnly(0x49, MINECRAFT_1_12);
        m.writeOnly(0x4A, MINECRAFT_1_12_1);
        m.writeOnly(0x4E, MINECRAFT_1_13);
        m.writeOnly(0x53, MINECRAFT_1_14);
        m.writeOnly(0x54, MINECRAFT_1_15);
        m.writeOnly(0x53, MINECRAFT_1_16);
        m.writeOnly(0x5E, MINECRAFT_1_17);
        m.writeOnly(0x5F, MINECRAFT_1_18);
        m.writeOnly(0x60, MINECRAFT_1_19);
        m.writeOnly(0x63, MINECRAFT_1_19_1);
        m.writeOnly(0x61, MINECRAFT_1_19_3);
        m.writeOnly(0x65, MINECRAFT_1_19_4);
        m.writeOnly(0x68, MINECRAFT_1_20_2);
        m.writeOnly(0x6A, MINECRAFT_1_20_3);
        m.writeOnly(0x6D, MINECRAFT_1_20_5);
      });
      clientbound(LegacyTitlePacket.class, LegacyTitlePacket::new, m -> {
        m.writeOnly(0x45, MINECRAFT_1_8);
        m.writeOnly(0x45, MINECRAFT_1_9);
        m.writeOnly(0x47, MINECRAFT_1_12);
        m.writeOnly(0x48, MINECRAFT_1_12_1);
        m.writeOnly(0x4B, MINECRAFT_1_13);
        m.writeOnly(0x4F, MINECRAFT_1_14);
        m.writeOnly(0x50, MINECRAFT_1_15);
        m.writeOnly(0x4F, MINECRAFT_1_16, MINECRAFT_1_16_4);
      });
      clientbound(TitleSubtitlePacket.class, TitleSubtitlePacket::new, m -> {
        m.writeOnly(0x57, MINECRAFT_1_17);
        m.writeOnly(0x58, MINECRAFT_1_18);
        m.writeOnly(0x5B, MINECRAFT_1_19_1);
        m.writeOnly(0x59, MINECRAFT_1_19_3);
        m.writeOnly(0x5D, MINECRAFT_1_19_4);
        m.writeOnly(0x5F, MINECRAFT_1_20_2);
        m.writeOnly(0x61, MINECRAFT_1_20_3);
        m.writeOnly(0x63, MINECRAFT_1_20_5);
      });
      clientbound(TitleTextPacket.class, TitleTextPacket::new, m -> {
        m.writeOnly(0x59, MINECRAFT_1_17);
        m.writeOnly(0x5A, MINECRAFT_1_18);
        m.writeOnly(0x5D, MINECRAFT_1_19_1);
        m.writeOnly(0x5B, MINECRAFT_1_19_3);
        m.writeOnly(0x5F, MINECRAFT_1_19_4);
        m.writeOnly(0x61, MINECRAFT_1_20_2);
        m.writeOnly(0x63, MINECRAFT_1_20_3);
        m.writeOnly(0x65, MINECRAFT_1_20_5);
      });
      clientbound(TitleActionbarPacket.class, TitleActionbarPacket::new, m -> {
        m.writeOnly(0x41, MINECRAFT_1_17);
        m.writeOnly(0x40, MINECRAFT_1_19);
        m.writeOnly(0x43, MINECRAFT_1_19_1);
        m.writeOnly(0x42, MINECRAFT_1_19_3);
        m.writeOnly(0x46, MINECRAFT_1_19_4);
        m.writeOnly(0x48, MINECRAFT_1_20_2);
        m.writeOnly(0x4A, MINECRAFT_1_20_3);
        m.writeOnly(0x4C, MINECRAFT_1_20_5);
      });
      clientbound(TitleTimesPacket.class, TitleTimesPacket::new, m -> {
        m.writeOnly(0x5A, MINECRAFT_1_17);
        m.writeOnly(0x5B, MINECRAFT_1_18);
        m.writeOnly(0x5E, MINECRAFT_1_19_1);
        m.writeOnly(0x5C, MINECRAFT_1_19_3);
        m.writeOnly(0x60, MINECRAFT_1_19_4);
        m.writeOnly(0x62, MINECRAFT_1_20_2);
        m.writeOnly(0x64, MINECRAFT_1_20_3);
        m.writeOnly(0x66, MINECRAFT_1_20_5);
      });
      clientbound(TitleClearPacket.class, TitleClearPacket::new, m -> {
        m.writeOnly(0x10, MINECRAFT_1_17);
        m.writeOnly(0x0D, MINECRAFT_1_19);
        m.writeOnly(0x0C, MINECRAFT_1_19_3);
        m.writeOnly(0x0E, MINECRAFT_1_19_4);
        m.writeOnly(0x0F, MINECRAFT_1_20_2);
      });
      clientbound(LegacyPlayerListItemPacket.class, LegacyPlayerListItemPacket::new, m -> {
        m.readWrite(0x38, MINECRAFT_1_7_2);
        m.readWrite(0x2D, MINECRAFT_1_9);
        m.readWrite(0x2E, MINECRAFT_1_12_1);
        m.readWrite(0x30, MINECRAFT_1_13);
        m.readWrite(0x33, MINECRAFT_1_14);
        m.readWrite(0x34, MINECRAFT_1_15);
        m.readWrite(0x33, MINECRAFT_1_16);
        m.readWrite(0x32, MINECRAFT_1_16_2);
        m.readWrite(0x36, MINECRAFT_1_17);
        m.readWrite(0x34, MINECRAFT_1_19);
        m.readWrite(0x37, MINECRAFT_1_19_1, MINECRAFT_1_19_1);
      });
      clientbound(RemovePlayerInfoPacket.class, RemovePlayerInfoPacket::new, m -> {
        m.readWrite(0x35, MINECRAFT_1_19_3);
        m.readWrite(0x39, MINECRAFT_1_19_4);
        m.readWrite(0x3B, MINECRAFT_1_20_2);
        m.readWrite(0x3D, MINECRAFT_1_20_5);
      });
      clientbound(UpsertPlayerInfoPacket.class, UpsertPlayerInfoPacket::new, m -> {
        m.readWrite(0x36, MINECRAFT_1_19_3);
        m.readWrite(0x3A, MINECRAFT_1_19_4);
        m.readWrite(0x3C, MINECRAFT_1_20_2);
        m.readWrite(0x3E, MINECRAFT_1_20_5);
      });
      clientbound(SystemChatPacket.class, SystemChatPacket::new, m -> {
        m.writeOnly(0x5F, MINECRAFT_1_19);
        m.writeOnly(0x62, MINECRAFT_1_19_1);
        m.writeOnly(0x60, MINECRAFT_1_19_3);
        m.writeOnly(0x64, MINECRAFT_1_19_4);
        m.writeOnly(0x67, MINECRAFT_1_20_2);
        m.writeOnly(0x69, MINECRAFT_1_20_3);
        m.writeOnly(0x6C, MINECRAFT_1_20_5);
      });
      clientbound(PlayerChatCompletionPacket.class, PlayerChatCompletionPacket::new, m -> {
        m.writeOnly(0x15, MINECRAFT_1_19_1);
        m.writeOnly(0x14, MINECRAFT_1_19_3);
        m.writeOnly(0x16, MINECRAFT_1_19_4);
        m.writeOnly(0x17, MINECRAFT_1_20_2);
        m.writeOnly(0x18, MINECRAFT_1_20_5);
      });
      clientbound(ServerDataPacket.class, ServerDataPacket::new, m -> {
        m.readWrite(0x3F, MINECRAFT_1_19);
        m.readWrite(0x42, MINECRAFT_1_19_1);
        m.readWrite(0x41, MINECRAFT_1_19_3);
        m.readWrite(0x45, MINECRAFT_1_19_4);
        m.readWrite(0x47, MINECRAFT_1_20_2);
        m.readWrite(0x49, MINECRAFT_1_20_3);
        m.readWrite(0x4B, MINECRAFT_1_20_5);
      });
      clientbound(StartUpdatePacket.class, () -> StartUpdatePacket.INSTANCE, m -> {
        m.readWrite(0x65, MINECRAFT_1_20_2);
        m.readWrite(0x67, MINECRAFT_1_20_3);
        m.readWrite(0x69, MINECRAFT_1_20_5);
      });
      clientbound(BundleDelimiterPacket.class, () -> BundleDelimiterPacket.INSTANCE, m -> {
        m.readWrite(0x00, MINECRAFT_1_19_4);
      });
      clientbound(TransferPacket.class, TransferPacket::new, m -> {
        m.readWrite(0x73, MINECRAFT_1_20_5);
      });
    }
  },
  LOGIN {
    {
      serverbound(ServerLoginPacket.class, ServerLoginPacket::new, m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
      });
      serverbound(EncryptionResponsePacket.class, EncryptionResponsePacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
      });
      serverbound(LoginPluginResponsePacket.class, LoginPluginResponsePacket::new, m -> {
        m.readWrite(0x02, MINECRAFT_1_13);
      });
      serverbound(LoginAcknowledgedPacket.class, LoginAcknowledgedPacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_20_2);
      });

      clientbound(DisconnectPacket.class, () -> new DisconnectPacket(this), m -> {
        m.readWrite(0x00, MINECRAFT_1_7_2);
      });
      clientbound(EncryptionRequestPacket.class, EncryptionRequestPacket::new, m -> {
        m.readWrite(0x01, MINECRAFT_1_7_2);
      });
      clientbound(ServerLoginSuccessPacket.class, ServerLoginSuccessPacket::new, m -> {
        m.readWrite(0x02, MINECRAFT_1_7_2);
      });
      clientbound(SetCompressionPacket.class, SetCompressionPacket::new, m -> {
        m.readWrite(0x03, MINECRAFT_1_8);
      });
      clientbound(LoginPluginMessagePacket.class, LoginPluginMessagePacket::new, m -> {
        m.readWrite(0x04, MINECRAFT_1_13);
      });
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  public static final int TRANSFER_ID = 3;
  protected final PacketRegistry clientbound = new PacketRegistry(CLIENTBOUND, this);
  protected final PacketRegistry serverbound = new PacketRegistry(SERVERBOUND, this);

  <P extends MinecraftPacket>  void clientbound(Class<P> clazz, Supplier<P> factory, Consumer<PacketMapper> mapper) {
    this.clientbound.register(clazz, factory, mapper);
  }

  <P extends MinecraftPacket>  void serverbound(Class<P> clazz, Supplier<P> factory, Consumer<PacketMapper> mapper) {
    this.serverbound.register(clazz, factory, mapper);
  }

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

    <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> factory, Consumer<PacketMapper> mapper) {
      final List<PacketMapping> mappings = new ArrayList<>();
      mapper.accept(new PacketMapper() {
        @Override
        public void readWrite(final int id, final ProtocolVersion from, final @Nullable ProtocolVersion to) {
          mappings.add(new PacketMapping(id, from, to, false));
        }

        @Override
        public void writeOnly(final int id, final ProtocolVersion from, final @Nullable ProtocolVersion to) {
          mappings.add(new PacketMapping(id, from, to, true));
        }
      });

      final int size = mappings.size();
      if (size == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (int i = 0; i < size; i++) {
        PacketMapping current = mappings.get(i);
        PacketMapping next = (i + 1 < size) ? mappings.get(i + 1) : current;

        ProtocolVersion from = current.from();
        ProtocolVersion lastValid = current.to();
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
            ? lastValid : getLast(SUPPORTED_VERSIONS) : next.from();

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
                "Unknown protocol version " + current.from());
          }

          if (registry.packetIdToSupplier.containsKey(current.id())) {
            throw new IllegalArgumentException(
                "Can not register class "
                    + clazz.getSimpleName()
                    + " with id "
                    + current.id()
                    + " for "
                    + registry.version
                    + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(
                clazz.getSimpleName() + " is already registered for version " + registry.version);
          }

          if (!current.writeOnly()) {
            registry.packetIdToSupplier.put(current.id(), factory);
          }
          registry.packetClassToId.put(clazz, current.id());
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

}