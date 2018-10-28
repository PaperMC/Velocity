package com.velocitypowered.natives.util;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.natives.NativeSetupException;
import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.NativeVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressorFactory;
import com.velocitypowered.natives.encryption.JavaVelocityCipher;
import com.velocitypowered.natives.encryption.VelocityCipherFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Natives {

  private Natives() {
    throw new AssertionError();
  }

  private static Runnable copyAndLoadNative(String path) {
    return () -> {
      try {
        Path tempFile = Files.createTempFile("native-", path.substring(path.lastIndexOf('.')));
        InputStream nativeLib = Natives.class.getResourceAsStream(path);
        if (nativeLib == null) {
          throw new IllegalStateException("Native library " + path + " not found.");
        }

        Files.copy(nativeLib, tempFile, StandardCopyOption.REPLACE_EXISTING);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
            Files.deleteIfExists(tempFile);
          } catch (IOException ignored) {
            // Well, it doesn't matter...
          }
        }));
        System.load(tempFile.toAbsolutePath().toString());
      } catch (IOException e) {
        throw new NativeSetupException("Unable to copy natives", e);
      }
    };
  }

  public static final NativeCodeLoader<VelocityCompressorFactory> compressor = new NativeCodeLoader<>(
      ImmutableList.of(
          new NativeCodeLoader.Variant<>(NativeCodeLoader.MACOS,
              copyAndLoadNative("/macosx/velocity-compress.dylib"), "native (macOS)",
              NativeVelocityCompressor.FACTORY),
          new NativeCodeLoader.Variant<>(NativeCodeLoader.LINUX,
              copyAndLoadNative("/linux_x64/velocity-compress.so"), "native (Linux amd64)",
              NativeVelocityCompressor.FACTORY),
          new NativeCodeLoader.Variant<>(NativeCodeLoader.ALWAYS, () -> {
          }, "Java", JavaVelocityCompressor.FACTORY)
      )
  );

  public static final NativeCodeLoader<VelocityCipherFactory> cipher = new NativeCodeLoader<>(
      ImmutableList.of(
          /*new NativeCodeLoader.Variant<>(NativeCodeLoader.MACOS,
              copyAndLoadNative("/macosx/velocity-cipher.dylib"), "mbed TLS (macOS)",
              NativeVelocityCipher.FACTORY),
            new NativeCodeLoader.Variant<>(NativeCodeLoader.LINUX,
              copyAndLoadNative("/linux_x64/velocity-cipher.so"), "mbed TLS (Linux amd64)",
              NativeVelocityCipher.FACTORY),*/
          new NativeCodeLoader.Variant<>(NativeCodeLoader.ALWAYS, () -> {
          }, "Java", JavaVelocityCipher.FACTORY)
      )
  );
}
