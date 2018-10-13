package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a builder which can be used to form a {@link Command}.
 */
public interface CommandBuilder {

    /**
     * Sets the description for the potential {@link Command}.
     * @param description the description
     * @return this builder
     */
    CommandBuilder description(String description);

    /**
     * Adds a potential child to this potential {@link Command}.
     * @param builder the child's builder
     * @return this builder
     */
    CommandBuilder child(@NonNull CommandBuilder builder);

    /**
     * Sets the {@link CommandExecutor} for the potential {@link Command}.
     * @param executor the executor
     * @return this builder
     */
    CommandBuilder executor(@NonNull CommandExecutor executor);

    /**
     * Sets the {@link PermissionState} for the potential {@link Command}.
     * @param state the state
     * @return this builder
     */
    CommandBuilder permissionState(@NonNull PermissionState state);

    /**
     * Sets the permission required for {@link CommandSource}s to
     * execute the potential {@link Command}.
     * @param permission the permission
     * @return this builder
     */
    CommandBuilder permission(String permission);

    /**
     * Builds the potential {@link Command}.
     *
     * @return the built Command
     */
    Command build();

    /**
     * Builds the potential {@link Command} as a child to the parent Command.
     * @param parent the parent command
     * @return the build Command
     */
    Command build(Command parent);
}
