/*
 * This file is part of commons, licensed under the MIT License.
 *
 * Copyright (c) 2021-2024 Seiama
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.velocitypowered.api.util;

import org.jspecify.annotations.NullMarked;

/**
 * Something that is ordered.
 *
 * @param <T> the type
 * @since 3.3.0
 */
@NullMarked
@SuppressWarnings("ComparableType") // allows us to be more flexible
public interface Ordered<T> extends Comparable<T> {
  /**
   * Checks if {@code this} is greater than {@code that}.
   *
   * @param that the other object
   * @return {@code true} if {@code this} is greater than {@code that}, {@code false} otherwise
   * @since 3.3.0
   */
  default boolean greaterThan(final T that) {
    return this.compareTo(that) > 0;
  }

  /**
   * Checks if {@code this} is greater than or equal to {@code that}.
   *
   * @param that the other object
   * @return {@code true} if {@code this} is greater than or
   *     equal to {@code that}, {@code false} otherwise
   * @since 3.3.0
   */
  default boolean noLessThan(final T that) {
    return this.compareTo(that) >= 0;
  }

  /**
   * Checks if {@code this} is less than {@code that}.
   *
   * @param that the other object
   * @return {@code true} if {@code this} is less than {@code that}, {@code false} otherwise
   * @since 3.3.0
   */
  default boolean lessThan(final T that) {
    return this.compareTo(that) < 0;
  }

  /**
   * Checks if {@code this} is less than or equal to {@code that}.
   *
   * @param that the other object
   * @return {@code true} if {@code this} is less than or
   *     equal to {@code that}, {@code false} otherwise
   * @since 3.3.0
   */
  default boolean noGreaterThan(final T that) {
    return this.compareTo(that) <= 0;
  }

  /**
   * Checks if {@code this} is equal to {@code that}.
   *
   * @param that the other object
   * @return {@code true} if {@code this} is equal to {@code that}, {@code false} otherwise
   * @since 3.3.0
   */
  default boolean noGreaterOrLessThan(final T that) {
    return this.compareTo(that) == 0;
  }
}
