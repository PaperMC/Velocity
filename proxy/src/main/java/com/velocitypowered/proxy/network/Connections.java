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

package com.velocitypowered.proxy.network;

/**
 * Constants used for the pipeline.
 */
public class Connections {

  public static final String CIPHER_DECODER = "cipher-decoder";
  public static final String CIPHER_ENCODER = "cipher-encoder";
  public static final String COMPRESSION_DECODER = "compression-decoder";
  public static final String COMPRESSION_ENCODER = "compression-encoder";
  public static final String FLOW_HANDLER = "flow-handler";
  public static final String FRAME_DECODER = "frame-decoder";
  public static final String FRAME_ENCODER = "frame-encoder";
  public static final String HANDLER = "handler";
  public static final String LEGACY_PING_DECODER = "legacy-ping-decoder";
  public static final String LEGACY_PING_ENCODER = "legacy-ping-encoder";
  public static final String MINECRAFT_DECODER = "minecraft-decoder";
  public static final String MINECRAFT_ENCODER = "minecraft-encoder";
  public static final String READ_TIMEOUT = "read-timeout";
  public static final String PLAY_PACKET_QUEUE = "play-packet-queue";

  private Connections() {
    throw new AssertionError();
  }
}
