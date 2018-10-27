package com.velocitypowered.api.event.query;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.server.QueryResponse;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.InetAddress;

/**
 * This event is fired if proxy is getting queried over GS4 Query protocol
 */
public final class ProxyQueryEvent {
    private final QueryType queryType;
    private final InetAddress querierAddress;
    private QueryResponse response;

    public ProxyQueryEvent(QueryType queryType, InetAddress querierAddress, QueryResponse response) {
        this.queryType = Preconditions.checkNotNull(queryType, "queryType");
        this.querierAddress = Preconditions.checkNotNull(querierAddress, "querierAddress");
        this.response = Preconditions.checkNotNull(response, "response");
    }

    /**
     * Get query type
     * @return query type
     */
    @NonNull
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * Get querier address
     * @return querier address
     */
    @NonNull
    public InetAddress getQuerierAddress() {
        return querierAddress;
    }

    /**
     * Get query response
     * @return query response
     */
    @NonNull
    public QueryResponse getResponse() {
        return response;
    }

    /**
     * Set query response
     * @param response query response
     */
    public void setResponse(@NonNull QueryResponse response) {
        this.response = Preconditions.checkNotNull(response, "response");
    }

    @Override
    public String toString() {
        return "ProxyQueryEvent{" +
                "queryType=" + queryType +
                ", querierAddress=" + querierAddress +
                ", response=" + response +
                '}';
    }

    /**
     * The type of query
     */
    public enum QueryType {
        /**
         * Basic query asks only a subset of information, such as hostname, game type (hardcoded to <pre>MINECRAFT</pre>), map,
         * current players, max players, proxy port and proxy hostname
         */
        BASIC,

        /**
         * Full query asks pretty much everything present on this event (only hardcoded values cannot be modified here).
         */
        FULL
        ;
    }
}
