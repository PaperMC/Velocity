/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utilities to load resources.
 */
public class ResourceUtils {

  /**
   * Visits the resources at the given {@link Path} within the resource path of the given
   * {@link Class}.
   *
   * @param target                  The target class of the resource path to scan
   * @param consumer                The consumer to visit the resolved path
   * @param firstPathComponent      First path component
   * @param remainingPathComponents Remaining path components
   */
  @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
  public static boolean visitResources(Class<?> target, Consumer<Path> consumer,
      String firstPathComponent, String... remainingPathComponents)
      throws IOException {
    final URL knownResource = ResourceUtils.class.getClassLoader()
        .getResource("default-velocity.toml");
    if (knownResource == null) {
      throw new IllegalStateException(
          "default-velocity.toml does not exist, don't know where we are");
    }
    if (knownResource.getProtocol().equals("jar")) {
      // Running from a JAR
      String jarPathRaw = knownResource.toString().split("!")[0];
      URI path = URI.create(jarPathRaw + "!/");

      try (FileSystem fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"))) {
        Path toVisit = fileSystem.getPath(firstPathComponent, remainingPathComponents);
        if (Files.exists(toVisit)) {
          consumer.accept(toVisit);
          return true;
        }
        return false;
      }
    } else {
      // Running from the file system
      URI uri;
      List<String> componentList = new ArrayList<>();
      componentList.add(firstPathComponent);
      componentList.addAll(Arrays.asList(remainingPathComponents));

      try {
        URL url = target.getClassLoader().getResource(String.join("/", componentList));
        if (url == null) {
          return false;
        }
        uri = url.toURI();
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
      consumer.accept(Path.of(uri));
      return true;
    }
  }
}
