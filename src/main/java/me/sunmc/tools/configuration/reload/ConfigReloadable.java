package me.sunmc.tools.configuration.reload;

import me.sunmc.tools.Tools;
import me.sunmc.tools.configuration.ConfigIdWrapper;
import me.sunmc.tools.configuration.ConfigurationProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Optional;

public interface ConfigReloadable {

    static @NonNull ConfigurationNode reloadRootNode(@NonNull Tools plugin, @NonNull ConfigIdWrapper idWrapper) {
        Optional<ConfigurationProvider> config = plugin.getConfigurationManager().getConfigById(idWrapper);
        if (config.isEmpty()) {
            throw new NullPointerException("Could not find config root node with id: " + idWrapper.getKey());
        }

        ConfigurationProvider configurationProvider = config.get();
        configurationProvider.reload(plugin.getConfigurationManager().getDefaultOptions());
        return configurationProvider.getRootNode();
    }

    void loadConfig(@NonNull Tools plugin);
}