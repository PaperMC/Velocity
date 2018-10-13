package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.Set;

/**
 * Represents a command that can be executed by a {@link CommandSource}, such as a {@link com.velocitypowered.api.proxy.Player}
 * or the console.
 */
public interface Command {
    /**
     * Gets the CommandManager for this command.
     * @return the CommandManager
     */
    CommandManager getCommandManager();

    /**
     * Gets the alternative 'triggers' for this command.
     * @return the aliases for this command
     */
    String[] getAliases();

    /**
     * Specifies whether or not the provided argument would trigger this command.
     * @param trigger the potential trigger
     * @return true if this command would be triggered
     */
    boolean isTriggered(@NonNull String trigger);

    /**
     * Gets the description for this command.
     *
     * <p>Primarily used for Velocity's internal help command.</p>
     *
     * @return the description for this command
     */
    String getDescription();

    /**
     * Gets the parent of this command, if it exists.
     * @return the parent of this command
     */
    Optional<Command> getParent();

    /**
     * Specifies whether this command has a parent.
     * @return whether this command has a parent
     */
    boolean hasParent();

    /**
     * Gets the command path for this command.
     *
     * <p>For example: the command <code>/velocity help</code>
     * would return <code>velocity help</code> as a String.</p>
     *
     * @return the command path for this command
     */
    String getCommandPath();

    /**
     * Gets the children that belong to this command.
     *
     * @return the children for this command
     */
    Set<Command> getChildren();

    /**
     * Gets a specific child belonging to this command.
     * @param name the name or alias belonging to to the child
     * @return the specific child belonging to this command
     */
    Optional<Command> getChild(@NonNull String name);

    /**
     * Specifies whether or not a child by the specified String
     * belongs to this command.
     * @param name the child name to query
     * @return whether the
     */
    boolean hasChild(@NonNull String name);

    /**
     * Gets the {@link CommandExecutor} for this command.
     * @return the CommandExecutor for this command
     */
    CommandExecutor getExecutor();

    /**
     * Get the state of permissions for this command.
     * @return the permission state for this command
     */
    PermissionState getPermissionState();

    /**
     * Tests to check if the {@code source} has permission to use this command.
     *
     * <p>If this method returns false, the handling will be forwarded onto
     * the player's current server.</p>
     *
     * @param source the source of the command
     * @return whether the source has permission
     */
    boolean hasPermission(@NonNull CommandSource source);
}
