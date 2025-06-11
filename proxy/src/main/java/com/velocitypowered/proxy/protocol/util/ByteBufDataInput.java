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

import com.google.common.io.ByteArrayDataInput;
import io.netty.buffer.ByteBuf;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A wrapper around {@link io.netty.buffer.ByteBuf} that implements the exception-free
 * {@link ByteArrayDataInput} interface from Guava.
 */
public class ByteBufDataInput implements ByteArrayDataInput {

  private final ByteBuf in;

  /**
   * Creates a new ByteBufDataInput instance. The ByteBufDataInput simply "borrows" the ByteBuf
   * while it is in use.
   *
   * @param buf the buffer to read from
   */
  public ByteBufDataInput(ByteBuf buf) {
    this.in = buf;
  }

  public ByteBuf unwrap() {
    return in;
  }

  @Override
  public void readFully(byte[] b) {
    in.readBytes(b);
  }

  @Override
  public void readFully(byte[] b, int off, int len) {
    in.readBytes(b, off, len);
  }

  @Override
  public int skipBytes(int n) {
    in.skipBytes(n);
    return n;
  }

  @Override
  public boolean readBoolean() {
    return in.readBoolean();
  }

  @Override
  public byte readByte() {
    return in.readByte();
  }

  @Override
  public int readUnsignedByte() {
    return in.readUnsignedByte() & 0xFF;
  }

  @Override
  public short readShort() {
    return in.readShort();
  }

  @Override
  public int readUnsignedShort() {
    return in.readUnsignedShort();
  }

  @Override
  public char readChar() {
    return in.readChar();
  }

  @Override
  public int readInt() {
    return in.readInt();
  }

  @Override
  public long readLong() {
    return in.readLong();
  }

  @Override
  public float readFloat() {
    return in.readFloat();
  }

  @Override
  public double readDouble() {
    return in.readDouble();
  }

  @Override
  public String readLine() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String readUTF() {
    try {
      return DataInputStream.readUTF(this);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
