package com.velocitypowered.proxy.plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginManager;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.kyori.event.EventSubscriber;
import net.kyori.event.PostResult;
import net.kyori.event.SimpleEventBus;
import net.kyori.event.method.MethodScanner;
import net.kyori.event.method.MethodSubscriptionAdapter;
import net.kyori.event.method.SimpleMethodSubscriptionAdapter;
import net.kyori.event.method.asm.ASMEventExecutorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityEventManager implements EventManager {

  private static final Logger logger = LogManager.getLogger(VelocityEventManager.class);

  private final ListMultimap<Object, Object> registeredListenersByPlugin = Multimaps
      .synchronizedListMultimap(Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));
  private final ListMultimap<Object, EventHandler<?>> registeredHandlersByPlugin = Multimaps
      .synchronizedListMultimap(Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));
  private final SimpleEventBus<Object> bus;
  private final MethodSubscriptionAdapter<Object> methodAdapter;
  private final ExecutorService service;
  private final PluginManager pluginManager;

  /**
   * Initializes the Velocity event manager.
   *
   * @param pluginManager a reference to the Velocity plugin manager
   */
  public VelocityEventManager(PluginManager pluginManager) {
    // Expose the event executors to the plugins - required in order for the generated ASM classes
    // to work.
    PluginClassLoader cl = new PluginClassLoader(new URL[0]);
    cl.addToClassloaders();

    // Initialize the event bus.
    this.bus = new SimpleEventBus<Object>(Object.class) {
      @Override
      protected boolean shouldPost(@NonNull Object event, @NonNull EventSubscriber<?> subscriber) {
        // Velocity doesn't use Cancellable or generic events, so we can skip those checks.
        return true;
      }
    };
    this.methodAdapter = new SimpleMethodSubscriptionAdapter<>(bus,
        new ASMEventExecutorFactory<>(cl),
        new VelocityMethodScanner());
    this.pluginManager = pluginManager;
    this.service = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder()
            .setNameFormat("Velocity Event Executor - #%d").setDaemon(true).build());
  }

  private void ensurePlugin(Object plugin) {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(),
        "Specified plugin is not loaded");
  }

  @Override
  public void register(Object plugin, Object listener) {
    ensurePlugin(plugin);
    Preconditions.checkNotNull(listener, "listener");
    if (plugin == listener && registeredListenersByPlugin.containsEntry(plugin, plugin)) {
      throw new IllegalArgumentException("The plugin main instance is automatically registered.");
    }

    for (Method method : listener.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(com.google.common.eventbus.Subscribe.class)) {
        throw new IllegalArgumentException("Method " + listener.getClass().getName() + "#"
          + method.getName() + " has a Guava @Subscribe annotation. Use the Velocity @Subscribe "
          + "annotation instead.");
      }
    }

    registeredListenersByPlugin.put(plugin, listener);
    methodAdapter.register(listener);
  }

  @Override
  @SuppressWarnings("type.argument.type.incompatible")
  public <E> void register(Object plugin, Class<E> eventClass, PostOrder postOrder,
      EventHandler<E> handler) {
    ensurePlugin(plugin);
    Preconditions.checkNotNull(eventClass, "eventClass");
    Preconditions.checkNotNull(postOrder, "postOrder");
    Preconditions.checkNotNull(handler, "listener");
    bus.register(eventClass, new KyoriToVelocityHandler<>(handler, postOrder));
  }

  @Override
  public <E> CompletableFuture<E> fire(E event) {
    if (event == null) {
      throw new NullPointerException("event");
    }
    if (!bus.hasSubscribers(event.getClass())) {
      // Optimization: nobody's listening.
      return CompletableFuture.completedFuture(event);
    }

    CompletableFuture<E> eventFuture = new CompletableFuture<>();
    service.execute(() -> {
      fireEvent(event);
      eventFuture.complete(event);
    });
    return eventFuture;
  }

  private void fireEvent(Object event) {
    PostResult result = bus.post(event);
    if (!result.exceptions().isEmpty()) {
      logger.error("Some errors occurred whilst posting event {}.", event);
      int i = 0;
      for (Throwable exception : result.exceptions().values()) {
        logger.error("#{}: \n", ++i, exception);
      }
    }
  }

  private void unregisterHandler(EventHandler<?> handler) {
    bus.unregister(s -> s instanceof KyoriToVelocityHandler
        && ((KyoriToVelocityHandler<?>) s).handler == handler);
  }

  @Override
  public void unregisterListeners(Object plugin) {
    ensurePlugin(plugin);
    Collection<Object> listeners = registeredListenersByPlugin.removeAll(plugin);
    listeners.forEach(methodAdapter::unregister);
    Collection<EventHandler<?>> handlers = registeredHandlersByPlugin.removeAll(plugin);
    handlers.forEach(this::unregisterHandler);
  }

  @Override
  public void unregisterListener(Object plugin, Object listener) {
    ensurePlugin(plugin);
    Preconditions.checkNotNull(listener, "listener");
    registeredListenersByPlugin.remove(plugin, listener);
    methodAdapter.unregister(listener);
  }

  @Override
  public <E> void unregister(Object plugin, EventHandler<E> handler) {
    ensurePlugin(plugin);
    Preconditions.checkNotNull(handler, "listener");
    registeredHandlersByPlugin.remove(plugin, handler);
    unregisterHandler(handler);
  }

  public boolean shutdown() throws InterruptedException {
    service.shutdown();
    return service.awaitTermination(10, TimeUnit.SECONDS);
  }

  public void fireShutdownEvent() {
    // We shut down the proxy already, so the fact this executes in the main thread is irrelevant.
    fireEvent(new ProxyShutdownEvent());
  }

  private static class VelocityMethodScanner implements MethodScanner<Object> {

    @Override
    public boolean shouldRegister(@NonNull Object listener, @NonNull Method method) {
      return method.isAnnotationPresent(Subscribe.class);
    }

    @Override
    public int postOrder(@NonNull Object listener, @NonNull Method method) {
      return method.getAnnotation(Subscribe.class).order().ordinal();
    }

    @Override
    public boolean consumeCancelledEvents(@NonNull Object listener, @NonNull Method method) {
      return true;
    }
  }

  private static class KyoriToVelocityHandler<E> implements EventSubscriber<E> {

    private final EventHandler<E> handler;
    private final int postOrder;

    private KyoriToVelocityHandler(EventHandler<E> handler, PostOrder postOrder) {
      this.handler = handler;
      this.postOrder = postOrder.ordinal();
    }

    @Override
    public void invoke(@NonNull E event) {
      handler.execute(event);
    }

    @Override
    public int postOrder() {
      return postOrder;
    }
  }
}
