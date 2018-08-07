package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandInvoker;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import javax.annotation.Nonnull;

public class ShutdownCommand implements CommandExecutor {
    @Override
    public void execute(@Nonnull CommandInvoker invoker, @Nonnull String[] args) {
        if (invoker != VelocityServer.getServer().getConsoleCommandInvoker()) {
            invoker.sendMessage(TextComponent.of("You are not allowed to use this command.", TextColor.RED));
            return;
        }
        VelocityServer.getServer().shutdown();
    }
}
