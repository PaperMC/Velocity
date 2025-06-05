package com.velocitypowered.api.event.plugin;

import com.google.common.base.Preconditions;
import com.google.inject.Module;
import com.velocitypowered.api.plugin.PluginContainer;

public final class PluginLoadEvent {

	private final PluginContainer pluginContainer;
	private final Module module;

	public PluginLoadEvent(PluginContainer pluginContainer, Module module) {
		this.pluginContainer = Preconditions.checkNotNull(pluginContainer, "pluginContainer");
		this.module = Preconditions.checkNotNull(module, "module");
	}

	public PluginContainer getPluginContainer() {
		return pluginContainer;
	}

	public Module getModule() {
		return module;
	}
}
