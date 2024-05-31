/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.jar.Manifest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal class that is able to do {@link Manifest} stuff.
 */
@ApiStatus.Internal
public final class JarManifests {
  private JarManifests() {
  }

  private static final Map<ClassLoader, Manifest> MANIFESTS = Collections.synchronizedMap(new WeakHashMap<>());

  /**
   * Gets the {@link Manifest} from a class.
   *
   * @param clazz the class to get the {@link Manifest} from.
   * @return the manifest or null.
  */
  public static @Nullable Manifest manifest(final @NotNull Class<?> clazz) {
    return MANIFESTS.computeIfAbsent(clazz.getClassLoader(), classLoader -> {
      final String classLocation = "/" + clazz.getName().replace(".", "/") + ".class";
      final URL resource = clazz.getResource(classLocation);
      if (resource == null) {
        return null;
      }
      final String classFilePath = resource.toString().replace("\\", "/");
      final String archivePath = classFilePath.substring(0, classFilePath.length() - classLocation.length());
      try (final InputStream stream = new URL(archivePath + "/META-INF/MANIFEST.MF").openStream()) {
        return new Manifest(stream);
      } catch (final IOException ex) {
        return null;
      }
    });
  }
}
