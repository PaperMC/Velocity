package com.velocitypowered.proxy.protocol.util;

import com.google.common.io.ByteArrayDataInput;
import io.netty.buffer.ByteBuf;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * A wrapper around {@link io.netty.buffer.ByteBuf} that implements the exception-free
 * {@link ByteArrayDataInput} interface from Guava.
 */
public class ByteBufDataInput implements ByteArrayDataInput, DataInput {

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
