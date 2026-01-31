package me.sunmc.tools.configuration.watcher;

import me.sunmc.tools.Tools;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.configuration.ConfigurationManager;
import me.sunmc.tools.configuration.reload.ConfigReloadable;
import me.sunmc.tools.registry.AutoRegister;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Watches configuration files for changes and automatically reloads them.
 * This component monitors the plugin's data folder for file modifications
 * and triggers reload events when changes are detected.
 *
 * <p>Features:
 * - Automatic file change detection
 * - Configurable watch interval
 * - Support for multiple configuration files
 * - Integration with ConfigReloadable components
 * - Debouncing to prevent multiple reloads
 *
 * @version 1.0.0
 */
@AutoRegister(Component.class)
public class ConfigWatcher implements Component {

    private final @NonNull Tools plugin;
    private final @NonNull Map<String, Long> lastModified;
    private @NonNull WatchService watchService;
    private @NonNull Thread watchThread;
    private volatile boolean running = false;
    private long debounceMs = 1000; // 1 second debounce

    public ConfigWatcher(@NonNull Tools plugin) {
        this.plugin = plugin;
        this.lastModified = new HashMap<>();
    }

    @Override
    public void onEnable() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.startWatching();
            Tools.LOG.info("Configuration file watcher enabled");
        } catch (IOException e) {
            Tools.LOG.error("Failed to initialize configuration watcher", e);
        }
    }

    @Override
    public void onDisable() {
        this.stopWatching();
        try {
            if (this.watchService != null) {
                this.watchService.close();
            }
        } catch (IOException e) {
            Tools.LOG.error("Error closing watch service", e);
        }
    }

    /**
     * Sets the debounce time in milliseconds.
     * This prevents multiple reloads when a file is saved multiple times quickly.
     *
     * @param debounceMs The debounce time in milliseconds.
     */
    public void setDebounceTime(long debounceMs) {
        this.debounceMs = debounceMs;
    }

    /**
     * Starts watching the configuration directory.
     */
    private void startWatching() {
        Path dataFolder = this.plugin.getDataFolder().toPath();

        try {
            dataFolder.register(
                    this.watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE
            );
        } catch (IOException e) {
            Tools.LOG.error("Failed to register watch service", e);
            return;
        }

        this.running = true;
        this.watchThread = new Thread(this::watchLoop, "ConfigWatcher");
        this.watchThread.setDaemon(true);
        this.watchThread.start();
    }

    /**
     * Stops watching for configuration changes.
     */
    private void stopWatching() {
        this.running = false;
        if (this.watchThread != null && this.watchThread.isAlive()) {
            this.watchThread.interrupt();
        }
    }

    /**
     * Main watch loop that monitors for file changes.
     */
    private void watchLoop() {
        while (this.running) {
            WatchKey key;
            try {
                key = this.watchService.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                break;
            }

            if (key == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path fileName = pathEvent.context();
                String fileNameStr = fileName.toString();

                // Only process .yml files
                if (!fileNameStr.endsWith(".yml") && !fileNameStr.endsWith(".yaml")) {
                    continue;
                }

                // Check debounce
                long now = System.currentTimeMillis();
                Long lastMod = this.lastModified.get(fileNameStr);
                if (lastMod != null && (now - lastMod) < this.debounceMs) {
                    continue;
                }

                this.lastModified.put(fileNameStr, now);

                // Schedule reload on main thread
                this.plugin.getSchedulerAdapter().executeSync(() -> this.reloadConfig(fileNameStr));
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    /**
     * Reloads a specific configuration file.
     *
     * @param fileName The file name that was modified.
     */
    private void reloadConfig(@NonNull String fileName) {
        // Remove extension
        String configName = fileName.replace(".yml", "").replace(".yaml", "");

        Tools.LOG.info("Detected change in configuration file: {}", fileName);
        Tools.LOG.info("Reloading configuration: {}", configName);

        // Reload through ConfigurationManager
        ConfigurationManager configManager = this.plugin.getConfigurationManager();
        configManager.getConfigById(configName).ifPresent(provider -> {
            try {
                provider.reload(configManager.getDefaultOptions());
                Tools.LOG.info("Successfully reloaded configuration: {}", configName);

                // Trigger reload for all reloadable components
                this.triggerComponentReloads(configName);
            } catch (Exception e) {
                Tools.LOG.error("Failed to reload configuration: {}", configName, e);
            }
        });
    }

    /**
     * Triggers reload events for all components that implement ConfigReloadable.
     *
     * @param configName The name of the configuration that was reloaded.
     */
    private void triggerComponentReloads(@NonNull String configName) {
        // Find all components that implement ConfigReloadable
        this.plugin.getRegistryFactory()
                .findClassesImplementingInterface(ConfigReloadable.class)
                .forEach(clazz -> {
                    try {
                        Object instance = this.plugin.getRegistryFactory()
                                .getInstance(clazz.getName());

                        if (instance instanceof ConfigReloadable reloadable) {
                            reloadable.loadConfig(this.plugin);
                            Tools.LOG.debug("Reloaded configuration for: {}",
                                    clazz.getSimpleName());
                        }
                    } catch (Exception e) {
                        Tools.LOG.error("Failed to reload component: {}",
                                clazz.getSimpleName(), e);
                    }
                });
    }
}