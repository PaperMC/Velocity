/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.crypto;

import com.velocitypowered.api.proxy.crypto.SignedMessage;
import java.util.Arrays;
import java.util.Base64;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HeaderData {

  private final SignaturePair header;
  private final byte[] signature;
  private final byte[] dataHash;
  //private final IdentifiedKey signer;
  private final @Nullable SignedMessage message;

  public HeaderData(SignaturePair header, byte[] signature, byte[] dataHash) {
    this.header = header;
    this.signature = signature;
    this.dataHash = dataHash;
    this.message = null;
  }

  public byte[] getDataHash() {
    return dataHash;
  }

  public SignaturePair getHeader() {
    return header;
  }

  public @Nullable byte[] getSignature() {
    return signature;
  }

  @Override
  public String toString() {
    return "HeaderData{"
            + "header=" + header
            + ", signature=" + new String(Base64.getEncoder().encode(signature))
            + ", dataHash=" + new String(Base64.getEncoder().encode(dataHash))
            + ", message=" + message
            + '}';
  }
}
