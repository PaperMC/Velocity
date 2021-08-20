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

package com.velocitypowered.proxy.util.hash;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Perfect hashing, used as an optimization for the {@code DensePacketRegistryMap}.
 */
public class PerfectHash {

  /**
   * Determines a key that, when used with {@link #hash(Object, int, int)}, results in a perfect 1:1
   * representation of the hash codes given in the {@code hashCodes} when placed into a table of
   * size {@code hashSize}.
   *
   * @param hashCodes the array of hash codes
   * @param hashSize the size of the array to place the hash codes in
   * @return the key for use with {@link #hash(Object, int, int)}
   */
  public static int findPerfectHashKey(int[] hashCodes, int hashSize) {
    int[] frequencies = new int[hashSize];
    int key = -1;

    do {
      // need to clear the hashCodes and try again
      Arrays.fill(frequencies, 0);

      ++key;
      for (int elem : hashCodes) {
        frequencies[hash(elem, key, hashSize)]++;
      }
    } while (!isPerfect(frequencies));

    return key;
  }

  private static boolean isPerfect(int[] frequencies) {
    for (int frequency : frequencies) {
      if (frequency != 0 && frequency != 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determines the bucket that the key {@code o} belongs in a perfect hash table, with a key
   * computed by {@link #findPerfectHashKey(int[], int)}.
   *
   * @param o the object to hash
   * @param key the key computed by {@link #findPerfectHashKey(int[], int)}
   * @param hashSize the size of the hash table
   * @return the appropriate bucket
   */
  public static int hash(Object o, int key, int hashSize) {
    return Math.abs(HashCommon.mix(o.hashCode() + key)) % hashSize;
  }
}
