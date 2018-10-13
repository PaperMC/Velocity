package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;

import java.util.Arrays;
import java.util.Set;

public class CommandLookup {

    private final String rawInput;

    private Command command;
    private String[] remaining;

    public CommandLookup(String rawInput) {
        this.rawInput = rawInput;

        this.command = null;
        this.remaining = null;
    }

    public void lookup(Set<Command> commands) {
        this.remaining = this.rawInput.split("\\s+");
        if (this.remaining.length == 0) {
            return;
        }

        this.command = commands.stream()
                .filter(command -> command.isTriggered(this.remaining[0]))
                .findFirst().orElse(null);

        if (this.command == null) {
            return;
        }

        this.remaining = Arrays.copyOfRange(this.remaining, 1, this.remaining.length);

        while (this.remaining.length > 0 && this.command.hasChild(this.remaining[0])) {
            this.command = command.getChild(this.remaining[0]).get();
            this.remaining = Arrays.copyOfRange(this.remaining, 1, this.remaining.length);
        }
    }

    public Command getCommand() {
        return this.command;
    }

    public String[] getRemaining() {
        return this.remaining;
    }
}
