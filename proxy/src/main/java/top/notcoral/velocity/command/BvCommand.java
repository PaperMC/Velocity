/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package top.notcoral.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import top.notcoral.velocity.compression.BvCompressionStats;

/**
 * Implements the {@code /bvelocity} command and aliases.
 */
public final class BvCommand {

  private static final String ROOT_COMMAND = "/bvelocity";
  private static final String SIZE_ARG = "bytes";
  private static final String ROUNDS_ARG = "rounds";
  private static final NamedTextColor ACCENT = NamedTextColor.AQUA;
  private static final NamedTextColor MUTED = NamedTextColor.DARK_GRAY;
  private static final NamedTextColor VALUE = NamedTextColor.WHITE;
  private static final NamedTextColor GOOD = NamedTextColor.GREEN;
  private static final int DEFAULT_BENCHMARK_BYTES = 32768;
  private static final int DEFAULT_BENCHMARK_ROUNDS = 64;

  private BvCommand() {
  }

  /**
   * Creates the root Brigadier command for bVelocity.
   *
   * @param server the proxy server instance
   * @return the command tree root
   */
  public static BrigadierCommand create(final VelocityServer server) {
    final LiteralCommandNode<CommandSource> backend = BrigadierCommand
        .literalArgumentBuilder("backend")
        .requires(source -> source.getPermissionValue("bvelocity.command.status") == Tristate.TRUE)
        .executes(new Backend())
        .build();
    final LiteralCommandNode<CommandSource> config = BrigadierCommand
        .literalArgumentBuilder("config")
        .requires(source -> source.getPermissionValue("bvelocity.command.status") == Tristate.TRUE)
        .executes(new Config(server))
        .build();
    final LiteralCommandNode<CommandSource> status = BrigadierCommand
        .literalArgumentBuilder("status")
        .requires(source -> source.getPermissionValue("bvelocity.command.status") == Tristate.TRUE)
        .executes(new Status(server))
        .build();
    final LiteralCommandNode<CommandSource> compressionBenchmark = BrigadierCommand
        .literalArgumentBuilder("benchmark")
        .requires(source ->
            source.getPermissionValue("bvelocity.command.compression") == Tristate.TRUE)
        .executes(new CompressionBenchmark(server, DEFAULT_BENCHMARK_BYTES, DEFAULT_BENCHMARK_ROUNDS))
        .then(
            BrigadierCommand.requiredArgumentBuilder(SIZE_ARG, IntegerArgumentType.integer(512))
                .executes(ctx -> new CompressionBenchmark(
                    server,
                    IntegerArgumentType.getInteger(ctx, SIZE_ARG),
                    DEFAULT_BENCHMARK_ROUNDS
                ).run(ctx))
                .then(
                    BrigadierCommand.requiredArgumentBuilder(
                            ROUNDS_ARG,
                            IntegerArgumentType.integer(1)
                        )
                        .executes(ctx -> new CompressionBenchmark(
                            server,
                            IntegerArgumentType.getInteger(ctx, SIZE_ARG),
                            IntegerArgumentType.getInteger(ctx, ROUNDS_ARG)
                        ).run(ctx))
                )
        )
        .build();
    final LiteralCommandNode<CommandSource> compressionStats = BrigadierCommand
        .literalArgumentBuilder("stats")
        .requires(source ->
            source.getPermissionValue("bvelocity.command.compression") == Tristate.TRUE)
        .executes(new CompressionStats(server))
        .build();
    final LiteralCommandNode<CommandSource> compressionReset = BrigadierCommand
        .literalArgumentBuilder("reset")
        .requires(source ->
            source.getPermissionValue("bvelocity.command.compression.reset") == Tristate.TRUE)
        .executes(new CompressionReset())
        .build();
    final LiteralCommandNode<CommandSource> compression = BrigadierCommand
        .literalArgumentBuilder("compression")
        .requires(source ->
            source.getPermissionValue("bvelocity.command.compression") == Tristate.TRUE
                || source.getPermissionValue("bvelocity.command.compression.reset")
                == Tristate.TRUE)
        .executes(new CompressionStats(server))
        .then(compressionStats)
        .then(compressionReset)
        .then(compressionBenchmark)
        .build();

    final List<LiteralCommandNode<CommandSource>> commands =
        List.of(status, backend, config, compression);
    return new BrigadierCommand(
        commands.stream()
            .reduce(
                BrigadierCommand.literalArgumentBuilder("bvelocity")
                    .executes(ctx -> {
                      final CommandSource source = ctx.getSource();
                      final String availableCommands = commands.stream()
                          .filter(e -> e.getRequirement().test(source))
                          .map(LiteralCommandNode::getName)
                          .collect(Collectors.joining("|"));
                      if (availableCommands.isEmpty()) {
                        sendCopyright(source, server);
                      } else {
                        sendHelp(source, availableCommands);
                      }
                      return Command.SINGLE_SUCCESS;
                    }),
                ArgumentBuilder::then,
                ArgumentBuilder::then
            )
    );
  }

  private record Status(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final int configuredLevel = server.getConfiguration().getCompressionLevel();
      final String effectiveLevel = configuredLevel == -1 ? "auto(native=12/java=9)"
          : Integer.toString(configuredLevel);

      sendSectionTitle(source, "bvelocity.command.status-title");
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-summary",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant()),
          Argument.string(
              "threshold",
              Integer.toString(server.getConfiguration().getCompressionThreshold())
          ),
          Argument.string("level", effectiveLevel)
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-backend",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-threshold",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(server.getConfiguration().getCompressionThreshold())
          )
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-level",
          VALUE,
          Argument.string("level", effectiveLevel)
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record Backend() implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      sendSectionTitle(source, "bvelocity.command.backend-title");
      sendStatLine(source, Component.translatable(
          "bvelocity.command.backend-name",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.backend-levels",
          VALUE,
          Argument.string("levels", Integer.toString(maxSupportedLevel()))
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record Config(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final int configuredLevel = server.getConfiguration().getCompressionLevel();
      sendSectionTitle(source, "bvelocity.command.config-title");
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-threshold",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(server.getConfiguration().getCompressionThreshold())
          )
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.status-level",
          VALUE,
          Argument.string("level", describeLevel(configuredLevel))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.config-defaults",
          VALUE,
          Argument.string("native", "12"),
          Argument.string("java", "9")
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record CompressionStats(VelocityServer server) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final BvCompressionStats.Snapshot snapshot =
          BvCompressionStats.INSTANCE.snapshot();

      sendSectionTitle(source, "bvelocity.command.compression-title");
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-window",
          VALUE,
          Argument.string("seconds", Long.toString(snapshot.elapsedSeconds()))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-backend",
          VALUE,
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-settings",
          VALUE,
          Argument.string(
              "threshold",
              Integer.toString(server.getConfiguration().getCompressionThreshold())
          ),
          Argument.string(
              "level",
              describeLevel(server.getConfiguration().getCompressionLevel())
          )
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-packets",
          VALUE,
          Argument.string("total", Long.toString(snapshot.totalPackets())),
          Argument.string("compressed", Long.toString(snapshot.compressedPackets())),
          Argument.string("passthrough", Long.toString(snapshot.passthroughPackets()))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-raw-wire",
          VALUE,
          Argument.string("raw", humanBytes(snapshot.totalRawBytes())),
          Argument.string("wire", humanBytes(snapshot.totalEncodedBytes()))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-compressed-wire",
          VALUE,
          Argument.string("raw", humanBytes(snapshot.compressedRawBytes())),
          Argument.string("wire", humanBytes(snapshot.compressedEncodedBytes()))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-savings",
          GOOD,
          Argument.string(
              "bytes",
              humanBytes(Math.max(
                  0L,
                  snapshot.compressedRawBytes() - snapshot.compressedEncodedBytes()
              ))
          ),
          Argument.string(
              "percent",
              percentSaved(snapshot.compressedRawBytes(), snapshot.compressedEncodedBytes())
          )
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.compression-efficiency",
          GOOD,
          Argument.string(
              "percent",
              percentSaved(snapshot.totalRawBytes(), snapshot.totalEncodedBytes())
          )
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record CompressionBenchmark(
      VelocityServer server,
      int sampleBytes,
      int rounds
  ) implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      final CommandSource source = context.getSource();
      final byte[] payload = createBenchmarkPayload(sampleBytes);
      final int maxLevel = maxSupportedLevel();
      long bestSize = Long.MAX_VALUE;
      int bestSizeLevel = 1;
      long bestTime = Long.MAX_VALUE;
      int bestTimeLevel = 1;

      sendSectionTitle(source, "bvelocity.command.benchmark-title");
      sendStatLine(source, Component.translatable(
          "bvelocity.command.benchmark-input",
          VALUE,
          Argument.string("bytes", humanBytes(sampleBytes)),
          Argument.string("rounds", Integer.toString(rounds)),
          Argument.string("backend", Natives.compress.getLoadedVariant())
      ));

      for (int level = 1; level <= maxLevel; level++) {
        final BenchmarkResult result = benchmark(level, payload, rounds);
        if (result.encodedBytes() < bestSize) {
          bestSize = result.encodedBytes();
          bestSizeLevel = level;
        }
        if (result.averageNanos() < bestTime) {
          bestTime = result.averageNanos();
          bestTimeLevel = level;
        }
        sendStatLine(source, Component.translatable(
            "bvelocity.command.benchmark-row",
            VALUE,
            Argument.string("level", Integer.toString(level)),
            Argument.string("encoded", humanBytes(result.encodedBytes())),
            Argument.string("ratio", percentSaved(payload.length, result.encodedBytes())),
            Argument.string("time", formatNanos(result.averageNanos()))
        ));
      }

      sendStatLine(source, Component.translatable(
          "bvelocity.command.benchmark-best-size",
          GOOD,
          Argument.string("level", Integer.toString(bestSizeLevel)),
          Argument.string("encoded", humanBytes(bestSize))
      ));
      sendStatLine(source, Component.translatable(
          "bvelocity.command.benchmark-best-speed",
          GOOD,
          Argument.string("level", Integer.toString(bestTimeLevel)),
          Argument.string("time", formatNanos(bestTime))
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private record CompressionReset() implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) {
      BvCompressionStats.INSTANCE.reset();
      context.getSource().sendMessage(Component.translatable(
          "bvelocity.command.compression-reset",
          NamedTextColor.GREEN
      ));
      return Command.SINGLE_SUCCESS;
    }
  }

  private static String describeLevel(int configuredLevel) {
    return configuredLevel == -1
        ? "auto(native=12/java=9)"
        : Integer.toString(configuredLevel);
  }

  private static int maxSupportedLevel() {
    return Natives.compress.getLoadedVariant().toLowerCase(Locale.ROOT).contains("java") ? 9 : 12;
  }

  private static String percentSaved(long rawBytes, long encodedBytes) {
    if (rawBytes <= 0L) {
      return "0.00%";
    }
    final double percent = 100.0d - ((double) encodedBytes / (double) rawBytes * 100.0d);
    return String.format("%.2f%%", percent);
  }

  private static String humanBytes(long bytes) {
    if (bytes < 1024L) {
      return bytes + " B";
    }
    final String[] units = {"KiB", "MiB", "GiB", "TiB"};
    double value = bytes;
    int index = -1;
    do {
      value /= 1024.0d;
      index++;
    } while (value >= 1024.0d && index < units.length - 1);
    return String.format("%.2f %s", value, units[index]);
  }

  private static String formatNanos(long nanos) {
    if (nanos >= 1_000_000L) {
      return String.format("%.2f ms", nanos / 1_000_000.0d);
    }
    if (nanos >= 1_000L) {
      return String.format("%.2f us", nanos / 1_000.0d);
    }
    return nanos + " ns";
  }

  private static void sendHelp(CommandSource source, String availableCommands) {
    sendHeader(source, Component.translatable("bvelocity.command.help-title", ACCENT));
    sendStatLine(source, Component.translatable(
        "bvelocity.command.usage",
        VALUE,
        Argument.string("commands", availableCommands)
    ));
    sendCommandLine(source, "bvelocity.command.help-backend", ROOT_COMMAND + " backend");
    sendCommandLine(source, "bvelocity.command.help-config", ROOT_COMMAND + " config");
    sendCommandLine(source, "bvelocity.command.help-status", ROOT_COMMAND + " status");
    sendCommandLine(source, "bvelocity.command.help-compression", ROOT_COMMAND + " compression");
    sendCommandLine(source, "bvelocity.command.help-stats", ROOT_COMMAND + " compression stats");
    sendCommandLine(source, "bvelocity.command.help-reset", ROOT_COMMAND + " compression reset");
    sendCommandLine(
        source,
        "bvelocity.command.help-benchmark",
        ROOT_COMMAND + " compression benchmark 32768 64"
    );
  }

  private static void sendCopyright(CommandSource source, VelocityServer server) {
    final ProxyVersion version = server.getVersion();
    sendHeader(source, Component.text(version.getName(), ACCENT));
    sendStatLine(source, Component.text()
        .append(Component.text("Version: ", ACCENT))
        .append(Component.text(version.getVersion(), VALUE))
        .build());
    sendStatLine(source, Component.text()
        .append(Component.text("Author: ", ACCENT))
        .append(Component.text("NotCoral", VALUE))
        .append(Component.text(" (Springbringer)", NamedTextColor.GRAY))
        .build());
    sendStatLine(source, Component.text()
        .append(Component.text("Copyright: ", ACCENT))
        .append(Component.text("(C) " + LocalDate.now().getYear() + " NotCoral", VALUE))
        .build());
  }

  private static void sendSectionTitle(CommandSource source, String translationKey) {
    sendHeader(source, Component.translatable(translationKey, ACCENT));
  }

  private static void sendHeader(CommandSource source, Component title) {
    final TextComponent line = Component.text()
        .append(Component.text("◆ ", ACCENT, TextDecoration.BOLD))
        .append(title.decoration(TextDecoration.BOLD, true))
        .build();
    source.sendMessage(line);
  }

  private static void sendStatLine(CommandSource source, Component content) {
    source.sendMessage(Component.text()
        .append(Component.text("│ ", MUTED))
        .append(content)
        .build());
  }

  private static void sendCommandLine(CommandSource source, String key, String command) {
    source.sendMessage(Component.text()
        .append(Component.text("│ ", MUTED))
        .append(Component.translatable(key, VALUE))
        .append(Component.text(" "))
        .append(Component.text(command, ACCENT, TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(command, VALUE))))
        .build());
  }

  private static BenchmarkResult benchmark(int level, byte[] payload, int rounds) {
    try (VelocityCompressor compressor = Natives.compress.get().create(level)) {
      long totalNanos = 0L;
      int encodedBytes = 0;
      for (int index = 0; index < rounds; index++) {
        ByteBuf source = Unpooled.directBuffer(payload.length);
        ByteBuf encoded = Unpooled.directBuffer(Math.max(512, payload.length));
        ByteBuf decoded = Unpooled.directBuffer(payload.length);
        ByteBuf encodedCopy = null;
        ByteBuf expected = null;
        source.writeBytes(payload);
        long started = System.nanoTime();
        try {
          compressor.deflate(source, encoded);
          totalNanos += System.nanoTime() - started;
          encodedBytes = encoded.readableBytes();
          encodedCopy = encoded.copy();
          compressor.inflate(encodedCopy, decoded, payload.length);
          expected = Unpooled.wrappedBuffer(payload);
          decoded.readerIndex(0);
          if (!ByteBufUtil.equals(expected, decoded)) {
            throw new DataFormatException("Decoded payload did not match source data.");
          }
        } finally {
          if (encodedCopy != null) {
            encodedCopy.release();
          }
          if (expected != null) {
            expected.release();
          }
          source.release();
          encoded.release();
          decoded.release();
        }
      }
      return new BenchmarkResult(encodedBytes, totalNanos / rounds);
    } catch (DataFormatException ex) {
      throw new IllegalStateException("Compression benchmark failed at level " + level, ex);
    }
  }

  private static byte[] createBenchmarkPayload(int sampleBytes) {
    final byte[] payload = new byte[sampleBytes];
    final byte[] repeated = (
        "{\"packet\":\"chat\",\"component\":\"<gray>[bVelocity]</gray> "
            + "<aqua>compression probe</aqua>\",\"coords\":[123,64,-512],"
            + "\"server\":\"lobby\",\"nbt\":\"{foo:1b,bar:\\\"baz\\\"}\"}"
    ).getBytes(StandardCharsets.UTF_8);
    final Random random = new Random(12L);
    for (int index = 0; index < payload.length; index++) {
      final int marker = index % 16;
      if (marker < 12) {
        payload[index] = repeated[index % repeated.length];
      } else {
        payload[index] = (byte) random.nextInt(256);
      }
    }
    return payload;
  }

  private record BenchmarkResult(int encodedBytes, long averageNanos) {
  }
}
