/*
 * Copyright (C) 2025 Velocity Contributors
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

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_17;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_17_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_18;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_18_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_3;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_19_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_3;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_20_5;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_5;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_6;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_21_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_7_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_4;

import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.BundleDelimiterPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundCookieRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundSoundEntityPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundStopSoundPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundStoreCookiePacket;
import com.velocitypowered.proxy.protocol.packet.DialogClearPacket;
import com.velocitypowered.proxy.protocol.packet.DialogShowPacket;
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
import com.velocitypowered.proxy.protocol.packet.ServerboundCustomClickActionPacket;
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
import com.velocitypowered.proxy.protocol.packet.config.CodeOfConductAcceptPacket;
import com.velocitypowered.proxy.protocol.packet.config.CodeOfConductPacket;
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
import com.velocitypowered.proxy.protocol.registry.MultiVersionPacketRegistry;
import com.velocitypowered.proxy.protocol.registry.MultiVersionPacketRegistry.VersionRange;
import com.velocitypowered.proxy.protocol.registry.ProtocolToPacketRegistry;
import com.velocitypowered.proxy.protocol.registry.SimplePacketRegistry;

/**
 * Defines the packet mappings for all protocol states and versions supported by Velocity.
 */
public class ProtocolStates {

  public static final ProtocolToPacketRegistry HANDSHAKE_CLIENTBOUND;
  public static final ProtocolToPacketRegistry HANDSHAKE_SERVERBOUND;
  public static final ProtocolToPacketRegistry STATUS_SERVERBOUND;
  public static final ProtocolToPacketRegistry STATUS_CLIENTBOUND;
  public static final ProtocolToPacketRegistry CONFIG_SERVERBOUND;
  public static final ProtocolToPacketRegistry CONFIG_CLIENTBOUND;
  public static final ProtocolToPacketRegistry PLAY_SERVERBOUND;
  public static final ProtocolToPacketRegistry PLAY_CLIENTBOUND;
  public static final ProtocolToPacketRegistry LOGIN_SERVERBOUND;
  public static final ProtocolToPacketRegistry LOGIN_CLIENTBOUND;

  static {
    HANDSHAKE_CLIENTBOUND = new SimplePacketRegistry(Direction.CLIENTBOUND);

    SimplePacketRegistry handshakeServerbound = new SimplePacketRegistry(Direction.SERVERBOUND);
    handshakeServerbound.register(0x00, HandshakePacket.class, HandshakePacket.Codec.INSTANCE);
    HANDSHAKE_SERVERBOUND = handshakeServerbound;

    SimplePacketRegistry statusServerbound = new SimplePacketRegistry(Direction.SERVERBOUND);
    statusServerbound.register(0x00,
        StatusRequestPacket.class, StatusRequestPacket.Codec.INSTANCE);
    statusServerbound.register(0x01, StatusPingPacket.class, StatusPingPacket.Codec.INSTANCE);
    STATUS_SERVERBOUND = statusServerbound;

    SimplePacketRegistry statusClientbound = new SimplePacketRegistry(Direction.CLIENTBOUND);
    statusClientbound.register(0x00,
        StatusResponsePacket.class, StatusResponsePacket.Codec.INSTANCE);
    statusClientbound.register(0x01, StatusPingPacket.class, StatusPingPacket.Codec.INSTANCE);
    STATUS_CLIENTBOUND = statusClientbound;

    CONFIG_SERVERBOUND = MultiVersionPacketRegistry.builder(Direction.SERVERBOUND)
        .register(ClientSettingsPacket.class, ClientSettingsPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, 0x00))
        .register(ServerboundCookieResponsePacket.class,
            ServerboundCookieResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x01))
        .register(PluginMessagePacket.class, PluginMessagePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x01),
            VersionRange.of(MINECRAFT_1_20_5, 0x02))
        .register(FinishedUpdatePacket.class, FinishedUpdatePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x02),
            VersionRange.of(MINECRAFT_1_20_5, 0x03))
        .register(KeepAlivePacket.class, KeepAlivePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x03),
            VersionRange.of(MINECRAFT_1_20_5, 0x04))
        .register(PingIdentifyPacket.class, PingIdentifyPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x04),
            VersionRange.of(MINECRAFT_1_20_5, 0x05))
        .register(ResourcePackResponsePacket.class, ResourcePackResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x05),
            VersionRange.of(MINECRAFT_1_20_5, 0x06))
        .register(KnownPacksPacket.class, KnownPacksPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x07))
        .register(ServerboundCustomClickActionPacket.class,
            ServerboundCustomClickActionPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21_6, 0x08))
        .register(CodeOfConductAcceptPacket.class, CodeOfConductAcceptPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21_9, 0x09))
        .build();

    CONFIG_CLIENTBOUND = MultiVersionPacketRegistry.builder(Direction.CLIENTBOUND)
        .register(ClientboundCookieRequestPacket.class,
            ClientboundCookieRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x00))
        .register(PluginMessagePacket.class, PluginMessagePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x00),
            VersionRange.of(MINECRAFT_1_20_5, 0x01))
        .register(DisconnectPacket.class, new DisconnectPacket.Codec(StateRegistry.CONFIG),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x01),
            VersionRange.of(MINECRAFT_1_20_5, 0x02))
        .register(FinishedUpdatePacket.class, FinishedUpdatePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x02),
            VersionRange.of(MINECRAFT_1_20_5, 0x03))
        .register(KeepAlivePacket.class, KeepAlivePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x03),
            VersionRange.of(MINECRAFT_1_20_5, 0x04))
        .register(PingIdentifyPacket.class, PingIdentifyPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x04),
            VersionRange.of(MINECRAFT_1_20_5, 0x05))
        .register(RegistrySyncPacket.class, RegistrySyncPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x05),
            VersionRange.of(MINECRAFT_1_20_5, 0x07))
        .register(RemoveResourcePackPacket.class, RemoveResourcePackPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x06),
            VersionRange.of(MINECRAFT_1_20_5, 0x08))
        .register(ResourcePackRequestPacket.class, ResourcePackRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x06),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x07),
            VersionRange.of(MINECRAFT_1_20_5, 0x09))
        .register(ClientboundStoreCookiePacket.class, ClientboundStoreCookiePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x0A))
        .register(TransferPacket.class, TransferPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x0B))
        .register(ActiveFeaturesPacket.class, ActiveFeaturesPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x07),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x08),
            VersionRange.of(MINECRAFT_1_20_5, 0x0C))
        .register(TagsUpdatePacket.class, TagsUpdatePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x08),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x09),
            VersionRange.of(MINECRAFT_1_20_5, 0x0D))
        .register(KnownPacksPacket.class, KnownPacksPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x0E))
        .register(ClientboundCustomReportDetailsPacket.class,
            ClientboundCustomReportDetailsPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21, 0x0F))
        .register(ClientboundServerLinksPacket.class, ClientboundServerLinksPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21, 0x10))
        .register(DialogClearPacket.class, DialogClearPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21_6, 0x11))
        .register(DialogShowPacket.class, new DialogShowPacket.Codec(StateRegistry.CONFIG),
            VersionRange.of(MINECRAFT_1_21_6, 0x12))
        .register(CodeOfConductPacket.class, CodeOfConductPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21_9, 0x13))
        .build();

    LOGIN_SERVERBOUND = MultiVersionPacketRegistry.builder(Direction.SERVERBOUND)
        .register(ServerLoginPacket.class, ServerLoginPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, 0x00))
        .register(EncryptionResponsePacket.class, EncryptionResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, 0x01))
        .register(LoginPluginResponsePacket.class, LoginPluginResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_13, 0x02))
        .register(LoginAcknowledgedPacket.class, LoginAcknowledgedPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, 0x03))
        .register(ServerboundCookieResponsePacket.class,
            ServerboundCookieResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x04))
        .build();

    LOGIN_CLIENTBOUND = MultiVersionPacketRegistry.builder(Direction.CLIENTBOUND)
        .register(DisconnectPacket.class, new DisconnectPacket.Codec(StateRegistry.LOGIN),
            VersionRange.of(MINECRAFT_1_7_2, 0x00))
        .register(EncryptionRequestPacket.class, EncryptionRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, 0x01))
        .register(ServerLoginSuccessPacket.class, ServerLoginSuccessPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, 0x02))
        .register(SetCompressionPacket.class, SetCompressionPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_8, 0x03))
        .register(LoginPluginMessagePacket.class, LoginPluginMessagePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_13, 0x04))
        .register(ClientboundCookieRequestPacket.class,
            ClientboundCookieRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, 0x05))
        .build();

    // PLAY state is much larger, will continue below
    PLAY_SERVERBOUND = buildPlayServerbound();
    PLAY_CLIENTBOUND = buildPlayClientbound();
  }

  private static ProtocolToPacketRegistry buildPlayServerbound() {
    return MultiVersionPacketRegistry.builder(Direction.SERVERBOUND)
        .register(TabCompleteRequestPacket.class, TabCompleteRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x14),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x01),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x02),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x01),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x05),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_18_2, 0x06),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x08),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x09),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x08),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x09),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x0A),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x0B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x0D),
            VersionRange.of(MINECRAFT_1_21_6, 0x0E))
        .register(LegacyChatPacket.class, LegacyChatPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x01),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x02),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x03),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_13, 0x02),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_18_2, 0x03))
        .register(ChatAcknowledgementPacket.class, ChatAcknowledgementPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_21, 0x03),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x04),
            VersionRange.of(MINECRAFT_1_21_6, 0x05))
        .register(KeyedPlayerCommandPacket.class, KeyedPlayerCommandPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x03),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x04))
        .register(KeyedPlayerChatPacket.class, KeyedPlayerChatPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x04),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x05))
        .register(SessionPlayerCommandPacket.class, SessionPlayerCommandPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_20_3, 0x04),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x05),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x06),
            VersionRange.of(MINECRAFT_1_21_6, 0x07))
        .register(UnsignedPlayerCommandPacket.class, UnsignedPlayerCommandPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x04),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x05),
            VersionRange.of(MINECRAFT_1_21_6, 0x06))
        .register(SessionPlayerChatPacket.class, SessionPlayerChatPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_20_3, 0x05),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x06),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x07),
            VersionRange.of(MINECRAFT_1_21_6, 0x08))
        .register(ClientSettingsPacket.class, ClientSettingsPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x15),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x04),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x05),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_13, 0x04),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_18_2, 0x05),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x07),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x08),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x07),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x08),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x09),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x0A),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x0C),
            VersionRange.of(MINECRAFT_1_21_6, 0x0D))
        .register(ServerboundCookieResponsePacket.class,
            ServerboundCookieResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x11),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x13),
            VersionRange.of(MINECRAFT_1_21_6, 0x14))
        .register(PluginMessagePacket.class, PluginMessagePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x17),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x09),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x0A),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x09),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x0A),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_16_4, 0x0B),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x0A),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x0C),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x0D),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x0C),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x0D),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x0F),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x10),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x12),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x14),
            VersionRange.of(MINECRAFT_1_21_6, 0x15))
        .register(KeepAlivePacket.class, KeepAlivePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x00),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x0B),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x0C),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x0B),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x0E),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_15_2, 0x0F),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_4, 0x10),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x0F),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x11),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x12),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x11),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x12),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x14),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x15),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x18),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x1A),
            VersionRange.of(MINECRAFT_1_21_6, 0x1B))
        .register(ResourcePackResponsePacket.class, ResourcePackResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_8, 0x19),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x16),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x18),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x1D),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_15_2, 0x1F),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x20),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_18_2, 0x21),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x23),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_20, 0x24),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x27),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x28),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x2B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_2, 0x2D),
            VersionRange.of(MINECRAFT_1_21_4, MINECRAFT_1_21_4, 0x2F),
            VersionRange.of(MINECRAFT_1_21_6, 0x30))
        .register(FinishedUpdatePacket.class, FinishedUpdatePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x0B),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x0C),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x0E),
            VersionRange.of(MINECRAFT_1_21_6, 0x0F))
        .build();
  }

  private static ProtocolToPacketRegistry buildPlayClientbound() {
    return MultiVersionPacketRegistry.builder(Direction.CLIENTBOUND)
        .register(BossBarPacket.class, BossBarPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_14, 0x0C),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x0D),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_4, 0x0C),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x0D),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19_3, 0x0A),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x0B),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_21_4, 0x0A),
            VersionRange.of(MINECRAFT_1_21_5, 0x09))
        .register(LegacyChatPacket.class, LegacyChatPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x02),
            VersionRange.encodeOnly(MINECRAFT_1_9, MINECRAFT_1_12, 0x0F),
            VersionRange.encodeOnly(MINECRAFT_1_13, MINECRAFT_1_13, 0x0E),
            VersionRange.encodeOnly(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x0F),
            VersionRange.encodeOnly(MINECRAFT_1_16, MINECRAFT_1_16_4, 0x0E),
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x0F))
        .register(TabCompleteResponsePacket.class, TabCompleteResponsePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x3A),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x0E),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_14, 0x10),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x11),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x10),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x0F),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x11),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19_1, 0x0E),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x0D),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x0F),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_21_4, 0x10),
            VersionRange.of(MINECRAFT_1_21_5, 0x0F))
        .register(AvailableCommandsPacket.class, AvailableCommandsPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_14, 0x11),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x12),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x11),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x10),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x12),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19_1, 0x0F),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x0E),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x10),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_21_4, 0x11),
            VersionRange.of(MINECRAFT_1_21_5, 0x10))
        .register(ClientboundCookieRequestPacket.class,
            ClientboundCookieRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21_4, 0x16),
            VersionRange.of(MINECRAFT_1_21_5, 0x15))
        .register(ClientboundSoundEntityPacket.class, ClientboundSoundEntityPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5D),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x61),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x63),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x65),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x67),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x6E),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x6D),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x72))
        .register(ClientboundStopSoundPacket.class, ClientboundStopSoundPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5F),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x63),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x66),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x68),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x6A),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x71),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x70),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x75))
        .register(PluginMessagePacket.class, PluginMessagePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x3F),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x18),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x19),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_14, 0x18),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x19),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x18),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x17),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x18),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x15),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x16),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x15),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x17),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x18),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21_4, 0x19),
            VersionRange.of(MINECRAFT_1_21_5, 0x18))
        .register(DisconnectPacket.class, new DisconnectPacket.Codec(StateRegistry.PLAY),
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x40),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x1A),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x1B),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_14, 0x1A),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x1B),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x1A),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x19),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x1A),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x17),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x19),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x17),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x1A),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x1B),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21_4, 0x1D),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x1C),
            VersionRange.of(MINECRAFT_1_21_9, 0x20))
        .register(KeepAlivePacket.class, KeepAlivePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x00),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x1F),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x21),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x20),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x21),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x20),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x1F),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x21),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x1E),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x20),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x1F),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x23),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x24),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x26),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x27),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x26),
            VersionRange.of(MINECRAFT_1_21_9, 0x2B))
        .register(JoinGamePacket.class, JoinGamePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x01),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x23),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_14, 0x25),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x26),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x25),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x24),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x26),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x23),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x25),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x24),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x28),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x29),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x2B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x2C),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x2B),
            VersionRange.of(MINECRAFT_1_21_9, 0x30))
        .register(RespawnPacket.class, RespawnPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x07),
            VersionRange.encodeOnly(MINECRAFT_1_9, MINECRAFT_1_11, 0x33),
            VersionRange.encodeOnly(MINECRAFT_1_12, MINECRAFT_1_12, 0x34),
            VersionRange.encodeOnly(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x35),
            VersionRange.encodeOnly(MINECRAFT_1_13, MINECRAFT_1_13, 0x38),
            VersionRange.encodeOnly(MINECRAFT_1_14, MINECRAFT_1_14, 0x3A),
            VersionRange.encodeOnly(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x3B),
            VersionRange.encodeOnly(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x3A),
            VersionRange.encodeOnly(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x39),
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x3D),
            VersionRange.encodeOnly(MINECRAFT_1_19, MINECRAFT_1_19, 0x3B),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x3E),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x3D),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x41),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x43),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x45),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x47),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x4C),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x4B),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x50))
        .register(RemoveResourcePackPacket.class, RemoveResourcePackPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x43),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x45),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x4A),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x49),
            VersionRange.of(MINECRAFT_1_21_9, 0x4E))
        .register(ResourcePackRequestPacket.class, ResourcePackRequestPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_8, 0x48),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_11, 0x32),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x33),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x34),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x37),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_14, 0x39),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x3A),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x39),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x38),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x3C),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x3A),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x3D),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x3C),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x40),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x42),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x44),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x46),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x4B),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x4A),
            VersionRange.of(MINECRAFT_1_21_9, 0x4F))
        .register(HeaderAndFooterPacket.class, HeaderAndFooterPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_8, MINECRAFT_1_8, 0x47),
            VersionRange.encodeOnly(MINECRAFT_1_9, MINECRAFT_1_9_1, 0x48),
            VersionRange.encodeOnly(MINECRAFT_1_9_4, MINECRAFT_1_11, 0x47),
            VersionRange.encodeOnly(MINECRAFT_1_12, MINECRAFT_1_12, 0x49),
            VersionRange.encodeOnly(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x4A),
            VersionRange.encodeOnly(MINECRAFT_1_13, MINECRAFT_1_13, 0x4E),
            VersionRange.encodeOnly(MINECRAFT_1_14, MINECRAFT_1_14, 0x53),
            VersionRange.encodeOnly(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x54),
            VersionRange.encodeOnly(MINECRAFT_1_16, MINECRAFT_1_16_4, 0x53),
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x5E),
            VersionRange.encodeOnly(MINECRAFT_1_18, MINECRAFT_1_18_2, 0x5F),
            VersionRange.encodeOnly(MINECRAFT_1_19, MINECRAFT_1_19, 0x60),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x63),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x61),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x65),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x68),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x6A),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x6D),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x74),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x73),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x78))
        .register(LegacyTitlePacket.class, LegacyTitlePacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_8, MINECRAFT_1_11, 0x45),
            VersionRange.encodeOnly(MINECRAFT_1_12, MINECRAFT_1_12, 0x47),
            VersionRange.encodeOnly(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x48),
            VersionRange.encodeOnly(MINECRAFT_1_13, MINECRAFT_1_13, 0x4B),
            VersionRange.encodeOnly(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x4F),
            VersionRange.encodeOnly(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x50),
            VersionRange.encodeOnly(MINECRAFT_1_16, MINECRAFT_1_16_4, 0x4F))
        .register(TitleSubtitlePacket.class, TitleSubtitlePacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x57),
            VersionRange.encodeOnly(MINECRAFT_1_18, MINECRAFT_1_18_2, 0x58),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x5B),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x59),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x5D),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x5F),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x61),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x63),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x6A),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x69),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x6E))
        .register(TitleTextPacket.class, TitleTextPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x59),
            VersionRange.encodeOnly(MINECRAFT_1_18, MINECRAFT_1_18_2, 0x5A),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x5D),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5B),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x5F),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x61),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x63),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x65),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x6C),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x6B),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x70))
        .register(TitleActionbarPacket.class, TitleActionbarPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x41),
            VersionRange.encodeOnly(MINECRAFT_1_19, MINECRAFT_1_19, 0x40),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x43),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x42),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x46),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x48),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x4A),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x4C),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x51),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x50),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x55))
        .register(TitleTimesPacket.class, TitleTimesPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x5A),
            VersionRange.encodeOnly(MINECRAFT_1_18, MINECRAFT_1_18_2, 0x5B),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x5E),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x5C),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x60),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x62),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x64),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x66),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x6D),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x6C),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x71))
        .register(TitleClearPacket.class, TitleClearPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x10),
            VersionRange.encodeOnly(MINECRAFT_1_19, MINECRAFT_1_19_1, 0x0D),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x0C),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x0E),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_21_4, 0x0F),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, 0x0E))
        .register(LegacyPlayerListItemPacket.class, LegacyPlayerListItemPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_7_2, MINECRAFT_1_8, 0x38),
            VersionRange.of(MINECRAFT_1_9, MINECRAFT_1_12, 0x2D),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_1, 0x2E),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_13, 0x30),
            VersionRange.of(MINECRAFT_1_14, MINECRAFT_1_14, 0x33),
            VersionRange.of(MINECRAFT_1_15, MINECRAFT_1_15_2, 0x34),
            VersionRange.of(MINECRAFT_1_16, MINECRAFT_1_16_1, 0x33),
            VersionRange.of(MINECRAFT_1_16_2, MINECRAFT_1_16_4, 0x32),
            VersionRange.of(MINECRAFT_1_17, MINECRAFT_1_18_2, 0x36),
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x34),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x37))
        .register(RemovePlayerInfoPacket.class, RemovePlayerInfoPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x35),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x39),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x3B),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x3D),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x3F),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x3E),
            VersionRange.of(MINECRAFT_1_21_9, 0x43))
        .register(UpsertPlayerInfoPacket.class, UpsertPlayerInfoPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x36),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x3A),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x3C),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x3E),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x40),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x3F),
            VersionRange.of(MINECRAFT_1_21_9, 0x44))
        .register(ClientboundStoreCookiePacket.class, ClientboundStoreCookiePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x6B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x72),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x71),
            VersionRange.of(MINECRAFT_1_21_9, 0x76))
        .register(SystemChatPacket.class, SystemChatPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_19, MINECRAFT_1_19, 0x5F),
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x62),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x60),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x64),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x67),
            VersionRange.encodeOnly(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x69),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x6C),
            VersionRange.encodeOnly(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x73),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x72),
            VersionRange.encodeOnly(MINECRAFT_1_21_9, 0x77))
        .register(PlayerChatCompletionPacket.class, PlayerChatCompletionPacket.Codec.INSTANCE,
            VersionRange.encodeOnly(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x15),
            VersionRange.encodeOnly(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x14),
            VersionRange.encodeOnly(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x16),
            VersionRange.encodeOnly(MINECRAFT_1_20_2, MINECRAFT_1_20_3, 0x17),
            VersionRange.encodeOnly(MINECRAFT_1_20_5, MINECRAFT_1_21_4, 0x18),
            VersionRange.encodeOnly(MINECRAFT_1_21_5, 0x17))
        .register(ServerDataPacket.class, ServerDataPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19, MINECRAFT_1_19, 0x3F),
            VersionRange.of(MINECRAFT_1_19_1, MINECRAFT_1_19_1, 0x42),
            VersionRange.of(MINECRAFT_1_19_3, MINECRAFT_1_19_3, 0x41),
            VersionRange.of(MINECRAFT_1_19_4, MINECRAFT_1_20, 0x45),
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x47),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x49),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x4B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x50),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x4F),
            VersionRange.of(MINECRAFT_1_21_9, 0x54))
        .register(StartUpdatePacket.class, StartUpdatePacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_2, MINECRAFT_1_20_2, 0x65),
            VersionRange.of(MINECRAFT_1_20_3, MINECRAFT_1_20_3, 0x67),
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x69),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_4, 0x70),
            VersionRange.of(MINECRAFT_1_21_5, MINECRAFT_1_21_6, 0x6F),
            VersionRange.of(MINECRAFT_1_21_9, 0x74))
        .register(BundleDelimiterPacket.class, BundleDelimiterPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_19_4, 0x00))
        .register(TransferPacket.class, TransferPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_20_5, MINECRAFT_1_21, 0x73),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_6, 0x7A),
            VersionRange.of(MINECRAFT_1_21_9, 0x7F))
        .register(ClientboundCustomReportDetailsPacket.class,
            ClientboundCustomReportDetailsPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21, MINECRAFT_1_21, 0x7A),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_6, 0x81),
            VersionRange.of(MINECRAFT_1_21_9, 0x86))
        .register(ClientboundServerLinksPacket.class, ClientboundServerLinksPacket.Codec.INSTANCE,
            VersionRange.of(MINECRAFT_1_21, MINECRAFT_1_21, 0x7B),
            VersionRange.of(MINECRAFT_1_21_2, MINECRAFT_1_21_6, 0x82),
            VersionRange.of(MINECRAFT_1_21_9, 0x87))
        .build();
  }

  public static ProtocolToPacketRegistry handshake(Direction direction) {
    return direction == Direction.SERVERBOUND ? HANDSHAKE_SERVERBOUND : HANDSHAKE_CLIENTBOUND;
  }

  public static ProtocolToPacketRegistry status(Direction direction) {
    return direction == Direction.SERVERBOUND ? STATUS_SERVERBOUND : STATUS_CLIENTBOUND;
  }

  public static ProtocolToPacketRegistry login(Direction direction) {
    return direction == Direction.SERVERBOUND ? LOGIN_SERVERBOUND : LOGIN_CLIENTBOUND;
  }

  public static ProtocolToPacketRegistry configuration(Direction direction) {
    return direction == Direction.SERVERBOUND ? CONFIG_SERVERBOUND : CONFIG_CLIENTBOUND;
  }

  public static ProtocolToPacketRegistry play(Direction direction) {
    return direction == Direction.SERVERBOUND ? PLAY_SERVERBOUND : PLAY_CLIENTBOUND;
  }

  /**
   * Looks up the packet registry for the specified state and direction.
   *
   * @param registry the protocol state
   * @param direction the packet direction
   * @return the packet registry for the specified state and direction
   */
  public static ProtocolToPacketRegistry lookup(StateRegistry registry, Direction direction) {
    return switch (registry) {
      case HANDSHAKE -> handshake(direction);
      case STATUS -> status(direction);
      case LOGIN -> login(direction);
      case CONFIG -> configuration(direction);
      case PLAY -> play(direction);
    };
  }
}
