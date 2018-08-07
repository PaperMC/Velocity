package com.velocitypowered.proxy.console;

import com.velocitypowered.proxy.VelocityServer;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

public final class VelocityConsole extends SimpleTerminalConsole {

    private final VelocityServer server;

    public VelocityConsole(VelocityServer server) {
        this.server = server;
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        return super.buildReader(builder
            .appName("Velocity")
            // TODO: Command completion
        );
    }

    @Override
    protected boolean isRunning() {
        return !this.server.isShutdown();
    }

    @Override
    protected void runCommand(String command) {
        this.server.getCommandManager().execute(this.server.getConsoleCommandInvoker(), command);
    }

    @Override
    protected void shutdown() {
        this.server.shutdown();
    }

}
