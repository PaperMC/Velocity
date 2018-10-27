package com.velocitypowered.proxy.plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginManager;
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

    public VelocityEventManager(PluginManager pluginManager) {
        PluginClassLoader cl = new PluginClassLoader(new URL[0]);
        cl.addToClassloaders();
        this.bus = new SimpleEventBus<>(Object.class);
        this.methodAdapter = new SimpleMethodSubscriptionAdapter<>(bus, new ASMEventExecutorFactory<>(cl),
                new VelocityMethodScanner());
        this.pluginManager = pluginManager;
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder()
                .setNameFormat("Velocity Event Executor - #%d").setDaemon(true).build());
    }

    @Override
    public void register(Object plugin, Object listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        if (plugin == listener && registeredListenersByPlugin.containsEntry(plugin, plugin)) {
            throw new IllegalArgumentException("Trying to register the plugin main instance. Velocity already takes care of this for you.");
        }
        registeredListenersByPlugin.put(plugin, listener);
        methodAdapter.register(listener);
    }

    @Override
    @SuppressWarnings("type.argument.type.incompatible")
    public <E> void register(Object plugin, Class<E> eventClass, PostOrder postOrder, EventHandler<E> handler) {
        Preconditions.checkNotNull(plugin, "plugin");
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

        Runnable runEvent = () -> {
            PostResult result = bus.post(event);
            if (!result.exceptions().isEmpty()) {
                logger.error("Some errors occurred whilst posting event {}.", event);
                int i = 0;
                for (Throwable exception : result.exceptions().values()) {
                    logger.error("#{}: \n", ++i, exception);
                }
            }
        };

        CompletableFuture<E> eventFuture = new CompletableFuture<>();
        service.execute(() -> {
            runEvent.run();
            eventFuture.complete(event);
        });
        return eventFuture;
    }

    @Override
    public void unregisterListeners(Object plugin) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        Collection<Object> listeners = registeredListenersByPlugin.removeAll(plugin);
        listeners.forEach(methodAdapter::unregister);
        Collection<EventHandler<?>> handlers = registeredHandlersByPlugin.removeAll(plugin);
        handlers.forEach(handler -> bus.unregister(new KyoriToVelocityHandler<>(handler, PostOrder.LAST)));
    }

    @Override
    public void unregisterListener(Object plugin, Object listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        registeredListenersByPlugin.remove(plugin, listener);
        methodAdapter.unregister(listener);
    }

    @Override
    public <E> void unregister(Object plugin, EventHandler<E> handler) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(handler, "listener");
        registeredHandlersByPlugin.remove(plugin, handler);
        bus.unregister(new KyoriToVelocityHandler<>(handler, PostOrder.LAST));
    }

    public boolean shutdown() throws InterruptedException {
        service.shutdown();
        return service.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static class VelocityMethodScanner implements MethodScanner<Object> {
        @Override
        public boolean shouldRegister(@NonNull Object listener, @NonNull Method method) {
            return method.isAnnotationPresent(Subscribe.class);
        }

        @Override
        public int postOrder(@NonNull Object listener, @NonNull Method method) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) {
                throw new IllegalStateException("Trying to determine post order for listener without @Subscribe annotation");
            }
            return annotation.order().ordinal();
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

        public EventHandler<E> getHandler() {
            return handler;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KyoriToVelocityHandler<?> that = (KyoriToVelocityHandler<?>) o;
            return Objects.equals(handler, that.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(handler);
        }
    }
}
