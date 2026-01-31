package me.sunmc.tools;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import me.sunmc.tools.command.CommandManager;
import me.sunmc.tools.component.Component;
import me.sunmc.tools.component.ComponentManager;
import me.sunmc.tools.configuration.ConfigIdWrapper;
import me.sunmc.tools.configuration.ConfigurationManager;
import me.sunmc.tools.configuration.ConfigurationProvider;
import me.sunmc.tools.menu.input.InputMenu;
import me.sunmc.tools.registry.RegistryFactory;
import me.sunmc.tools.scheduler.BukkitSchedulerAdapter;
import me.sunmc.tools.scheduler.handler.SchedulerHandlerManager;
import me.sunmc.tools.scheduler.interfaces.SchedulerAdapter;
import me.sunmc.tools.utils.bukkit.ListenerRegistryFactory;
import me.sunmc.tools.utils.java.LoggerUtil;
import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Optional;

/**
 * Enhanced plugin entry point for the SunMC Tools framework.
 *
 * <p>Features:
 * - Component system with dependency management
 * - Configuration management with auto-reload
 * - Command API integration
 * - Scheduler system with handlers
 * - Menu system integration
 * - Automatic class registration
 * - Lifecycle management
 * - Performance metrics
 *
 * <p>Example usage:
 * <pre>{@code
 * @LoadConfigurations({"config", "messages", "database"})
 * public class MyPlugin extends Tools {
 *
 *     @Override
 *     public void onStartup() {
 *         getLogger().info("Plugin started!");
 *     }
 *
 *     @Override
 *     public void onShutdown() {
 *         getLogger().info("Plugin stopped!");
 *     }
 * }
 * }</pre>
 *
 * @version 1.0.0
 */
public abstract class Tools extends JavaPlugin {

    public static Logger LOG;

    private final @NonNull Class<? extends Tools> parentPluginClass;
    private final @NonNull String parentPluginIdentifier;
    private final @NonNull Reflections reflections;
    private final @NonNull RegistryFactory registryFactory;
    private final @NonNull ConfigurationManager configurationManager;

    private ComponentManager componentManager;
    private CommandManager commandManager;
    private SchedulerAdapter schedulerAdapter;
    private SchedulerHandlerManager schedulerHandlerManager;

    private boolean shouldLogStartupInformationStart = true;
    private boolean shouldLogStartupInformationDone = true;
    private boolean debugMode = false;
    private long startupTime = 0;

    public Tools() {
        this.parentPluginClass = this.getClass();
        this.parentPluginIdentifier = this.getPluginMeta().getName();
        LOG = LoggerUtil.createLogger(this.parentPluginIdentifier);

        this.reflections = new Reflections(ConfigurationBuilder.build().forPackages(
                "me.sunmc.tools",
                this.parentPluginClass.getPackageName()
        ));
        this.registryFactory = new RegistryFactory(this.reflections, this);

        try {
            this.configurationManager = new ConfigurationManager(this);
        } catch (IOException exception) {
            throw new RuntimeException("Could not set up ConfigurationManager!", exception);
        }
    }

    /**
     * @return Instance of the plugin entry point.
     */
    public static @NonNull Tools getInstance() {
        return getPlugin(Tools.class);
    }

    /**
     * Static method to get an instance of a registered component.
     *
     * @param componentClass Class of the component.
     * @return Instance of a registered component.
     */
    public static <T extends Component> @NonNull T getComponent(@NonNull Class<T> componentClass) {
        return Tools.getInstance().getRegisteredComponent(componentClass);
    }

    /**
     * Called when plugin loads.
     */
    public void onPreLoad() {
    }

    /**
     * Called when the plugin starts up.
     */
    public abstract void onStartup();

    /**
     * Called when the plugin shuts down.
     */
    public abstract void onShutdown();

    /**
     * Called when the plugin needs to reload.
     * Override to add custom reload logic.
     */
    public void onReload() {
        LOG.info("Reloading plugin...");
        this.configurationManager.reloadConfigurations();
        LOG.info("Plugin reloaded successfully!");
    }

    @Deprecated
    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));
        this.onPreLoad();
    }

    @Deprecated
    @Override
    public void onEnable() {
        this.startupTime = System.currentTimeMillis();

        if (this.shouldLogStartupInformationStart) {
            this.logStartupInformationStart(this.getServer());
        }

        try {
            this.componentManager = new ComponentManager(this, this.registryFactory);
            this.commandManager = new CommandManager(this.registryFactory);
            this.schedulerAdapter = new BukkitSchedulerAdapter(this);
            this.schedulerHandlerManager = new SchedulerHandlerManager(this.registryFactory);
            this.registryFactory.executeAllAutoRegistering();
            this.componentManager.enableAllComponents();

            CommandAPI.onEnable();
            new ListenerRegistryFactory(this).registerAllListeners();

            this.onStartup();
            this.schedulerHandlerManager.startAllAutoSchedulers();

            long finishedTime = System.currentTimeMillis() - this.startupTime;

            if (this.shouldLogStartupInformationDone) {
                this.logStartupInformationDone(finishedTime);
            } else {
                LOG.info("Plugin finished loading in " + finishedTime + "ms.");
            }

        } catch (Exception e) {
            LOG.error("Fatal error during plugin startup!", e);
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Deprecated
    @Override
    public void onDisable() {
        final long timeAtStart = System.currentTimeMillis();

        try {
            InputMenu.unregisterListener();
            this.onShutdown();
            CommandAPI.onDisable();

            if (this.componentManager != null) {
                this.componentManager.disableAllComponents();
            }

            if (this.schedulerAdapter != null) {
                this.schedulerAdapter.shutdown();
            }

            long finishedTime = System.currentTimeMillis() - timeAtStart;
            LOG.info("Plugin shutdown in " + finishedTime + "ms.");

        } catch (Exception e) {
            LOG.error("Error during plugin shutdown!", e);
        }
    }

    /**
     * Logs startup information.
     */
    private void logStartupInformationStart(@NonNull Server server) {
        final String version = server.getVersion()
                .replace("(", "")
                .replace(")", "");

        final PluginDescriptionFile desc = (PluginDescriptionFile) this.getPluginMeta();

        LOG.info("#-----------------------------------#");
        LOG.info("    Plugin Startup Information       ");
        LOG.info("     " + desc.getName());
        LOG.info("                                     ");
        LOG.info("Internal ID: " + this.parentPluginIdentifier);
        LOG.info("Found Main Class: " + this.parentPluginClass.getSimpleName() + " (" + this.parentPluginClass.getPackageName() + ")");
        LOG.info("Version: v" + desc.getVersion() + " (Minecraft: " + version + ", " + server.getBukkitVersion() + ")");

        if (!desc.getAuthors().isEmpty()) {
            LOG.info("Authors: " + String.join(", ", desc.getAuthors()));
        }

        LOG.info("#-----------------------------------#");
    }

    /**
     * Logs startup completion information.
     */
    private void logStartupInformationDone(long finishedMilliseconds) {
        LOG.info("#-----------------------------------#");
        LOG.info("    Plugin Successfully Loaded       ");
        LOG.info("                                     ");
        LOG.info("Comment: '" + this.parentPluginIdentifier + " has successfully loaded without issues.'");
        LOG.info("Load Time: " + finishedMilliseconds + "ms");
        LOG.info("Components: " + (this.componentManager != null ? this.componentManager.getComponents().size() : 0));
        LOG.info("#-----------------------------------#");
    }

    /**
     * If the plugin should send a log message when the plugin is starting.
     *
     * @param value {@code true} if a log message should be sent, {@code false} otherwise.
     */
    public void shouldLogStartupInformationStart(boolean value) {
        this.shouldLogStartupInformationStart = value;
    }

    /**
     * If the plugin should send a log message once the plugin startup is finished.
     *
     * @param value {@code true} if a log message should be sent, {@code false} otherwise.
     */
    public void shouldLogStartupInformationDone(boolean value) {
        this.shouldLogStartupInformationDone = value;
    }

    /**
     * Enables or disables debug mode.
     *
     * @param debug True to enable debug logging, false to disable.
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        if (debug) {
            LOG.info("Debug mode enabled");
        }
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return True if debug mode is enabled, false otherwise.
     */
    public boolean isDebugMode() {
        return this.debugMode;
    }

    /**
     * Logs a debug message if debug mode is enabled.
     *
     * @param message The message to log.
     */
    public void debug(@NonNull String message) {
        if (this.debugMode) {
            LOG.debug("[DEBUG] " + message);
        }
    }

    /**
     * Gets the time it took for the plugin to start up.
     *
     * @return Startup time in milliseconds.
     */
    public long getStartupTime() {
        return this.startupTime;
    }

    /**
     * @return The identifier for this plugin.
     */
    public @NonNull String getPluginIdentifier() {
        return this.parentPluginIdentifier;
    }

    /**
     * @return Reflections instance for this plugin.
     */
    public @NonNull Reflections getReflections() {
        return this.reflections;
    }

    /**
     * @return Registry factory responsible for class instance registering and instantiation.
     */
    public @NonNull RegistryFactory getRegistryFactory() {
        return this.registryFactory;
    }

    /**
     * @return Configuration manager responsible for storing and managing configurations.
     */
    public @NonNull ConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    /**
     * @param fileId The id of the file to get the config for.
     * @return A ConfigurationProvider for the file id entered.
     */
    public Optional<ConfigurationProvider> getRegisteredConfig(@NonNull String fileId) {
        return this.configurationManager.getConfigById(fileId);
    }

    /**
     * @param identifier The identifier of the config to get.
     * @return A ConfigurationProvider for the identifier entered.
     */
    public Optional<ConfigurationProvider> getRegisteredConfig(@NonNull ConfigIdWrapper identifier) {
        return this.configurationManager.getConfigById(identifier);
    }

    /**
     * @return Component manager responsible for managing and holding all modules.
     */
    public @NonNull ComponentManager getComponentManager() {
        return this.componentManager;
    }

    /**
     * @return Command manager that provides custom integration extension to the {@link CommandAPI}.
     */
    public @NonNull CommandManager getCommandManager() {
        return this.commandManager;
    }

    /**
     * @return Scheduler adapter used to work with synchronous and asynchronous tasks.
     */
    public @NonNull SchedulerAdapter getSchedulerAdapter() {
        return this.schedulerAdapter;
    }

    /**
     * @return Scheduler handler manager used to abstract task logic to separate classes.
     */
    public @NonNull SchedulerHandlerManager getSchedulerHandlerManager() {
        return this.schedulerHandlerManager;
    }

    /**
     * Gets the instance of a registered component from the component manager.
     *
     * @param componentClass Class of the component.
     * @return Instance of a registered {@link Component}.
     * @throws NullPointerException If the component is not registered within the component manager.
     * @see Tools#getComponent(Class)
     */
    public <T extends Component> @NonNull T getRegisteredComponent(@NonNull Class<T> componentClass) {
        T module = this.getComponentManager().getComponent(componentClass);
        if (module == null) {
            throw new NullPointerException("The component with class " + componentClass.getSimpleName() + " is null");
        }
        return module;
    }
}