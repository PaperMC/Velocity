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
 *
 * @param <R> the type of result associated with the event
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

    /**
     * Returns a result indicating the event is allowed to proceed.
     *
     * @return an allowed {@link GenericResult}
     */
    public static GenericResult allowed() {
      return ALLOWED;
    }

    /**
     * Returns a result indicating the event is denied.
     *
     * @return a denied {@link GenericResult}
     */
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

    /**
     * Represents an allowed or denied result that may include a denial reason.
     *
     * @param status whether the result is allowed
     * @param reason the component explaining why the result was denied, or {@code null}
     */
    protected ComponentResult(boolean status, @Nullable Component reason) {
      this.status = status;
      this.reason = reason;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    /**
     * Returns the denial reason component, if present.
     *
     * @return an {@link Optional} containing the reason component if the result is denied
     */
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

    /**
     * Returns a result indicating the event is allowed to proceed.
     *
     * @return an allowed {@link ComponentResult}
     */
    public static ComponentResult allowed() {
      return ALLOWED;
    }

    /**
     * Returns a result indicating the event is denied, with the given reason component.
     *
     * @param reason the denial reason to show
     * @return a denied {@link ComponentResult}
     * @throws NullPointerException if the reason is null
     */
    public static ComponentResult denied(Component reason) {
      Preconditions.checkNotNull(reason, "reason");
      return new ComponentResult(false, reason);
    }
  }
}
