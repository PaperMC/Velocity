package com.velocitypowered.api.plugin.meta;

import javax.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * Represents a dependency on another plugin.
 */
public final class PluginDependency {
    private final String id;
    @Nullable private final String version;

    private final boolean optional;

    public PluginDependency(String id, @Nullable String version, boolean optional) {
        this.id = checkNotNull(id, "id");
        checkArgument(!id.isEmpty(), "id cannot be empty");
        this.version = emptyToNull(version);
        this.optional = optional;
    }

    /**
     * Returns the plugin ID of this {@link PluginDependency}
     *
     * @return the plugin ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the version this {@link PluginDependency} should match.
     *
     * @return an {@link Optional} with the plugin version, may be empty
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Returns whether the dependency is optional for the plugin to work
     * correctly.
     *
     * @return true if dependency is optional
     */
    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginDependency that = (PluginDependency) o;
        return optional == that.optional &&
                Objects.equals(id, that.id) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, optional);
    }

    @Override
    public String toString() {
        return "PluginDependency{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", optional=" + optional +
                '}';
    }
}
