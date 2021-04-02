/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.permission;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Called when a {@link PermissionSubject}'s permissions are being setup.
 *
 * <p>This event is only called once per subject, on initialisation.</p>
 */
public final class PermissionsSetupEvent {

  private final PermissionSubject subject;
  private final PermissionProvider defaultProvider;
  private PermissionProvider provider;

  public PermissionsSetupEvent(PermissionSubject subject, PermissionProvider provider) {
    this.subject = Preconditions.checkNotNull(subject, "subject");
    this.provider = this.defaultProvider = Preconditions.checkNotNull(provider, "provider");
  }

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
