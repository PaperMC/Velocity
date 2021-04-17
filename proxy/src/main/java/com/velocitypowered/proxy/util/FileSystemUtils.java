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

package com.velocitypowered.proxy.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;

public class FileSystemUtils {

  /**
   * Visits the resources at the given {@link Path} within the resource
   * path of the given {@link Class}.
   *
   * @param target The target class of the resource path to scan
   * @param path The path to scan within the resource path
   * @param consumer The consumer to visit the resolved path
   */
  @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
  public static boolean visitResources(Class<?> target, Path path, Consumer<Path> consumer)
      throws IOException {
    final File file = new File(target
        .getProtectionDomain().getCodeSource().getLocation().getPath());

    if (file.isFile()) { // jar
      URI uri = file.toURI();
      try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        Path toVisit = fileSystem.getPath(path.toString());
        if (Files.exists(toVisit)) {
          consumer.accept(toVisit);
          return true;
        }
        return false;
      }
    } else {
      URI uri;
      try {
        URL url = target.getClassLoader().getResource(path.toString());
        if (url == null) {
          return false;
        }
        uri = url.toURI();
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
      consumer.accept(Paths.get(uri));
      return true;
    }
  }
}
