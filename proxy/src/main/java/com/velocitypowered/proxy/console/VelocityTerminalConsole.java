package com.velocitypowered.proxy.console;

import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.proxy.VelocityServer;
import java.util.List;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

public final class VelocityTerminalConsole extends SimpleTerminalConsole {

  private static final Logger logger = LogManager.getLogger(VelocityTerminalConsole.class);

  private final ConsoleCommandSource consoleCommandSource;
  private final VelocityServer server;

  public VelocityTerminalConsole(ConsoleCommandSource consoleCommandSource, VelocityServer server) {
    this.consoleCommandSource = consoleCommandSource;
    this.server = server;
  }

  @Override
  protected LineReader buildReader(LineReaderBuilder builder) {
    return super.buildReader(builder
            .appName("Velocity")
            .completer((reader, parsedLine, list) -> {
              try {
                List<String> offers = this.server.getCommandManager()
                        .offerSuggestions(consoleCommandSource, parsedLine.line())
                        .join(); // Console doesn't get harmed much by this...
                for (String offer : offers) {
                  list.add(new Candidate(offer));
                }
              } catch (Exception e) {
                logger.error("An error occurred while trying to perform tab completion.", e);
              }
            })
    );
  }

  @Override
  protected boolean isRunning() {
    return !this.server.isShutdown();
  }

  @Override
  protected void runCommand(String command) {
    try {
      if (!this.server.getCommandManager().execute(consoleCommandSource, command)) {
        consoleCommandSource.sendMessage(TextComponent.of("Command not found.", TextColor.RED));
      }
    } catch (Exception e) {
      logger.error("An error occurred while running this command.", e);
    }
  }

  @Override
  protected void shutdown() {
    this.server.shutdown(true);
  }
}
