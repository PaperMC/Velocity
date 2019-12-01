package com.velocitypowered.proxy.network.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ThreadExecutorMap;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class DnsAddressResolverGroupNameResolverAdapter extends InetNameResolver {

  private final DnsAddressResolverGroup resolverGroup;
  private final EventLoopGroup group;

  /**
   * Creates a DnsAddressResolverGroupNameResolverAdapter.
   * @param resolverGroup the resolver group to use
   * @param group the event loop group
   */
  public DnsAddressResolverGroupNameResolverAdapter(
      DnsAddressResolverGroup resolverGroup, EventLoopGroup group) {
    super(ImmediateEventExecutor.INSTANCE);
    this.resolverGroup = resolverGroup;
    this.group = group;
  }

  @Override
  protected void doResolve(String inetHost, Promise<InetAddress> promise) throws Exception {
    EventExecutor executor = this.findExecutor();
    resolverGroup.getResolver(executor).resolve(InetSocketAddress.createUnresolved(inetHost, 17))
        .addListener((FutureListener<InetSocketAddress>) future -> {
          if (future.isSuccess()) {
            promise.trySuccess(future.getNow().getAddress());
          } else {
            promise.tryFailure(future.cause());
          }
        });
  }

  @Override
  protected void doResolveAll(String inetHost, Promise<List<InetAddress>> promise)
      throws Exception {
    EventExecutor executor = this.findExecutor();
    resolverGroup.getResolver(executor).resolveAll(InetSocketAddress.createUnresolved(inetHost, 17))
        .addListener((FutureListener<List<InetSocketAddress>>) future -> {
          if (future.isSuccess()) {
            List<InetAddress> addresses = new ArrayList<>(future.getNow().size());
            for (InetSocketAddress address : future.getNow()) {
              addresses.add(address.getAddress());
            }
            promise.trySuccess(addresses);
          } else {
            promise.tryFailure(future.cause());
          }
        });
  }

  private EventExecutor findExecutor() {
    EventExecutor current = ThreadExecutorMap.currentExecutor();
    return current == null ? group.next() : current;
  }
}
