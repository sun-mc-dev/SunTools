package me.sunmc.tools.configuration;

import io.leangen.geantyref.TypeToken;
import me.sunmc.tools.Tools;
import me.sunmc.tools.configuration.serializers.location.LocationConfigSerializer;
import me.sunmc.tools.configuration.serializers.sound.SoundConfigSerializer;
import me.sunmc.tools.configuration.serializers.sound.SoundWrapper;
import me.sunmc.tools.item.config.ItemStackConfigSerializer;
import me.sunmc.tools.utils.bukkit.BukkitFileUtil;
import me.sunmc.tools.utils.java.LoggerUtil;
import me.sunmc.tools.utils.java.SinglePointInitiator;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced configuration manager with advanced features:
 * - Multi-file support
 * - Custom serializers
 * - Hot reload capability
 * - Configuration validation
 * - Backup system
 * - Thread-safe operations
 *
 * @version 1.0.0
 */
public class ConfigurationManager extends SinglePointInitiator {

    private final @NonNull Logger logger;
    private final @NonNull Map<String, ConfigurationProvider> configurations;
    private final @NonNull Map<String, Long> lastModified;
    private final @NonNull ConfigurationOptions options;
    private final @NonNull Tools plugin;
    private final @NonNull File configDirectory;

    private boolean createBackups = true;
    private int maxBackups = 5;

    public ConfigurationManager(@NonNull Tools plugin) throws IOException {
        this.configurations = new ConcurrentHashMap<>();
        this.lastModified = new ConcurrentHashMap<>();
        this.plugin = plugin;
        this.configDirectory = plugin.getDataFolder();

        Logger logger = LoggerUtil.createLoggerWithIdentifier(plugin, "ConfigManager");
        this.logger = logger;

        // Build type serializer collection
        TypeSerializerCollection serializers = TypeSerializerCollection.defaults()
                .childBuilder()
                .register(TypeToken.get(ItemStack.class), new ItemStackConfigSerializer())
                .register(TypeToken.get(SoundWrapper.class), new SoundConfigSerializer())
                .register(TypeToken.get(Location.class), new LocationConfigSerializer())
                .build();

        this.options = ConfigurationOptions.defaults().serializers(serializers);

        Class<? extends Tools> mainClass = plugin.getClass();
        if (!mainClass.isAnnotationPresent(LoadConfigurations.class)) {
            this.logger.info("No @LoadConfigurations annotation found, skipping auto-load");
            return;
        }

        File config = new File(this.configDirectory, "config.yml");

        if (!config.exists()) {
            logger.info("Detected fresh plugin setup.");

            if (this.configDirectory.mkdir()) {
                logger.info("Plugin data folder was created.");
            }
        }

        // Load all configurations from annotation
        for (String identifier : mainClass.getAnnotation(LoadConfigurations.class).value()) {
            this.loadConfiguration(identifier);
        }
    }

    /**
     * Loads a configuration file.
     *
     * @param identifier The configuration identifier (without extension).
     * @throws IOException If an I/O error occurs.
     */
    private void loadConfiguration(@NonNull String identifier) throws IOException {
        File file = BukkitFileUtil.setupPluginFile(this.plugin, identifier + ".yml");
        ConfigurationProvider provider = new ConfigurationProvider(identifier, file, this.options);
        this.registerConfig(provider);
        this.lastModified.put(identifier, file.lastModified());
    }

    /**
     * Register a new configuration into storage.
     *
     * @param provider Instance of the configuration provider to register.
     */
    public void registerConfig(@NonNull ConfigurationProvider provider) {
        final String identifier = provider.getFileId();
        this.configurations.putIfAbsent(identifier, provider);
        this.logger.info("Registered the configuration with file id '{}'.", identifier);
    }

    /**
     * Loads a configuration that wasn't automatically loaded.
     *
     * @param identifier The configuration identifier.
     * @param createIfMissing If true, creates the file if it doesn't exist.
     * @return The loaded configuration provider.
     */
    public @Nullable ConfigurationProvider loadConfig(@NonNull String identifier, boolean createIfMissing) {
        try {
            File file = new File(this.configDirectory, identifier + ".yml");

            if (!file.exists()) {
                if (createIfMissing) {
                    if (file.createNewFile()) {
                        this.logger.info("Created new configuration file: {}", identifier);
                    }
                } else {
                    this.logger.warn("Configuration file does not exist: {}", identifier);
                    return null;
                }
            }

            ConfigurationProvider provider = new ConfigurationProvider(identifier, file, this.options);
            this.registerConfig(provider);
            this.lastModified.put(identifier, file.lastModified());
            return provider;

        } catch (IOException e) {
            this.logger.error("Failed to load configuration: {}", identifier, e);
            return null;
        }
    }

    /**
     * Reloads all configurations with default options.
     */
    public void reloadConfigurations() {
        this.logger.info("Reloading all configurations...");
        int reloaded = 0;

        for (ConfigurationProvider provider : this.configurations.values()) {
            try {
                provider.reload(this.getDefaultOptions());
                this.lastModified.put(provider.getFileId(),
                        new File(this.configDirectory, provider.getFileId() + ".yml").lastModified());
                reloaded++;
            } catch (Exception e) {
                this.logger.error("Failed to reload configuration: {}", provider.getFileId(), e);
            }
        }

        this.logger.info("Reloaded {}/{} configurations", reloaded, this.configurations.size());
    }

    /**
     * Reloads a specific configuration.
     *
     * @param identifier The configuration identifier.
     * @return True if reloaded successfully, false otherwise.
     */
    public boolean reloadConfiguration(@NonNull String identifier) {
        ConfigurationProvider provider = this.configurations.get(identifier);
        if (provider == null) {
            this.logger.warn("Cannot reload unknown configuration: {}", identifier);
            return false;
        }

        try {
            if (this.createBackups) {
                this.createBackup(identifier);
            }

            provider.reload(this.options);
            this.lastModified.put(identifier,
                    new File(this.configDirectory, identifier + ".yml").lastModified());
            this.logger.info("Reloaded configuration: {}", identifier);
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to reload configuration: {}", identifier, e);
            return false;
        }
    }

    /**
     * Creates a backup of a configuration file.
     *
     * @param identifier The configuration identifier.
     */
    private void createBackup(@NonNull String identifier) {
        try {
            File original = new File(this.configDirectory, identifier + ".yml");
            if (!original.exists()) {
                return;
            }

            File backupDir = new File(this.configDirectory, "backups");
            if (!backupDir.exists()) {
                backupDir.mkdir();
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            File backup = new File(backupDir, identifier + "_" + timestamp + ".yml");

            Files.copy(original.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.logger.debug("Created backup: {}", backup.getName());

            // Clean old backups
            this.cleanOldBackups(identifier, backupDir);

        } catch (IOException e) {
            this.logger.warn("Failed to create backup for: {}", identifier, e);
        }
    }

    /**
     * Cleans old backups keeping only the most recent ones.
     */
    private void cleanOldBackups(@NonNull String identifier, @NonNull File backupDir) {
        File[] backups = backupDir.listFiles((dir, name) ->
                name.startsWith(identifier + "_") && name.endsWith(".yml"));

        if (backups == null || backups.length <= this.maxBackups) {
            return;
        }

        // Sort by modification time (oldest first)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        // Delete oldest backups
        int toDelete = backups.length - this.maxBackups;
        for (int i = 0; i < toDelete; i++) {
            if (backups[i].delete()) {
                this.logger.debug("Deleted old backup: {}", backups[i].getName());
            }
        }
    }

    /**
     * Saves a configuration to disk.
     *
     * @param identifier The configuration identifier.
     * @return True if saved successfully, false otherwise.
     */
    public boolean saveConfiguration(@NonNull String identifier) {
        ConfigurationProvider provider = this.configurations.get(identifier);
        if (provider == null) {
            this.logger.warn("Cannot save unknown configuration: {}", identifier);
            return false;
        }

        try {
            provider.save();
            this.logger.info("Saved configuration: {}", identifier);
            return true;
        } catch (Exception e) {
            this.logger.error("Failed to save configuration: {}", identifier, e);
            return false;
        }
    }

    /**
     * Saves all configurations to disk.
     */
    public void saveAllConfigurations() {
        this.logger.info("Saving all configurations...");
        int saved = 0;

        for (ConfigurationProvider provider : this.configurations.values()) {
            try {
                provider.save();
                saved++;
            } catch (Exception e) {
                this.logger.error("Failed to save configuration: {}", provider.getFileId(), e);
            }
        }

        this.logger.info("Saved {}/{} configurations", saved, this.configurations.size());
    }

    /**
     * Checks if a configuration has been modified since last load.
     *
     * @param identifier The configuration identifier.
     * @return True if modified, false otherwise.
     */
    public boolean hasBeenModified(@NonNull String identifier) {
        Long lastMod = this.lastModified.get(identifier);
        if (lastMod == null) {
            return false;
        }

        File file = new File(this.configDirectory, identifier + ".yml");
        return file.lastModified() > lastMod;
    }

    /**
     * Gets all loaded configurations.
     *
     * @return Unmodifiable map of all configurations.
     */
    public @NonNull Map<String, ConfigurationProvider> getAllConfigurations() {
        return Collections.unmodifiableMap(this.configurations);
    }

    /**
     * Gets the number of loaded configurations.
     *
     * @return The count of loaded configurations.
     */
    public int getConfigurationCount() {
        return this.configurations.size();
    }

    /**
     * Checks if a configuration is loaded.
     *
     * @param identifier The configuration identifier.
     * @return True if loaded, false otherwise.
     */
    public boolean isLoaded(@NonNull String identifier) {
        return this.configurations.containsKey(identifier);
    }

    /**
     * Find a cached configuration based on its identifier wrapped in {@link ConfigIdWrapper}.
     *
     * @param identifier The identifier to find a configuration with.
     * @return Instance of the found {@link ConfigurationProvider} wrapped in an {@link Optional}.
     */
    public Optional<ConfigurationProvider> getConfigById(@NonNull ConfigIdWrapper identifier) {
        return this.getConfigById(identifier.getKey());
    }

    /**
     * Find a cached configuration based on its identifier.
     *
     * @param identifier The identifier to find a configuration with.
     * @return Instance of the found {@link ConfigurationProvider} wrapped in an {@link Optional}.
     */
    public Optional<ConfigurationProvider> getConfigById(@NonNull String identifier) {
        return Optional.ofNullable(this.configurations.get(identifier));
    }

    /**
     * Sets whether to create backups before reloading.
     *
     * @param createBackups True to create backups, false otherwise.
     */
    public void setCreateBackups(boolean createBackups) {
        this.createBackups = createBackups;
    }

    /**
     * Sets the maximum number of backups to keep.
     *
     * @param maxBackups The maximum number of backups.
     */
    public void setMaxBackups(int maxBackups) {
        this.maxBackups = Math.max(1, maxBackups);
    }

    /**
     * @return Default configuration options for this library.
     */
    public @NonNull ConfigurationOptions getDefaultOptions() {
        return this.options;
    }
}