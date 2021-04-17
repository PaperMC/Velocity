package com.velocitypowered.proxy.util;

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
