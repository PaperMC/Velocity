package com.velocitypowered.proxy.plugin;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import net.kyori.event.method.MethodEventBus;
import net.kyori.event.method.SimpleMethodEventBus;
import net.kyori.event.method.asm.ASMEventExecutorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;

public class EventManager {
    private final ListMultimap<Object, Object> eventListenersByPlugin = Multimaps
            .synchronizedListMultimap(Multimaps.newListMultimap(new IdentityHashMap<>(), ArrayList::new));
    private final MethodEventBus<Object, Object> bus = new SimpleMethodEventBus<>(new ASMEventExecutorFactory<>());

    public void register(Object plugin, Object listener) {
        eventListenersByPlugin.put(plugin, listener);
        bus.register(listener);
    }

    public void post(Object event) {
        bus.post(event);
    }

    public void unregisterPluginListeners(Object plugin) {
        Collection<Object> listeners = eventListenersByPlugin.removeAll(plugin);
        listeners.forEach(bus::unregister);
    }

    public void unregisterListener(Object plugin, Object listener) {
        eventListenersByPlugin.remove(plugin, listener);
        bus.unregister(listener);
    }
}
