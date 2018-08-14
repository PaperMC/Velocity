package com.velocitypowered.proxy.console;

import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.*;

import java.util.List;
import java.util.Optional;

public final class VelocityConsole extends SimpleTerminalConsole {

    private final VelocityServer server;

    public VelocityConsole(VelocityServer server) {
        this.server = server;
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder
                .appName("Velocity")
                .completer((reader, parsedLine, list) -> {
                    Optional<List<String>> offers = server.getCommandManager().offerSuggestions(server.getConsoleCommandSource(), parsedLine.line());
                    if (offers.isPresent()) {
                        for (String offer : offers.get()) {
                            if (offer.isEmpty()) continue;
                            list.add(new Candidate(offer));
                        }
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
        if (!this.server.getCommandManager().execute(this.server.getConsoleCommandSource(), command)) {
            server.getConsoleCommandSource().sendMessage(TextComponent.of("Command not found.", TextColor.RED));
        }
    }

    @Override
    protected void shutdown() {
        this.server.shutdown();
    }

}
