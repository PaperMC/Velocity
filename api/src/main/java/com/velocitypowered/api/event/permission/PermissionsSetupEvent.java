/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.permission;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Called when a {@link PermissionSubject}'s permissions are being setup. This event is typically
 * called for the {@link com.velocitypowered.api.proxy.ConsoleCommandSource} and any
 * {@link com.velocitypowered.api.proxy.Player}s who join the proxy.
 *
 * <p>This event is only called once per subject, on initialisation.</p>
 *
 * <p>
 *   Velocity will wait for this event to finish firing before proceeding further with server
 *   startup (for the console command source) and logins (for players) but it is strongly
 *   recommended to minimize the amount of work that must be done in this event.
 * </p>
 */
@AwaitingEvent
public final class PermissionsSetupEvent {

  private final PermissionSubject subject;
  private final PermissionProvider defaultProvider;
  private PermissionProvider provider;

  /**
   * Constructs a new {@link PermissionsSetupEvent}.
   *
   * @param subject the subject (e.g., player or console) whose permissions are being initialized
   * @param provider the default permission provider used for the subject
   */
  public PermissionsSetupEvent(PermissionSubject subject, PermissionProvider provider) {
    this.subject = Preconditions.checkNotNull(subject, "subject");
    this.provider = this.defaultProvider = Preconditions.checkNotNull(provider, "provider");
  }

  /**
   * Gets the subject whose permissions are being initialized.
   *
   * @return the permission subject
   */
  public PermissionSubject getSubject() {
    return this.subject;
  }

  /**
   * Uses the provider function to obtain a {@link PermissionFunction} for the subject.
   *
   * @param subject the subject
   * @return the obtained permission function
   */
  public PermissionFunction createFunction(PermissionSubject subject) {
    return this.provider.createFunction(subject);
  }

  /**
   * Gets the current {@link PermissionProvider} in use for this subject.
   *
   * @return the permission provider
   */
  public PermissionProvider getProvider() {
    return this.provider;
  }

  /**
   * Sets the {@link PermissionFunction} that should be used for the subject.
   *
   * <p>Specifying <code>null</code> will reset the provider to the default
   * instance given when the event was posted.</p>
   *
   * @param provider the provider
   */
  public void setProvider(@Nullable PermissionProvider provider) {
    this.provider = provider == null ? this.defaultProvider : provider;
  }

  @Override
  public String toString() {
    return "PermissionsSetupEvent{"
        + "subject=" + subject
        + ", defaultProvider=" + defaultProvider
        + ", provider=" + provider
        + '}';
  }
}
