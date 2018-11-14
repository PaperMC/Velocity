package com.velocitypowered.proxy.console;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.proxy.VelocityServer;
import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

public final class VelocityConsole extends SimpleTerminalConsole implements CommandSource {

  private static final Logger logger = LogManager.getLogger(VelocityConsole.class);

  private final VelocityServer server;
  private PermissionFunction permissionFunction = PermissionFunction.ALWAYS_TRUE;

  public VelocityConsole(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void sendMessage(Component component) {
    logger.info(ComponentSerializers.LEGACY.serialize(component));
  }

  @Override
  public @NonNull Tristate getPermissionValue(@NonNull String permission) {
    return this.permissionFunction.getPermissionValue(permission);
  }

  public void setupPermissions() {
    PermissionsSetupEvent event = new PermissionsSetupEvent(this,
        s -> PermissionFunction.ALWAYS_TRUE);
    this.server.getEventManager().fire(event)
        .join(); // this is called on startup, we can safely #join
    this.permissionFunction = event.createFunction(this);
  }

  @Override
  protected LineReader buildReader(LineReaderBuilder builder) {
    return super.buildReader(builder
        .appName("Velocity")
        .completer((reader, parsedLine, list) -> {
          try {
            boolean isCommand = parsedLine.line().indexOf(' ') == -1;
            List<String> offers = this.server.getCommandManager()
                .offerSuggestions(this, parsedLine.line());
            for (String offer : offers) {
              if (isCommand) {
                list.add(new Candidate(offer.substring(1)));
              } else {
                list.add(new Candidate(offer));
              }
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
      if (!this.server.getCommandManager().execute(this, command)) {
        sendMessage(TextComponent.of("Command not found.", TextColor.RED));
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
