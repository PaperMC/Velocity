package com.velocitypowered.proxy.plugin.loader.java;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VelocityPluginModule implements Module {
    private final JavaVelocityPluginCandidate description;

    public VelocityPluginModule(JavaVelocityPluginCandidate description) {
        this.description = description;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Logger.class).toInstance(LoggerFactory.getLogger(description.getId()));
        binder.bind(ProxyServer.class).toInstance(VelocityServer.getServer());
    }
}
