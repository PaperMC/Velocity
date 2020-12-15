package com.velocitypowered.proxy.event;

import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;

public class VelocityEventManager implements EventManager {

  private static final Logger logger = LogManager.getLogger(VelocityEventManager.class);

  private static final MethodHandles.Lookup methodHandlesLookup = MethodHandles.lookup();
  private static final LambdaType<UntargetedEventHandler> untargetedHandlerType =
      LambdaType.of(UntargetedEventHandler.class);
  private static final LambdaType<UntargetedEventHandler.Void> untargetedVoidHandlerType =
      LambdaType.of(UntargetedEventHandler.Void.class);

  private static final Comparator<HandlerRegistration> handlerComparator =
      Comparator.comparingInt(o -> o.order);

  private final ExecutorService asyncExecutor;
  private final PluginManager pluginManager;

  private final Multimap<Class<?>, HandlerRegistration> handlersByType = HashMultimap.create();
  private final LoadingCache<Class<?>, @Nullable HandlersCache> handlersCache =
      Caffeine.newBuilder().build(this::bakeHandlers);

  private final LoadingCache<Method, UntargetedEventHandler> untargetedMethodHandlers =
      Caffeine.newBuilder().weakValues().build(this::buildUntargetedMethodHandler);

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  /**
   * Initializes the Velocity event manager.
   *
   * @param pluginManager a reference to the Velocity plugin manager
   */
  public VelocityEventManager(final PluginManager pluginManager) {
    this.pluginManager = pluginManager;
    this.asyncExecutor = Executors
        .newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder()
            .setNameFormat("Velocity Async Event Executor - #%d").setDaemon(true).build());
  }

  /**
   * Represents the registration of a single {@link EventHandler}.
   */
  static final class HandlerRegistration {

    final PluginContainer plugin;
    final short order;
    final Class<?> eventType;
    final EventHandler<Object> handler;
    final AsyncType asyncType;

    /**
     * The instance of the {@link EventHandler} or the listener
     * instance that was registered.
     */
    final Object instance;

    public HandlerRegistration(final PluginContainer plugin, final short order,
        final Class<?> eventType, final Object instance, final EventHandler<Object> handler,
        final AsyncType asyncType) {
      this.plugin = plugin;
      this.order = order;
      this.eventType = eventType;
      this.instance = instance;
      this.handler = handler;
      this.asyncType = asyncType;
    }
  }

  enum AsyncType {
    /**
     * The complete event will be handled on an async thread.
     */
    ALWAYS,
    /**
     * The event will initially start on the netty thread, and possibly
     * switch over to an async thread.
     */
    SOMETIMES,
    /**
     * The event will never run async, everything is handled on
     * the netty thread.
     */
    NEVER
  }

  static final class HandlersCache {

    final AsyncType asyncType;
    final HandlerRegistration[] handlers;

    HandlersCache(final HandlerRegistration[] handlers, final AsyncType asyncType) {
      this.asyncType = asyncType;
      this.handlers = handlers;
    }
  }

  private static List<Class<?>> getEventTypes(final Class<?> eventType) {
    return TypeToken.of(eventType).getTypes().rawTypes().stream()
        .filter(type -> type != Object.class)
        .collect(Collectors.toList());
  }

  private @Nullable HandlersCache bakeHandlers(final Class<?> eventType) {
    final List<HandlerRegistration> baked = new ArrayList<>();
    final List<Class<?>> types = getEventTypes(eventType);

    lock.readLock().lock();
    try {
      for (final Class<?> type : types) {
        baked.addAll(handlersByType.get(type));
      }
    } finally {
      lock.readLock().unlock();
    }

    if (baked.isEmpty()) {
      return null;
    }

    baked.sort(handlerComparator);

    final AsyncType asyncType;
    if (baked.stream().anyMatch(reg -> reg.asyncType == AsyncType.ALWAYS)) {
      asyncType = AsyncType.ALWAYS;
    } else if (baked.stream().anyMatch(reg -> reg.asyncType == AsyncType.SOMETIMES)) {
      asyncType = AsyncType.SOMETIMES;
    } else {
      asyncType = AsyncType.NEVER;
    }

    return new HandlersCache(baked.toArray(new HandlerRegistration[0]), asyncType);
  }

  /**
   * Creates an {@link UntargetedEventHandler} for the given {@link Method}. This essentially
   * implements the {@link UntargetedEventHandler} (or the no async task variant) to invoke the
   * target method. The implemented class is defined in the same package as the declaring class.
   * The {@link UntargetedEventHandler} interface must be public so the implementation can access
   * it.
   *
   * @param method The method to generate an untargeted handler for
   * @return The untargeted handler
   */
  private UntargetedEventHandler buildUntargetedMethodHandler(final Method method)
      throws IllegalAccessException {
    final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
        method.getDeclaringClass(), methodHandlesLookup);
    final LambdaType<? extends UntargetedEventHandler> type;
    if (EventTask.class.isAssignableFrom(method.getReturnType())) {
      type = untargetedHandlerType;
    } else {
      type = untargetedVoidHandlerType;
    }
    return LambdaFactory.create(type.defineClassesWith(lookup), lookup.unreflect(method));
  }

  static final class MethodHandlerInfo {

    final Method method;
    final AsyncType asyncType;
    final @Nullable Class<?> eventType;
    final short order;
    final @Nullable String errors;

    private MethodHandlerInfo(final Method method, final AsyncType asyncType,
        final @Nullable Class<?> eventType, final short order, final @Nullable String errors) {
      this.method = method;
      this.asyncType = asyncType;
      this.eventType = eventType;
      this.order = order;
      this.errors = errors;
    }
  }

  private void collectMethods(final Class<?> targetClass,
      final Map<String, MethodHandlerInfo> collected) {
    for (final Method method : targetClass.getDeclaredMethods()) {
      final Subscribe subscribe = method.getAnnotation(Subscribe.class);
      if (subscribe == null) {
        continue;
      }
      String key = method.getName()
          + "("
          + Arrays.stream(method.getParameterTypes())
              .map(Class::getName)
              .collect(Collectors.joining(","))
          + ")";
      if (Modifier.isPrivate(method.getModifiers())) {
        key = targetClass.getName() + "$" + key;
      }
      if (collected.containsKey(key)) {
        continue;
      }
      final Set<String> errors = new HashSet<>();
      if (Modifier.isStatic(method.getModifiers())) {
        errors.add("method must not be static");
      }
      if (Modifier.isAbstract(method.getModifiers())) {
        errors.add("method must not be abstract");
      }
      Class<?> eventType = null;
      if (method.getParameterCount() != 1) {
        errors.add("method must have a single parameter which is the event");
      } else {
        eventType = method.getParameterTypes()[0];
      }
      AsyncType asyncType = AsyncType.NEVER;
      final Class<?> returnType = method.getReturnType();
      if (returnType != void.class
          && returnType != EventTask.class
          && returnType != EventTask.Basic.class
          && returnType != EventTask.WithContinuation.class) {
        errors.add("method return type must be void, AsyncTask, "
            + "AsyncTask.Basic or AsyncTask.WithContinuation");
      } else if (returnType == EventTask.class
          || returnType == EventTask.Basic.class
          || returnType == EventTask.WithContinuation.class) {
        asyncType = AsyncType.SOMETIMES;
      }
      if (subscribe.async()) {
        asyncType = AsyncType.ALWAYS;
      }
      final short order = subscribe.order();
      final String errorsJoined = errors.isEmpty() ? null : String.join(",", errors);
      collected.put(key, new MethodHandlerInfo(method, asyncType, eventType, order, errorsJoined));
    }
    final Class<?> superclass = targetClass.getSuperclass();
    if (superclass != Object.class) {
      collectMethods(superclass, collected);
    }
  }

  private PluginContainer ensurePlugin(final Object plugin) {
    requireNonNull(plugin, "plugin");
    return pluginManager.fromInstance(plugin)
        .orElseThrow(() -> new IllegalArgumentException("Specified plugin is not loaded"));
  }

  private void register(final List<HandlerRegistration> registrations) {
    lock.writeLock().lock();
    try {
      for (final HandlerRegistration registration : registrations) {
        handlersByType.put(registration.eventType, registration);
      }
    } finally {
      lock.writeLock().unlock();
    }
    // Invalidate all the affected event subtypes
    handlersCache.invalidateAll(registrations.stream()
        .flatMap(registration -> getEventTypes(registration.eventType).stream())
        .distinct()
        .collect(Collectors.toList()));
  }

  @Override
  public void register(final Object plugin, final Object listener) {
    requireNonNull(listener, "listener");
    final PluginContainer pluginContainer = ensurePlugin(plugin);
    if (plugin == listener) {
      throw new IllegalArgumentException("The plugin main instance is automatically registered.");
    }

    final Class<?> targetClass = listener.getClass();
    final Map<String, MethodHandlerInfo> collected = new HashMap<>();
    collectMethods(targetClass, collected);

    final List<HandlerRegistration> registrations = new ArrayList<>();
    for (final MethodHandlerInfo info : collected.values()) {
      if (info.errors != null) {
        logger.info("Invalid listener method {} in {}: {}",
            info.method.getName(), info.method.getDeclaringClass().getName(), info.errors);
        continue;
      }
      final UntargetedEventHandler untargetedHandler =
          untargetedMethodHandlers.get(info.method);
      requireNonNull(untargetedHandler);
      final EventHandler<Object> handler = event -> untargetedHandler.execute(listener, event);
      registrations.add(new HandlerRegistration(pluginContainer, info.order,
          info.eventType, listener, handler, info.asyncType));
    }

    register(registrations);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E> void register(final Object plugin, final Class<E> eventClass,
      final short order, final EventHandler<E> handler) {
    final PluginContainer pluginContainer = ensurePlugin(plugin);
    requireNonNull(eventClass, "eventClass");
    requireNonNull(handler, "handler");

    final HandlerRegistration registration = new HandlerRegistration(pluginContainer, order,
        eventClass, handler, (EventHandler<Object>) handler, AsyncType.SOMETIMES);
    register(Collections.singletonList(registration));
  }

  @Override
  public void unregisterListeners(final Object plugin) {
    final PluginContainer pluginContainer = ensurePlugin(plugin);
    unregisterIf(registration -> registration.plugin == pluginContainer);
  }

  @Override
  public void unregisterListener(final Object plugin, final Object handler) {
    final PluginContainer pluginContainer = ensurePlugin(plugin);
    requireNonNull(handler, "handler");
    unregisterIf(registration ->
        registration.plugin == pluginContainer && registration.handler == handler);
  }

  @Override
  public <E> void unregister(final Object plugin, final EventHandler<E> handler) {
    unregisterListener(plugin, handler);
  }

  private void unregisterIf(final Predicate<HandlerRegistration> predicate) {
    final List<HandlerRegistration> removed = new ArrayList<>();
    lock.writeLock().lock();
    try {
      final Iterator<HandlerRegistration> it = handlersByType.values().iterator();
      while (it.hasNext()) {
        final HandlerRegistration registration = it.next();
        if (predicate.test(registration)) {
          it.remove();
          removed.add(registration);
        }
      }
    } finally {
      lock.writeLock().unlock();
    }

    // Invalidate all the affected event subtypes
    handlersCache.invalidateAll(removed.stream()
        .flatMap(registration -> getEventTypes(registration.eventType).stream())
        .distinct()
        .collect(Collectors.toList()));
  }

  @Override
  public void fireAndForget(final Object event) {
    requireNonNull(event, "event");
    final HandlersCache handlersCache = this.handlersCache.get(event.getClass());
    if (handlersCache == null) {
      // Optimization: nobody's listening.
      return;
    }
    fire(null, event, handlersCache);
  }

  @Override
  public <E> CompletableFuture<E> fire(final E event) {
    requireNonNull(event, "event");
    final HandlersCache handlersCache = this.handlersCache.get(event.getClass());
    if (handlersCache == null) {
      // Optimization: nobody's listening.
      return CompletableFuture.completedFuture(event);
    }
    final CompletableFuture<E> future = new CompletableFuture<>();
    fire(future, event, handlersCache);
    return future;
  }

  private <E> void fire(final @Nullable CompletableFuture<E> future,
      final E event, final HandlersCache handlersCache) {
    if (handlersCache.asyncType == AsyncType.ALWAYS) {
      // We already know that the event needs to be handled async, so
      // execute it asynchronously from the start
      asyncExecutor.execute(() -> fire(future, event, 0, true, handlersCache.handlers));
    } else {
      fire(future, event, 0, false, handlersCache.handlers);
    }
  }

  private <E> void fire(final @Nullable CompletableFuture<E> future, final E event,
      final int offset, final boolean currentlyAsync, final HandlerRegistration[] registrations) {
    for (int i = offset; i < registrations.length; i++) {
      final HandlerRegistration registration = registrations[i];
      try {
        final EventTask eventTask = registration.handler.execute(event);
        if (eventTask == null) {
          continue;
        }
        if (eventTask instanceof EventTask.WithContinuation) {
          final EventTask.WithContinuation withContinuation =
              (EventTask.WithContinuation) eventTask;
          final int index = i;
          final Continuation continuation = new Continuation() {
            private final AtomicBoolean resumed = new AtomicBoolean();

            @Override
            public void resume() {
              resume(null);
            }

            void resume(final @Nullable Throwable exception) {
              // Only allow the continuation to be resumed once
              if (!resumed.compareAndSet(false, true)) {
                throw new IllegalStateException("The continuation can only be resumed once.");
              }
              if (exception != null) {
                logHandlerException(registration, exception);
              }
              if (index + 1 == registrations.length) {
                // Optimization: don't schedule a task just to complete the future
                if (future != null) {
                  future.complete(event);
                }
                return;
              }
              asyncExecutor.execute(() -> fire(future, event, index + 1, true, registrations));
            }

            @Override
            public void resumeWithException(final Throwable exception) {
              resume(requireNonNull(exception, "exception"));
            }
          };
          final Runnable task = () -> {
            try {
              withContinuation.run(continuation);
            } catch (final Throwable t) {
              continuation.resumeWithException(t);
            }
          };
          if (currentlyAsync || !eventTask.requiresAsync()) {
            task.run();
          } else {
            asyncExecutor.execute(task);
          }
          // fire will continue in another thread once the async task is
          // executed and the continuation is resumed
          return;
        } else {
          final EventTask.Basic basic = (EventTask.Basic) eventTask;
          if (currentlyAsync || !basic.requiresAsync()) {
            // We are already async or we don't need async, so we can just run the
            // task and continue with the next handler
            basic.run();
          } else {
            final int index = i;
            // We are not yet in an async context, so the async task needs to be scheduled
            // to the async executor, the event handling will continue on an async thread.
            asyncExecutor.execute(() -> {
              try {
                basic.run();
              } catch (final Throwable t) {
                logHandlerException(registration, t);
              }
              fire(future, event, index + 1, true, registrations);
            });
            return; // fire will continue in another thread once the async task is completed
          }
        }
      } catch (final Throwable t) {
        logHandlerException(registration, t);
      }
    }
    if (future != null) {
      future.complete(event);
    }
  }

  private static void logHandlerException(
      final HandlerRegistration registration, final Throwable t) {
    logger.error("Couldn't pass {} to {}", registration.eventType.getSimpleName(),
        registration.plugin.getDescription().getId(), t);
  }

  public boolean shutdown() throws InterruptedException {
    asyncExecutor.shutdown();
    return asyncExecutor.awaitTermination(10, TimeUnit.SECONDS);
  }

  public ExecutorService getAsyncExecutor() {
    return asyncExecutor;
  }
}
