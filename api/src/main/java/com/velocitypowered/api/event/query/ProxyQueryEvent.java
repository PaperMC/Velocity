/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.query;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.server.QueryResponse;
import java.net.InetAddress;

/**
 * This event is fired if proxy is getting queried over GS4 Query protocol. Velocity will wait on
 * this event to fire before providing a response to the client.
 */
@AwaitingEvent
public final class ProxyQueryEvent {

  private final QueryType queryType;
  private final InetAddress querierAddress;
  private QueryResponse response;

  /**
   * Creates a new event.
   *
   * @param queryType the type of query
   * @param querierAddress the remote address for the query
   * @param response the current query response
   */
  public ProxyQueryEvent(QueryType queryType, InetAddress querierAddress, QueryResponse response) {
    this.queryType = Preconditions.checkNotNull(queryType, "queryType");
    this.querierAddress = Preconditions.checkNotNull(querierAddress, "querierAddress");
    this.response = Preconditions.checkNotNull(response, "response");
  }

  /**
   * Returns the kind of query the remote client is performing.
   *
   * @return query type
   */
  public QueryType getQueryType() {
    return queryType;
  }

  /**
   * Get the address of the client that sent this query.
   *
   * @return querier address
   */
  public InetAddress getQuerierAddress() {
    return querierAddress;
  }

  /**
   * Returns the current query response.
   *
   * @return the current query response
   */
  public QueryResponse getResponse() {
    return response;
  }

  /**
   * Sets a new query response.
   *
   * @param response the new non-null query response
   */
  public void setResponse(QueryResponse response) {
    this.response = Preconditions.checkNotNull(response, "response");
  }

  @Override
  public String toString() {
    return "ProxyQueryEvent{"
        + "queryType=" + queryType
        + ", querierAddress=" + querierAddress
        + ", response=" + response
        + '}';
  }

  /**
   * Represents the type of query the client is asking for.
   */
  public enum QueryType {
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
