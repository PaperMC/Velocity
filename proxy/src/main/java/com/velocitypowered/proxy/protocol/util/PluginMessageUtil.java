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

package com.velocitypowered.proxy.protocol.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilities for handling plugin messages.
 */
public final class PluginMessageUtil {

  private static final String BRAND_CHANNEL_LEGACY = "MC|Brand";
  private static final String BRAND_CHANNEL = "minecraft:brand";
  private static final String REGISTER_CHANNEL_LEGACY = "REGISTER";
  private static final String REGISTER_CHANNEL = "minecraft:register";
  private static final String UNREGISTER_CHANNEL_LEGACY = "UNREGISTER";
  private static final String UNREGISTER_CHANNEL = "minecraft:unregister";

  private PluginMessageUtil() {
    throw new AssertionError();
  }

  /**
   * Determines whether or not this is a brand plugin message. This is shown on the client.
   *
   * @param message the plugin message
   * @return whether or not this is a brand plugin message
   */
  public static boolean isMcBrand(PluginMessagePacket message) {
    checkNotNull(message, "message");
    return message.getChannel().equals(BRAND_CHANNEL_LEGACY) || message.getChannel()
        .equals(BRAND_CHANNEL);
  }

  /**
   * Determines whether or not this plugin message is being used to register plugin channels.
   *
   * @param message the plugin message
   * @return whether we are registering plugin channels or not
   */
  public static boolean isRegister(PluginMessagePacket message) {
    checkNotNull(message, "message");
    return message.getChannel().equals(REGISTER_CHANNEL_LEGACY) || message.getChannel()
        .equals(REGISTER_CHANNEL);
  }

  /**
   * Determines whether or not this plugin message is being used to unregister plugin channels.
   *
   * @param message the plugin message
   * @return whether we are unregistering plugin channels or not
   */
  public static boolean isUnregister(PluginMessagePacket message) {
    checkNotNull(message, "message");
    return message.getChannel().equals(UNREGISTER_CHANNEL_LEGACY) || message.getChannel()
        .equals(UNREGISTER_CHANNEL);
  }

  private static final QuietDecoderException ILLEGAL_CHANNEL = new QuietDecoderException("Illegal channel");

  /**
   * Fetches all the channels in a register or unregister plugin message.
   *
   * @param existingChannels the number of channels already registered
   * @param message the message to get the channels from
   * @return the channels, as an immutable list
   */
  public static List<ChannelIdentifier> getChannels(int existingChannels,
                                                    PluginMessagePacket message,
                                                    ProtocolVersion protocolVersion) {
    checkNotNull(message, "message");
    checkArgument(isRegister(message) || isUnregister(message), "Unknown channel type %s",
        message.getChannel());
    if (!message.content().isReadable()) {
      // If we try to split this, we will get an one-element array with the empty string, which
      // has caused issues with 1.13+ compatibility. Just return an empty list.
      return ImmutableList.of();
    }
    String payload = message.content().toString(StandardCharsets.UTF_8);
    checkArgument(payload.length() <= Short.MAX_VALUE, "payload too long: %s", payload.length());
    String[] channels = payload.split("\0");
    checkArgument(existingChannels + channels.length <= ConnectedPlayer.MAX_CLIENTSIDE_PLUGIN_CHANNELS,
        "too many channels: %s + %s > %s", existingChannels, channels.length, ConnectedPlayer.MAX_CLIENTSIDE_PLUGIN_CHANNELS);
    ImmutableList.Builder<ChannelIdentifier> channelIdentifiers = ImmutableList.builderWithExpectedSize(channels.length);
    try {
      for (String channel : channels) {
        if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_13)) {
          channelIdentifiers.add(MinecraftChannelIdentifier.from(channel));
        } else {
          channelIdentifiers.add(new LegacyChannelIdentifier(channel));
        }
      }
    } catch (IllegalArgumentException e) {
      if (MinecraftDecoder.DEBUG) {
        throw e;
      } else {
        throw ILLEGAL_CHANNEL;
      }
    }
    return channelIdentifiers.build();
  }

  /**
   * Constructs a channel (un)register packet.
   *
   * @param protocolVersion the client/server's protocol version
   * @param channels        the channels to register
   * @return the plugin message to send
   */
  public static PluginMessagePacket constructChannelsPacket(ProtocolVersion protocolVersion,
                                                            Collection<ChannelIdentifier> channels) {
    checkNotNull(channels, "channels");
    checkArgument(!channels.isEmpty(), "no channels specified");
    String channelName = protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_13)
        ? REGISTER_CHANNEL : REGISTER_CHANNEL_LEGACY;
    ByteBuf contents = Unpooled.buffer();
    contents.writeCharSequence(joinChannels(channels), StandardCharsets.UTF_8);
    return new PluginMessagePacket(channelName, contents);
  }

  private static String joinChannels(Collection<ChannelIdentifier> channels) {
    checkNotNull(channels, "channels");
    checkArgument(!channels.isEmpty(), "no channels specified");
    StringBuilder sb = new StringBuilder();
    Iterator<ChannelIdentifier> iterator = channels.iterator();
    while (iterator.hasNext()) {
      ChannelIdentifier channel = iterator.next();
      sb.append(channel.getId());
      if (iterator.hasNext()) {
        sb.append('\0');
      }
    }
    return sb.toString();
  }

  /**
   * Rewrites the brand message to indicate the presence of Velocity.
   *
   * @param message the plugin message
   * @param version the proxy version
   * @return the rewritten plugin message
   */
  public static PluginMessagePacket rewriteMinecraftBrand(PluginMessagePacket message,
                                                          ProxyVersion version,
                                                          ProtocolVersion protocolVersion) {
    checkNotNull(message, "message");
    checkNotNull(version, "version");
    checkArgument(isMcBrand(message), "message is not a brand plugin message");

    String currentBrand = readBrandMessage(message.content());
    String rewrittenBrand = String.format("%s (%s)", currentBrand, version.getName());

    ByteBuf rewrittenBuf = Unpooled.buffer();
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeString(rewrittenBuf, rewrittenBrand);
    } else {
      rewrittenBuf.writeCharSequence(rewrittenBrand, StandardCharsets.UTF_8);
    }

    return new PluginMessagePacket(message.getChannel(), rewrittenBuf);
  }

  /**
   * Some clients (mostly poorly-implemented bots) do not send validly-formed brand messages. In
   * order to accommodate their broken behavior, we'll first try to read in the 1.8 format, and if
   * that fails, treat it as a 1.7-format message (which has no prefixed length). (The message
   * Velocity sends will be in the correct format depending on the protocol.)
   *
   * @param content the brand packet
   * @return the client brand
   */
  public static String readBrandMessage(ByteBuf content) {
    try {
      return ProtocolUtils.readString(content.slice());
    } catch (Exception e) {
      return ProtocolUtils.readStringWithoutLength(content.slice());
    }
  }

  private static final Pattern INVALID_IDENTIFIER_REGEX = Pattern.compile("[^a-z0-9\\-_]*");

  /**
   * Transform a plugin message channel from a "legacy" (less than 1.13) form to a modern one.
   *
   * @param name the existing name
   * @return the new name
   */
  public static String transformLegacyToModernChannel(String name) {
    checkNotNull(name, "name");

    if (name.indexOf(':') != -1) {
      // Probably valid. We won't check this for now and go on faith.
      return name;
    }

    // Before falling into the fallback, explicitly rewrite certain messages.
    return switch (name) {
      case REGISTER_CHANNEL_LEGACY -> REGISTER_CHANNEL;
      case UNREGISTER_CHANNEL_LEGACY -> UNREGISTER_CHANNEL;
      case BRAND_CHANNEL_LEGACY -> BRAND_CHANNEL;
      // This is a special historical case we are compelled to support for the benefit of
      // BungeeQuack.
      case "BungeeCord" -> "bungeecord:main";
      default -> {
        // This is very likely a legacy name, so transform it. Velocity uses the same scheme as
        // BungeeCord does to transform channels, but also removes clearly invalid characters as
        // well.
        final String lower = name.toLowerCase(Locale.ROOT);
        yield "legacy:" + INVALID_IDENTIFIER_REGEX.matcher(lower).replaceAll("");
      }
    };
  }
}
