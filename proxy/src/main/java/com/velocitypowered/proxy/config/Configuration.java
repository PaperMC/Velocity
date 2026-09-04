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

import java.util.List;
import java.util.Set;

/**
 * Reads typed values from a set of keys. A section of a configuration is itself a configuration,
 * so a nested section is read the same way a whole file is. Each implementation converts its own
 * storage, so a source that keeps everything as text parses it here rather than at the call site.
 * A getter throws when the key is undefined, which {@link #contains(String)} tests for.
 *
 * <p>A key naming an entry of this configuration is matched as it is written, so a section of
 * user-chosen keys such as {@code forced-hosts} can be read even though its keys contain dots.
 * A key matching no entry is then tried as a dotted path into nested sections.</p>
 *
 * <p>{@link #keys()} names the entries of this configuration alone, while {@link #paths()} names
 * every path below it and {@link #leafPaths()} only those that are not themselves sections.</p>
 */
public interface Configuration {

  Set<String> keys();

  Set<String> paths();

  Set<String> leafPaths();

  boolean contains(String key);

  String getString(String key);

  int getInt(String key);

  boolean getBoolean(String key);

  List<String> getStringList(String key);

  <T extends Enum<T>> T getEnum(String key, Class<T> type);

  Configuration getSection(String key);
}
