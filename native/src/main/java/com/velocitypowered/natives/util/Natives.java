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

package com.velocitypowered.natives.util;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.natives.NativeSetupException;
import com.velocitypowered.natives.compression.Java11VelocityCompressor;
import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.LibdeflateVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressorFactory;
import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.encryption.NativeVelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Natives {

  private Natives() {
    throw new AssertionError();
  }

  private static Runnable copyAndLoadNative(String path) {
    return () -> {
      try {
        InputStream nativeLib = Natives.class.getResourceAsStream(path);
        if (nativeLib == null) {
          throw new IllegalStateException("Native library " + path + " not found.");
        }

        Path tempFile = createTemporaryNativeFilename(path.substring(path.lastIndexOf('.')));
        Files.copy(nativeLib, tempFile, StandardCopyOption.REPLACE_EXISTING);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            Files.deleteIfExists(tempFile);
          } catch (IOException ignored) {
            // Well, it doesn't matter...
          }
        }));

        try {
          System.load(tempFile.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
          throw new NativeSetupException("Unable to load native " + tempFile.toAbsolutePath(), e);
        }
      } catch (IOException e) {
        throw new NativeSetupException("Unable to copy natives", e);
      }
    };
  }

  private static Path createTemporaryNativeFilename(String ext) throws IOException {
    String temporaryFolderPath = System.getProperty("velocity.natives-tmpdir");
    if (temporaryFolderPath != null) {
      return Files.createTempFile(Paths.get(temporaryFolderPath), "native-", ext);
    } else {
      return Files.createTempFile("native-", ext);
    }
  }

  public static final NativeCodeLoader<VelocityCompressorFactory> compress = new NativeCodeLoader<>(
      ImmutableList.of(
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_X86_64,
              copyAndLoadNative("/linux_x86_64/velocity-compress.so"),
              "libdeflate (Linux x86_64)",
              LibdeflateVelocityCompressor.FACTORY),
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_AARCH64,
              copyAndLoadNative("/linux_aarch64/velocity-compress.so"),
              "libdeflate (Linux aarch64)",
              LibdeflateVelocityCompressor.FACTORY),
          new NativeCodeLoader.Variant<>(NativeConstraints.JAVA_11, () -> {
          }, "Java 11", () -> Java11VelocityCompressor.FACTORY),
          new NativeCodeLoader.Variant<>(NativeCodeLoader.ALWAYS, () -> {
          }, "Java", JavaVelocityCompressor.FACTORY)
      )
  );

  public static final NativeCodeLoader<VelocityCipherFactory> cipher = new NativeCodeLoader<>(
      ImmutableList.of(
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_X86_64,
              copyAndLoadNative("/linux_x86_64/velocity-cipher.so"), // Any local version
              "OpenSSL local (Linux x86_64)", NativeVelocityCipher.FACTORY),
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_X86_64,
              copyAndLoadNative("/linux_x86_64/velocity-cipher-ossl11x.so"), // Debian 9
              "OpenSSL 1.1.x (Linux x86_64)", NativeVelocityCipher.FACTORY),
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_X86_64,
              copyAndLoadNative("/linux_x86_64/velocity-cipher-ossl10x.so"), // CentOS 7
              "OpenSSL 1.0.x (Linux x86_64)", NativeVelocityCipher.FACTORY),
          new NativeCodeLoader.Variant<>(NativeConstraints.LINUX_AARCH64,
              copyAndLoadNative("/linux_aarch64/velocity-cipher.so"),
              "OpenSSL (Linux aarch64)", NativeVelocityCipher.FACTORY),
          new NativeCodeLoader.Variant<>(NativeCodeLoader.ALWAYS, () -> {
          }, "Java", JavaVelocityCipher.FACTORY)
      )
  );
}
