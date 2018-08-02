package com.velocitypowered.natives.util;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.natives.compression.JavaVelocityCompressor;
import com.velocitypowered.natives.compression.NativeVelocityCompressor;
import com.velocitypowered.natives.compression.VelocityCompressor;

import java.io.IOException;
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
                Files.copy(Natives.class.getResourceAsStream(path), tempFile, StandardCopyOption.REPLACE_EXISTING);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignored) {
                        // Well, it doesn't matter...
                    }
                }));
                System.load(tempFile.toAbsolutePath().toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static final NativeCodeLoader<VelocityCompressor> compressor = new NativeCodeLoader<>(
            ImmutableList.of(
                    new NativeCodeLoader.Variant<>(NativeCodeLoader.MACOS,
                            copyAndLoadNative("/macosx/velocity-compress.dylib"), "native compression (macOS)",
                            NativeVelocityCompressor::new),
                    new NativeCodeLoader.Variant<>(NativeCodeLoader.LINUX,
                            copyAndLoadNative("/linux_x64/velocity-compress.so"), "native compression (Linux amd64)",
                            NativeVelocityCompressor::new),
                    new NativeCodeLoader.Variant<>(NativeCodeLoader.ALWAYS, () -> {}, "Java compression", JavaVelocityCompressor::new)
            )
    );
}
