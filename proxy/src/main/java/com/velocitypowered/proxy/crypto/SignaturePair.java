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

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public class SignaturePair {

  private final UUID signer;
  private final byte[] signature;

  private static final byte[] EMPTY = new byte[0];

  public SignaturePair(UUID signer, byte[] signature) {
    this.signer = signer;
    this.signature = signature;
  }

  public SignaturePair(UUID signer) {
    this(signer, EMPTY);
  }

  public byte[] getSignature() {
    return signature;
  }

  public UUID getSigner() {
    return signer;
  }


  public boolean isEmpty() {
    return signature.length == 0;
  }

  @Override
  public String toString() {
    return "SignaturePair{"
            + "signer=" + signer
            + ", signature=" + new String(Base64.getEncoder().encode(signature))
            + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SignaturePair that = (SignaturePair) o;
    return signer.equals(that.signer) && Arrays.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(signer);
    result = 31 * result + Arrays.hashCode(signature);
    return result;
  }
}
