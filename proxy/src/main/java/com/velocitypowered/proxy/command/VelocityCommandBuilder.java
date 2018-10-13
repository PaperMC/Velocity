package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandBuilder;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.PermissionState;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VelocityCommandBuilder implements CommandBuilder {

    private final VelocityCommandManager manager;
    private final String[] aliases;

    private String description;
    private final Set<CommandBuilder> children;
    private CommandExecutor executor;
    private PermissionState permissionState;
    private String permission;

    public VelocityCommandBuilder(@NonNull VelocityCommandManager manager, @NonNull String alias, String[] aliases) {
        this.manager = manager;
        this.aliases = new String[alias.length() + 1];
        this.aliases[0] = alias.toLowerCase();
        for (int i = 0; i < aliases.length; i++) {
            this.aliases[i + 1] = aliases[i].toLowerCase();
        }

        this.description = "";
        this.children = new HashSet<>();
        this.executor = null;
        this.permissionState = PermissionState.PERMISSIVE;
        this.permission = null;
    }

    @Override
    public CommandBuilder description(String description) {
        if (description == null) {
            description = "";
        }
        this.description = description;
        return this;
    }

    @Override
    public CommandBuilder child(@NonNull CommandBuilder builder) {
        this.children.add(builder);
        return null;
    }

    @Override
    public CommandBuilder executor(@NonNull CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public CommandBuilder permissionState(@NonNull PermissionState state) {
        this.permissionState = state;
        return this;
    }

    @Override
    public CommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    @Override
    public Command build() {
        return this.build(null);
    }

    public Command build(Command parent) {
        Preconditions.checkNotNull(this.executor, "executor");

        final Command command = new VelocityCommand(this.manager, this.aliases, this.description, parent, this.executor, this.permissionState, this.permission);

        this.children.forEach(builder -> {
            Command child = builder.build(command);
            if (Arrays.stream(child.getAliases()).anyMatch(command::hasChild)) {
                throw new IllegalArgumentException("Duplicate command trigger for " + command.getAliases()[0] + ": " + child.getAliases()[0]);
            }
            command.getChildren().add(child);
        });

        return command;
    }
}
