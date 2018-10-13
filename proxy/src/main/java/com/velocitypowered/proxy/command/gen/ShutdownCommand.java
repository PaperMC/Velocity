package com.velocitypowered.proxy.command.gen;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public class ShutdownCommand implements CommandExecutor {

    private final VelocityServer server;

    public ShutdownCommand(VelocityServer server) {
        this.server = server;
    }

    @Override
    public void execute(@NonNull CommandSource source, @NonNull String[] args) {
        this.server.shutdown();
    }

    @Override
    public List<String> suggest(@NonNull CommandSource source, @NonNull String[] args) {
        return ImmutableList.of();
    }
}
