package me.sunmc.tools.configuration.reload;

import me.sunmc.tools.Tools;
import me.sunmc.tools.registry.RegistryFactory;
import me.sunmc.tools.utils.java.LoggerUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

public class ConfigReloader {

    private final Logger logger;

    private final @NonNull Tools plugin;
    private final @NonNull RegistryFactory registryFactory;

    public ConfigReloader(@NonNull Tools plugin) {
        this.logger = LoggerUtil.createLoggerWithIdentifier(plugin, this);
        this.plugin = plugin;
        this.registryFactory = plugin.getRegistryFactory();
    }

    public void reload() {
        this.plugin.getConfigurationManager().reloadConfigurations();

        this.registryFactory.getClassesImplementing(ConfigReloadable.class).forEach(clazz -> {
            ConfigReloadable reloadable = (ConfigReloadable) this.registryFactory.createEffectiveInstance(clazz);
            if (reloadable == null) {
                this.logger.warn("Could not reload class: {}", clazz.getName());
            } else {
                reloadable.loadConfig(this.plugin);
            }
        });
    }
}