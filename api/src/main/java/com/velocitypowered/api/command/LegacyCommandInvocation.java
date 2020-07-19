package com.velocitypowered.api.command;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Contains the invocation data for a {@link LegacyCommand}.
 */
@Deprecated
public interface LegacyCommandInvocation extends CommandInvocation<String @NonNull []> {

}
