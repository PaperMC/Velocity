package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This event is fired after a command tab complete response is sent by the remote server. The
 * plugin that owns this command then suggests tab completions. Here, this event provides you the
 * opportunity to modify the command tab completions suggested by the plugin. Velocity will wait
 * for this event to finish firing before sending the tab complete results to the client. Be sure
 * to be as fast as possible, since the client will freeze while it waits for the tab complete
 * results.
 */
@AwaitingEvent
public class CommandTabCompleteEvent {
    private final Player player;
    private final String command;
    private List<CommandSuggestion> suggestions;

    /**
     * Constructs a new TabCompleteCommandEvent instance.
     *
     * @param player the player
     * @param command the command prompt that the player is attempting to tab complete
     * @param suggestions the initial list of suggestions
     */
    public CommandTabCompleteEvent(Player player, String command, List<CommandSuggestion> suggestions) {
        this.player = checkNotNull(player, "player");
        this.command = checkNotNull(command, "command");
        this.suggestions = suggestions;
    }

    /**
     * Returns the player requesting the tab completion.
     *
     * @return the requesting player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the command being completed.
     *
     * @return the command message
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns all the suggestions provided to the user, as a mutable list.
     *
     * @return the suggestions
     */
    public List<CommandSuggestion> getSuggestions() {
        return suggestions;
    }

    /**
     * Set the suggestions provided to the user.
     *
     * @param suggestions the suggestions
     */
    public void setSuggestions(List<CommandSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "TabCompleteEvent{"
                + "player=" + player
                + ", partialMessage='" + command + '\''
                + ", suggestions=" + suggestions
                + '}';
    }

    public static class CommandSuggestion {
        private final String text;
        private final Component tooltip;

        public CommandSuggestion(String text, Component tooltip) {
            this.text = text;
            this.tooltip = tooltip;
        }

        public Component getTooltip() {
            return tooltip;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "CommandSuggestion{"
                    + "text='" + text + '\''
                    + ", tooltip='" + tooltip + '\''
                    + '}';
        }
    }
}
