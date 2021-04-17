/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.proxy.server.QueryResponse;
import java.net.InetAddress;

/**
 * This event is fired if proxy is getting queried over GS4 Query protocol.
 */
public interface ProxyQueryEvent {

  /**
   * Returns the kind of query the remote client is performing.
   *
   * @return query type
   */
  QueryType getQueryType();

  /**
   * Get the address of the client that sent this query.
   *
   * @return querier address
   */
  InetAddress getQuerierAddress();

  /**
   * Returns the current query response.
   *
   * @return the current query response
   */
  QueryResponse getResponse();

  /**
   * Sets a new query response.
   *
   * @param response the new non-null query response
   */
  void setResponse(QueryResponse response);

  /**
   * Represents the type of query the client is asking for.
   */
  enum QueryType {
    /**
     * Basic query asks only a subset of information, such as hostname, game type (hardcoded to
     * <pre>MINECRAFT</pre>), map, current players, max players, proxy port and proxy hostname.
     */
    BASIC,

    /**
     * Full query asks pretty much everything present on this event (only hardcoded values cannot be
     * modified here).
     */
    FULL
  }
}
