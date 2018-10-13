package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandBuilder;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class VelocityCommandManager implements CommandManager {

    private final VelocityServer server;
    private final Set<Command> commands;

    public VelocityCommandManager(final VelocityServer server) {
        this.server = server;
        this.commands = new HashSet<>();
    }

    public VelocityServer getServer() {
        return this.server;
    }

    @Override
    public void register(@NonNull Command command) {
        if (this.commands.stream().anyMatch(existing -> Arrays.stream(command.getAliases()).anyMatch(existing::isTriggered))) {
            throw new IllegalArgumentException("Duplicate command found: " + command.getAliases()[0]);
        }

        this.commands.add(command);
    }

    @Override
    public void unregister(@NonNull String alias) {
        this.commands.removeIf(command -> command.isTriggered(alias));
    }

    @Override
    public boolean hasCommand(@NonNull String alias) {
        return this.lookup(alias).getCommand() != null;
    }

    @Override
    public Optional<List<String>> offerSuggestions(@NonNull CommandSource source, @NonNull String rawInput) {
        if (rawInput.trim().isEmpty()) {
            return Optional.of(this.commands.stream()
                    .map(command -> "/" + command.getAliases()[0])
                    .collect(Collectors.toList()));
        }

        final CommandLookup lookup = this.lookup(rawInput);
        final Command command = lookup.getCommand();
        if (command == null) {
            return Optional.empty();
        }
        final String[] args = lookup.getRemaining();

        final List<String> suggestions = command.getExecutor().suggest(source, args);
        command.getChildren().forEach(child -> suggestions.add(child.getAliases()[0]));

        return Optional.of(suggestions);
    }

    @Override
    public boolean execute(@NonNull CommandSource source, @NonNull String rawInput) {
        final CommandLookup lookup = this.lookup(rawInput);
        final Command command = lookup.getCommand();
        if (command == null) {
            return false;
        }
        final String[] args = lookup.getRemaining();

        try {
            if (!command.hasPermission(source)) {
                return false;
            }

            command.getExecutor().execute(source, args);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke command " + rawInput + " for " + source, e);
        }
    }

    @Override
    public CommandBuilder builder(@NonNull String alias, String... aliases) {
        return new VelocityCommandBuilder(this, alias, aliases);
    }

    public CommandLookup lookup(@NonNull String input) {
        final CommandLookup lookup = new CommandLookup(input);
        lookup.lookup(this.commands);
        return lookup;
    }
}
