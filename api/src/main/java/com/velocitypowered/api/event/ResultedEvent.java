/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event;

import com.google.common.base.Preconditions;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Indicates an event that has a result attached to it.
 */
public interface ResultedEvent<R extends ResultedEvent.Result> {

  /**
   * Returns the result associated with this event.
   *
   * @return the result of this event
   */
  R getResult();

  /**
   * Sets the result of this event. The result must be non-null.
   *
   * @param result the new result
   */
  void setResult(R result);

  /**
   * Represents a result for an event.
   */
  interface Result {

    /**
     * Returns whether or not the event is allowed to proceed. Plugins may choose to skip denied
     * events, and the proxy will respect the result of this method.
     *
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

    private final boolean status;

    private GenericResult(boolean b) {
      this.status = b;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    @Override
    public String toString() {
      return status ? "allowed" : "denied";
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

    private final boolean status;
    private final @Nullable Component reason;

    protected ComponentResult(boolean status, @Nullable Component reason) {
      this.status = status;
      this.reason = reason;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    public Optional<Component> getReasonComponent() {
      return Optional.ofNullable(reason);
    }

    @Override
    public String toString() {
      if (status) {
        return "allowed";
      }
      if (reason != null) {
        return "denied: " + PlainTextComponentSerializer.plainText().serialize(reason);
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
