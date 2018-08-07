package com.velocitypowered.api.plugin;

import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * A wrapper around a class marked with an {@link Plugin} annotation to
 * retrieve information from the annotation for easier use.
 */
public interface PluginContainer extends PluginCandidate {
    /**
     * Returns the created instance of {@link Plugin} if it is available.
     *
     * @return the instance if available
     */
    default Optional<?> getInstance() {
        return Optional.empty();
    }

    /**
     * Returns the assigned logger to this {@link Plugin}.
     *
     * @return the assigned logger
     */
    Logger getLogger();
}
