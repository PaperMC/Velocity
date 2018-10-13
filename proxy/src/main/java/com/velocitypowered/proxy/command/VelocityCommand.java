package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandExecutor;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.PermissionState;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class VelocityCommand implements Command {

    private final VelocityCommandManager manager;

    private final String[] aliases;
    private final String description;
    private final Command parent;
    private final Set<Command> children;
    private final CommandExecutor executor;
    private final PermissionState permissionState;
    private final String permission;

    public VelocityCommand(@NonNull VelocityCommandManager manager,
                           @NonNull String[] aliases,
                           @NonNull String description,
                           Command parent,
                           @NonNull CommandExecutor executor,
                           @NonNull PermissionState permissionState,
                           String permission) {
        this.manager = manager;
        this.aliases = aliases;
        this.description = description;
        this.parent = parent;
        this.children = new HashSet<>();
        this.executor = executor;
        this.permissionState = permissionState;
        this.permission = permission;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.manager;
    }

    @Override
    public String[] getAliases() {
        return this.aliases;
    }

    @Override
    public boolean isTriggered(@NonNull String trigger) {
        return Arrays.asList(this.aliases).contains(trigger.toLowerCase());
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Optional<Command> getParent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public boolean hasParent() {
        return this.getParent().isPresent();
    }

    @Override
    public String getCommandPath() {
        return this.parent.getCommandPath() + " " + this.aliases[0];
    }

    @Override
    public Set<Command> getChildren() {
        return this.children;
    }

    @Override
    public Optional<Command> getChild(@NonNull String name) {
        return this.children.stream().filter(child -> child.isTriggered(name)).findFirst();
    }

    @Override
    public boolean hasChild(@NonNull String name) {
        return this.getChild(name).isPresent();
    }

    @Override
    public CommandExecutor getExecutor() {
        return this.executor;
    }

    @Override
    public PermissionState getPermissionState() {
        return this.permissionState;
    }

    @Override
    public boolean hasPermission(@NonNull CommandSource source) {
        if (source == this.manager.getServer().getConsoleCommandSource()) {
            // Source is the console.
            if (this.permissionState == PermissionState.PLAYER_ONLY) {
                source.sendMessage(TextComponent.of("This command can only be executed by a player.", TextColor.RED));
                return false;
            }

            return true;
        } else {
            // Source is a player.
            if (this.permissionState == PermissionState.CONSOLE_ONLY) {
                return false;
            }

            if (this.permission == null) {
                return true;
            }

            return source.getPermissionValue(this.permission) != Tristate.FALSE;
        }
    }
}
