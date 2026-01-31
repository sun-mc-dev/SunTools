package me.sunmc.tools.configuration;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced configuration provider with convenience methods.
 *
 * <p>Features:
 * - Easy value retrieval
 * - Type-safe getters
 * - Default value support
 * - Path traversal helpers
 * - Save functionality
 * - Reload capability
 *
 * @version 1.0.0
 */
public class ConfigurationProvider {

    private final @NonNull String fileId;
    private final @NonNull File file;
    private @NonNull ConfigurationLoader<?> loader;
    private ConfigurationNode rootNode;

    public ConfigurationProvider(@NonNull String fileId, @NonNull File file, @NonNull ConfigurationOptions options) {
        this.fileId = fileId;
        this.file = file;
        this.loader = this.setupConfigLoader();
        this.reload(options);
    }

    /**
     * Sets up the configuration loader.
     */
    private ConfigurationLoader<?> setupConfigLoader() {
        return YamlConfigurationLoader.builder().file(this.file).build();
    }

    /**
     * Reloads this configuration into cache.
     */
    public void reload() {
        this.reload(ConfigurationOptions.defaults());
    }

    /**
     * Reloads this configuration into cache.
     *
     * @param options Configuration options that should be used when reloading.
     */
    public void reload(@NonNull ConfigurationOptions options) {
        try {
            this.loader = this.setupConfigLoader();
            this.rootNode = this.loader.load(options);
        } catch (ConfigurateException exception) {
            throw new RuntimeException("Something went wrong when loading in the configuration with file id '" + this.fileId + "'", exception);
        }
    }

    /**
     * Saves this configuration to disk.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void save() throws IOException {
        this.loader.save(this.rootNode);
    }

    /**
     * Gets a string value from the configuration.
     *
     * @param path The path to the value.
     * @return The string value, or null if not found.
     */
    public @Nullable String getString(@NonNull Object... path) {
        return this.rootNode.node(path).getString();
    }

    /**
     * Gets a string value with a default.
     *
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @return The string value, or default if not found.
     */
    public @NonNull String getString(@NonNull String defaultValue, @NonNull Object... path) {
        return this.rootNode.node(path).getString(defaultValue);
    }

    /**
     * Gets an integer value from the configuration.
     *
     * @param path The path to the value.
     * @return The integer value, or 0 if not found.
     */
    public int getInt(@NonNull Object... path) {
        return this.rootNode.node(path).getInt();
    }

    /**
     * Gets an integer value with a default.
     *
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @return The integer value, or default if not found.
     */
    public int getInt(int defaultValue, @NonNull Object... path) {
        return this.rootNode.node(path).getInt(defaultValue);
    }

    /**
     * Gets a double value from the configuration.
     *
     * @param path The path to the value.
     * @return The double value, or 0.0 if not found.
     */
    public double getDouble(@NonNull Object... path) {
        return this.rootNode.node(path).getDouble();
    }

    /**
     * Gets a double value with a default.
     *
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @return The double value, or default if not found.
     */
    public double getDouble(double defaultValue, @NonNull Object... path) {
        return this.rootNode.node(path).getDouble(defaultValue);
    }

    /**
     * Gets a boolean value from the configuration.
     *
     * @param path The path to the value.
     * @return The boolean value, or false if not found.
     */
    public boolean getBoolean(@NonNull Object... path) {
        return this.rootNode.node(path).getBoolean();
    }

    /**
     * Gets a boolean value with a default.
     *
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @return The boolean value, or default if not found.
     */
    public boolean getBoolean(boolean defaultValue, @NonNull Object... path) {
        return this.rootNode.node(path).getBoolean(defaultValue);
    }

    /**
     * Gets a long value from the configuration.
     *
     * @param path The path to the value.
     * @return The long value, or 0L if not found.
     */
    public long getLong(@NonNull Object... path) {
        return this.rootNode.node(path).getLong();
    }

    /**
     * Gets a long value with a default.
     *
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @return The long value, or default if not found.
     */
    public long getLong(long defaultValue, @NonNull Object... path) {
        return this.rootNode.node(path).getLong(defaultValue);
    }

    /**
     * Gets a list of strings from the configuration.
     *
     * @param path The path to the list.
     * @return The string list, or empty list if not found.
     */
    public @NonNull List<String> getStringList(@NonNull Object... path) throws SerializationException {
        return this.rootNode.node(path).getList(String.class, List.of());
    }

    /**
     * Gets a list of integers from the configuration.
     *
     * @param path The path to the list.
     * @return The integer list, or empty list if not found.
     */
    public @NonNull List<Integer> getIntList(@NonNull Object... path) throws SerializationException {
        return this.rootNode.node(path).getList(Integer.class, List.of());
    }

    /**
     * Gets a typed object from the configuration.
     *
     * @param type The class type.
     * @param path The path to the value.
     * @param <T> The type parameter.
     * @return The object, or null if not found.
     */
    public <T> @Nullable T get(@NonNull Class<T> type, @NonNull Object... path) {
        try {
            return this.rootNode.node(path).get(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a typed object with a default value.
     *
     * @param type The class type.
     * @param defaultValue The default value.
     * @param path The path to the value.
     * @param <T> The type parameter.
     * @return The object, or default if not found.
     */
    public <T> @NonNull T get(@NonNull Class<T> type, @NonNull T defaultValue, @NonNull Object... path) {
        try {
            T value = this.rootNode.node(path).get(type);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value in the configuration.
     *
     * @param value The value to set.
     * @param path The path to set at.
     */
    public void set(@Nullable Object value, @NonNull Object... path) {
        try {
            this.rootNode.node(path).set(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set value at path: " + String.join(".", path.toString()), e);
        }
    }

    /**
     * Checks if a path exists in the configuration.
     *
     * @param path The path to check.
     * @return True if exists, false otherwise.
     */
    public boolean exists(@NonNull Object... path) {
        return !this.rootNode.node(path).virtual();
    }

    /**
     * Gets a node at the specified path.
     *
     * @param path The path to the node.
     * @return The configuration node.
     */
    public @NonNull ConfigurationNode getNode(@NonNull Object... path) {
        return this.rootNode.node(path);
    }

    /**
     * Gets a node wrapped in an Optional.
     *
     * @param path The path to the node.
     * @return Optional containing the node if it exists.
     */
    public @NonNull Optional<ConfigurationNode> getNodeOptional(@NonNull Object... path) {
        ConfigurationNode node = this.rootNode.node(path);
        return node.virtual() ? Optional.empty() : Optional.of(node);
    }

    /**
     * Gets the file id of this configuration.
     *
     * @return The file id as a non-null {@link String}.
     */
    public @NonNull String getFileId() {
        return this.fileId;
    }

    /**
     * Gets the file associated with this configuration.
     *
     * @return The file.
     */
    public @NonNull File getFile() {
        return this.file;
    }

    /**
     * Gets the loader for this configuration.
     *
     * @return The {@link ConfigurationLoader} for this configuration.
     */
    public @NonNull ConfigurationLoader<?> getLoader() {
        return this.loader;
    }

    /**
     * Gets the {@link ConfigurationNode root note} for this configuration.
     *
     * @return The configuration root node as a non-null {@link ConfigurationNode}.
     */
    public @NonNull ConfigurationNode getRootNode() {
        return this.rootNode;
    }
}