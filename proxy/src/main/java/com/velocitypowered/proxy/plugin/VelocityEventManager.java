package com.velocitypowered.proxy.plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.PluginManager;
import net.kyori.event.EventSubscriber;
import net.kyori.event.PostOrder;
import net.kyori.event.PostResult;
import net.kyori.event.method.*;
import net.kyori.event.method.asm.ASMEventExecutorFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class VelocityEventManager implements EventManager {
    private static final Logger logger = LogManager.getLogger(VelocityEventManager.class);

    private final ListMultimap<Object, Object> registeredListenersByPlugin = Multimaps
            .synchronizedListMultimap(Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));
    private final ListMultimap<Object, EventHandler<?>> registeredHandlersByPlugin = Multimaps
            .synchronizedListMultimap(Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));
    private final VelocityEventBus bus = new VelocityEventBus(
            new ASMEventExecutorFactory<>(new PluginClassLoader(new URL[0])),
            new VelocityMethodScanner());
    private final ExecutorService service;
    private final PluginManager pluginManager;

    public VelocityEventManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("Velocity Event Executor - #%d").setDaemon(true).build());
    }

    @Override
    public void register(@Nonnull Object plugin, @Nonnull Object listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        registeredListenersByPlugin.put(plugin, listener);
        bus.register(listener);
    }

    @Override
    public <E> void register(@Nonnull Object plugin, @Nonnull Class<E> eventClass, @Nonnull EventHandler<E> listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(eventClass, "eventClass");
        Preconditions.checkNotNull(listener, "listener");
        bus.register(eventClass, new KyoriToVelocityHandler<>(listener));
    }

    @Override
    public CompletableFuture<Object> post(@Nonnull Object event) {
        if (!bus.hasSubscribers(event.getClass())) {
            // Optimization: nobody's listening.
            return CompletableFuture.completedFuture(event);
        }

        CompletableFuture<Object> eventFuture = new CompletableFuture<>();
        service.execute(() -> {
            PostResult result = bus.post(event);
            if (!result.exceptions().isEmpty()) {
                logger.error("Some errors occurred whilst posting event {}.", event);
                for (int i = 0; i < result.exceptions().size(); i++) {
                    logger.error("#{}: \n", i + 1, result.exceptions().get(i));
                }
            }
            eventFuture.complete(event);
        });
        return eventFuture;
    }

    @Override
    public void unregisterPluginListeners(@Nonnull Object plugin) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        Collection<Object> listeners = registeredListenersByPlugin.removeAll(plugin);
        listeners.forEach(bus::unregister);
        Collection<EventHandler<?>> handlers = registeredHandlersByPlugin.removeAll(plugin);
        handlers.forEach(bus::unregister);
    }

    @Override
    public void unregisterListener(@Nonnull Object plugin, @Nonnull Object listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");
        Preconditions.checkArgument(pluginManager.fromInstance(plugin).isPresent(), "Specified plugin is not loaded");
        registeredListenersByPlugin.remove(plugin, listener);
        bus.unregister(listener);
    }

    @Override
    public <E> void unregister(@Nonnull Object plugin, @Nonnull EventHandler<E> listener) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(listener, "listener");
        registeredHandlersByPlugin.remove(plugin, listener);
        bus.unregister(listener);
    }

    public void shutdown() throws InterruptedException {
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static class VelocityEventBus extends SimpleMethodEventBus<Object, Object> {
        public VelocityEventBus(EventExecutor.@NonNull Factory<Object, Object> factory, @NonNull MethodScanner<Object> methodScanner) {
            super(factory, methodScanner);
        }

        public void unregister(EventHandler<?> handler) {
            this.unregisterMatching(s -> s instanceof KyoriToVelocityHandler && ((KyoriToVelocityHandler<?>) s).getHandler().equals(handler));
        }
    }

    private static class VelocityMethodScanner implements MethodScanner<Object> {
        @Override
        public boolean shouldRegister(@NonNull Object listener, @NonNull Method method) {
            return method.isAnnotationPresent(Subscribe.class);
        }

        @Override
        public @NonNull PostOrder postOrder(@NonNull Object listener, @NonNull Method method) {
            return PostOrder.NORMAL; // TODO: Allow customizing his
        }

        @Override
        public boolean consumeCancelledEvents(@NonNull Object listener, @NonNull Method method) {
            return true;
        }
    }

    private static class KyoriToVelocityHandler<E> implements EventSubscriber<E> {
        private final EventHandler<E> handler;

        private KyoriToVelocityHandler(EventHandler<E> handler) {
            this.handler = handler;
        }

        @Override
        public void invoke(@NonNull E event) throws Throwable {
            handler.execute(event);
        }

        public EventHandler<E> getHandler() {
            return handler;
        }
    }
}
