package com.velocitypowered.api.event;

import com.google.common.base.Preconditions;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * Indicates an event that has a result attached to it.
 */
public interface ResultedEvent<R extends ResultedEvent.Result> {
    /**
     * Returns the result associated with this event.
     * @return the result of this event
     */
    R getResult();

    /**
     * Sets the result of this event. The result must be non-null.
     * @param result the new result
     */
    void setResult(R result);

    /**
     * Represents a result for an event.
     */
    interface Result {
        /**
         * Returns whether or not the event is allowed to proceed. Plugins may choose to skip denied events, and the
         * proxy will respect the result of this method.
         * @return whether or not the event is allowed to proceed
         */
        boolean isAllowed();
    }

    /**
     * A generic "allowed/denied" result.
     */
    final class GenericResult implements Result {
        private static final GenericResult ALLOWED = new GenericResult(true);
        private static final GenericResult DENIED = new GenericResult(false);

        private final boolean allowed;

        private GenericResult(boolean b) {
            this.allowed = b;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        @Override
        public String toString() {
            return allowed ? "allowed" : "denied";
        }

        public static GenericResult allowed() {
            return ALLOWED;
        }

        public static GenericResult denied() {
            return DENIED;
        }
    }

    /**
     * Represents an "allowed/denied" result with a reason allowed for denial.
     */
    final class ComponentResult implements Result {
        private static final ComponentResult ALLOWED = new ComponentResult(true, null);

        private final boolean allowed;
        private final @Nullable Component reason;

        protected ComponentResult(boolean allowed, @Nullable Component reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        public Optional<Component> getReason() {
            return Optional.ofNullable(reason);
        }

        @Override
        public String toString() {
            if (allowed) {
                return "allowed";
            }
            if (reason != null) {
                return "denied: " + ComponentSerializers.PLAIN.serialize(reason);
            }
            return "denied";
        }

        public static ComponentResult allowed() {
            return ALLOWED;
        }

        public static ComponentResult denied(Component reason) {
            Preconditions.checkNotNull(reason, "reason");
            return new ComponentResult(false, reason);
        }
    }
}
