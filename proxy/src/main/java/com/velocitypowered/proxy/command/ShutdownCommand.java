package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class ShutdownCommand implements Command {
    @Override
    public void execute(CommandSource source, String[] args) {
        if (source != VelocityServer.getServer().getConsoleCommandSource()) {
            source.sendMessage(TextComponent.of("You are not allowed to use this command.", TextColor.RED));
            return;
        }
        VelocityServer.getServer().shutdown();
    }
}
