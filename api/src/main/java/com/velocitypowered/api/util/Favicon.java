/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util;

import com.google.common.base.Preconditions;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a Minecraft server favicon. A Minecraft server favicon is a 64x64 image that can be
 * displayed to a remote client that sends a Server List Ping packet, and is automatically displayed
 * in the Minecraft client.
 */
public final class Favicon {

  private final String base64Url;

  /**
   * Directly create a favicon using its Base64 URL directly. You are generally better served by the
   * create() series of functions.
   *
   * @param base64Url the url for use with this favicon
   */
  public Favicon(String base64Url) {
    this.base64Url = Preconditions.checkNotNull(base64Url, "base64Url");
  }

  /**
   * Returns the Base64-encoded URI for this image.
   *
   * @return a URL representing this favicon
   */
  public String getBase64Url() {
    return base64Url;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Favicon favicon = (Favicon) o;
    return Objects.equals(base64Url, favicon.base64Url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(base64Url);
  }

  @Override
  public String toString() {
    return "Favicon{"
        + "base64Url='" + base64Url + '\''
        + '}';
  }

  /**
   * Creates a new {@code Favicon} from the specified {@code image}.
   *
   * @param image the image to use for the favicon
   * @return the created {@link Favicon} instance
   */
  public static Favicon create(BufferedImage image) {
    Preconditions.checkNotNull(image, "image");
    Preconditions.checkArgument(image.getWidth() == 64 && image.getHeight() == 64,
        "Image is not 64x64 (found %sx%s)", image.getWidth(), image.getHeight());
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "PNG", os);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return new Favicon(
        "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray()));
  }

  /**
   * Creates a new {@code Favicon} by reading the image from the specified {@code path}.
   *
   * @param path the path to the image to create a favicon for
   * @return the created {@link Favicon} instance
   * @throws IOException if the file could not be read from the path
   */
  public static Favicon create(Path path) throws IOException {
    try (InputStream stream = Files.newInputStream(path)) {
      BufferedImage image = ImageIO.read(stream);
      if (image == null) {
        throw new IOException("Unable to read the image.");
      }
      return create(image);
    }
  }
}
