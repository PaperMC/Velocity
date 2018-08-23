package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;

public class VelocityCommand implements Command {
    @Override
    public void execute(CommandSource source, String[] args) {
        String implVersion = VelocityServer.class.getPackage().getImplementationVersion();
        TextComponent thisIsVelocity = TextComponent.builder()
                .content("This is ")
                .append(TextComponent.of("Velocity " + implVersion, TextColor.DARK_AQUA))
                .append(TextComponent.of(", the next generation Minecraft: Java Edition proxy.").resetStyle())
                .build();
        TextComponent velocityInfo = TextComponent.builder()
                .content("Copyright 2018 Velocity Contributors. Velocity is freely licensed under the terms of the " +
                        "MIT License.")
                .build();
        TextComponent velocityWebsite = TextComponent.builder()
                .content("Visit the ")
                .append(TextComponent.builder("Velocity website")
                        .color(TextColor.GREEN)
                        .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.velocitypowered.com"))
                        .build())
                .append(TextComponent.of(" or the ").resetStyle())
                .append(TextComponent.builder("Velocity GitHub")
                        .color(TextColor.GREEN)
                        .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/astei/velocity"))
                        .build())
                .build();

        source.sendMessage(thisIsVelocity);
        source.sendMessage(velocityInfo);
        source.sendMessage(velocityWebsite);
    }
}
