package com.velocitypowered.api.command;

import com.google.errorprone.annotations.Immutable;
import java.util.Collection;

// TODO Improve javadoc
/**
 * Contains metadata about a {@link Command}.
 */
@Deprecated
@Immutable
public interface CommandMeta {

  // TODO document

  String getMainAlias();

  Collection<String> getAllAliases();
}
