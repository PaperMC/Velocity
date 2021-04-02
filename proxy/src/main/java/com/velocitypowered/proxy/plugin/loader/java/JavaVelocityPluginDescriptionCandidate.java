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

package com.velocitypowered.proxy.plugin.loader.java;

import static com.google.common.base.Preconditions.checkNotNull;

import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

class JavaVelocityPluginDescriptionCandidate extends VelocityPluginDescription {

  private final String mainClass;

  JavaVelocityPluginDescriptionCandidate(String id, @Nullable String name, @Nullable String version,
      @Nullable String description, @Nullable String url,
      @Nullable List<String> authors, Collection<PluginDependency> dependencies, Path source,
      String mainClass) {
    super(id, name, version, description, url, authors, dependencies, source);
    this.mainClass = checkNotNull(mainClass);
  }

  String getMainClass() {
    return mainClass;
  }
}
