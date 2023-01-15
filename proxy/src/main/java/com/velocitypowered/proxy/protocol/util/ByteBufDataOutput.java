/*
 * Copyright (C) 2019-2021 Velocity Contributors
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

import com.google.common.io.ByteArrayDataOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A {@link ByteArrayDataOutput} equivalent to {@link ByteBufDataInput}.
 */
public class ByteBufDataOutput extends OutputStream implements ByteArrayDataOutput {

  private final ByteBuf buf;
  private final DataOutputStream utf8out;

  public ByteBufDataOutput(ByteBuf buf) {
    this.buf = buf;
    this.utf8out = new DataOutputStream(this);
  }

  @Override
  public byte[] toByteArray() {
    return ByteBufUtil.getBytes(buf);
  }

  @Override
  public void write(int b) {
    buf.writeByte(b);
  }

  @Override
  public void write(byte[] b) {
    buf.writeBytes(b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    buf.writeBytes(b, off, len);
  }

  @Override
  public void writeBoolean(boolean v) {
    buf.writeBoolean(v);
  }

  @Override
  public void writeByte(int v) {
    buf.writeByte(v);
  }

  @Override
  public void writeShort(int v) {
    buf.writeShort(v);
  }

  @Override
  public void writeChar(int v) {
    buf.writeChar(v);
  }

  @Override
  public void writeInt(int v) {
    buf.writeInt(v);
  }

  @Override
  public void writeLong(long v) {
    buf.writeLong(v);
  }

  @Override
  public void writeFloat(float v) {
    buf.writeFloat(v);
  }

  @Override
  public void writeDouble(double v) {
    buf.writeDouble(v);
  }

  @Override
  public void writeBytes(String s) {
    buf.writeCharSequence(s, StandardCharsets.US_ASCII);
  }

  @Override
  public void writeChars(String s) {
    for (char c : s.toCharArray()) {
      buf.writeChar(c);
    }
  }

  @Override
  public void writeUTF(String s) {
    try {
      this.utf8out.writeUTF(s);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void close() {
  }
}
