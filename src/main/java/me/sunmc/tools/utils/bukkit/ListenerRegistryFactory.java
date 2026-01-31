package me.sunmc.tools.utils.bukkit;

import me.sunmc.tools.Tools;
import me.sunmc.tools.registry.AutoRegister;
import me.sunmc.tools.registry.RegistryFactory;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Helper class for automatically registering and instantiating all Bukkit {@link Listener} with {@link RegistryFactory}.
 * Listeners are also registered into the {@link PluginManager}.
 */
public class ListenerRegistryFactory {

    private final @NonNull Tools plugin;

    public ListenerRegistryFactory(@NonNull Tools plugin) {
        this.plugin = plugin;
    }

    /**
     * Instantiates and registers all listeners annotated with {@link AutoRegister}.
     */
    public void registerAllListeners() {
        final RegistryFactory registryFactory = this.plugin.getRegistryFactory();
        final PluginManager pluginManager = this.plugin.getServer().getPluginManager();

        for (Class<? extends Listener> listenerClass : registryFactory.getClassesWithRegistryType(Listener.class, Listener.class)) {
            Object listener = registryFactory.createEffectiveInstance(listenerClass);
            if (listener != null) {
                pluginManager.registerEvents((Listener) listener, this.plugin);
            }
        }
    }
}