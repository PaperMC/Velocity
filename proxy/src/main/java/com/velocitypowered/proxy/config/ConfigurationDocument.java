/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.config;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A configuration that can be edited, commented and written back out. Reading the proxy's
 * options only needs {@link Configuration}; this is what migrations and the conversion from the
 * legacy TOML format work on, and what the proxy saves once they have run.
 */
public interface ConfigurationDocument extends Configuration {

  void set(String path, @Nullable Object value);

  void remove(String path);

  @Nullable String getComment(String path);

  void setComment(String path, @Nullable String comment);

  void save(Path path) throws IOException;

  void write(Writer writer) throws IOException;

  /**
   * Copies the comments of {@code source} onto the paths both documents share, leaving the
   * comments of every other path alone.
   */
  default void copyCommentsFrom(final ConfigurationDocument source) {
    for (final String path : source.paths()) {
      final String comment = source.getComment(path);
      if (comment != null && contains(path)) {
        setComment(path, comment);
      }
    }
  }
}
