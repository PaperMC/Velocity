package com.velocitypowered.proxy.network.pipeline;

import io.netty.util.ByteProcessor;

class VarintByteDecoder implements ByteProcessor {

  private int readVarint;
  private int bytesRead;
  private DecodeResult result = DecodeResult.TOO_SHORT;

  @Override
  public boolean process(byte k) {
    readVarint |= (k & 0x7F) << bytesRead++ * 7;
    if (bytesRead > 3) {
      result = DecodeResult.TOO_BIG;
      return false;
    }
    if ((k & 0x80) != 128) {
      result = DecodeResult.SUCCESS;
      return false;
    }
    return true;
  }

  public int getReadVarint() {
    return readVarint;
  }

  public int getBytesRead() {
    return bytesRead;
  }

  public DecodeResult getResult() {
    return result;
  }

  public enum DecodeResult {
    SUCCESS,
    TOO_SHORT,
    TOO_BIG
  }
}
