# Tools - Comprehensive Minecraft Plugin Development Framework

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Paper API](https://img.shields.io/badge/Paper-1.21.11-green.svg)](https://papermc.io/)
[![](https://jitpack.io/v/sun-mc-dev/SunTools.svg)](https://jitpack.io/#sun-mc-dev/SunTools)

A powerful, feature-rich framework and utilities library for Minecraft plugin development using Paper/Spigot API. Built
with modern Java 21 features and best practices.

## 🌟 Features

### Core Framework

- **Component System** - Modular architecture with dependency injection and lifecycle management
- **Configuration Management** - YAML configuration with hot-reload, backup system, and custom serializers
- **Command Framework** - Integration with CommandAPI for powerful command creation
- **Scheduler System** - Advanced task scheduling with sync/async support and handlers
- **Registry Factory** - Automatic class discovery, instantiation, and dependency resolution

### GUI System

- **Menu Framework** - Create interactive chest GUIs with ease
- **Paginated Menus** - Built-in pagination with search, filter, and sort support
- **Input Menus** - Chat-based input system for menus
- **Confirmation Dialogs** - Pre-built confirmation menus
- **Menu Patterns** - String-based layout patterns for quick menu design

### Utilities

- **Item Builder** - Fluent API for creating and modifying ItemStacks
- **Player Utilities** - Common player operations and state management
- **Legacy Component Support** - Easy migration from legacy color codes
- **Sound Utilities** - Simplified sound playing
- **Timer System** - Feature-rich countdown/countup timers
- **File Utilities** - File operations and download utilities

### Advanced Features

- **Auto-Registration** - Automatic component, listener, and command registration
- **Hot-Reload Support** - Live configuration reloading with file watchers
- **Performance Tracking** - Built-in metrics for instantiation times
- **Thread-Safe Operations** - Concurrent data structures and safe multi-threading
- **Event-Driven Architecture** - Callbacks and listeners throughout the framework

## 📋 Requirements

- **Java 21** or higher
- **Paper 1.21.11** or compatible fork (Purpur, Pufferfish, etc.)
- **Maven** or **Gradle** for dependency management

## 📦 Installation (see releases for version)

### Maven

Add JitPack repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.github.sun-mc-dev</groupId>
        <artifactId>SunTools</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>
```

### Gradle

Add JitPack repository:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add the dependency:

```gradle
dependencies {
    implementation 'com.github.sun-mc-dev:SunTools:VERSION'
}
```

## 🚀 Quick Start

### Basic Plugin Setup

```java
@LoadConfigurations({"config", "messages"})
public class MyPlugin extends Tools {
    
    @Override
    public void onStartup() {
        getLogger().info("Plugin started successfully!");
    }
    
    @Override
    public void onShutdown() {
        getLogger().info("Plugin stopped!");
    }
}
```

### Creating a Component

```java
@AutoRegister(Component.class)
public class GameManager implements Component {
    
    private final MyPlugin plugin;
    
    public GameManager(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onEnable() {
        // Component initialization logic
    }
    
    @Override
    public void onDisable() {
        // Component cleanup logic
    }
}
```

### Creating a Command

```java
@AutoRegister(CommandFactory.class)
public class GameCommands implements CommandFactory {
    
    @Override
    public CommandAPICommand buildSingleCommand() {
        return new CommandAPICommand("game")
            .withPermission("myplugin.game")
            .withSubcommand(new CommandAPICommand("start")
                .executesPlayer((player, args) -> {
                    player.sendMessage("Game started!");
                })
            )
            .register();
    }
}
```

### Creating a Menu

```java
public class ShopMenu extends Menu {
    
    public ShopMenu(Player viewer) {
        super(viewer);
    }
    
    @Override
    public Component getTitle() {
        return Component.text("Shop");
    }
    
    @Override
    public int getRows() {
        return 3;
    }
    
    @Override
    public void init() {
        // Add border
        fillBorder(MenuItem.placeholder(new ItemStack(Material.GRAY_STAINED_GLASS_PANE)));
        
        // Add shop items
        ItemStack sword = new ItemStackBuilder(Material.DIAMOND_SWORD)
            .name("Diamond Sword")
            .lore("Price: $100")
            .build();
            
        setItem(1, 4, MenuItem.of(sword, event -> {
            Player player = event.player();
            player.sendMessage("You purchased a Diamond Sword!");
            close();
        }));
    }
}

// Open the menu
new ShopMenu(player).open();
```

### Using Configuration

```java
@AutoRegister(Component.class)
public class ConfigManager implements Component, ConfigReloadable {
    
    private String welcomeMessage;
    private int maxPlayers;
    
    public ConfigManager(Tools plugin) {
        loadConfig(plugin);
    }
    
    @Override
    public void loadConfig(Tools plugin) {
        ConfigurationProvider config = plugin.getRegisteredConfig("config")
            .orElseThrow();
            
        welcomeMessage = config.getString("messages.welcome", "Welcome!");
        maxPlayers = config.getInt("settings.max-players", 10);
    }
}
```

### Creating a Paginated Menu

```java
public class PlayerListMenu extends PaginatedMenu {
    
    public PlayerListMenu(Player viewer) {
        super(viewer);
    }
    
    @Override
    public Component getTitle() {
        return Component.text("Online Players");
    }
    
    @Override
    public int getRows() {
        return 6;
    }
    
    @Override
    public void init() {
        // Add all online players as items
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(meta -> {
                if (meta instanceof SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(online);
                }
                meta.displayName(Component.text(online.getName()));
            });
            
            addContentItem(MenuItem.of(head, event -> {
                event.player().sendMessage("You clicked on " + online.getName());
            }));
        }
        
        super.init();
    }
}
```

### Using Timers

```java
SimpleTimer countdown = new SimpleTimer("game-countdown", 60, 0)
    .setInterval(1, TimeUnit.SECONDS)
    .onTick(seconds -> {
        if (seconds % 10 == 0) {
            Bukkit.broadcast(Component.text("Game starts in " + seconds + " seconds!"));
        }
    })
    .onComplete(() -> {
        Bukkit.broadcast(Component.text("Game started!"));
        startGame();
    })
    .setAutoRestart(false);

countdown.startTimer(SimpleTimer.TimeChange.DECREMENT);
```

### Creating a Scheduler Handler

```java
@AutoRegister(AbstractSchedulerHandler.class)
@AutoStartSchedulerHandler(async = true)
public class AutoSaveHandler extends AbstractSchedulerHandler {
    
    public AutoSaveHandler() {
        super("auto-save", 0, 5, TimeUnit.MINUTES);
    }
    
    @Override
    public void run() {
        // Save data every 5 minutes
        Tools.LOG.info("Auto-saving data...");
        saveAllData();
    }
}
```

## 📚 Advanced Examples

### Custom Item Serializer

```java
public class CustomItemSerializer implements TypeSerializer<CustomItem> {
    
    @Override
    public CustomItem deserialize(Type type, ConfigurationNode node) 
            throws SerializationException {
        String id = node.node("id").getString();
        String name = node.node("name").getString();
        int power = node.node("power").getInt();
        
        return new CustomItem(id, name, power);
    }
    
    @Override
    public void serialize(Type type, CustomItem obj, ConfigurationNode node) 
            throws SerializationException {
        node.node("id").set(obj.getId());
        node.node("name").set(obj.getName());
        node.node("power").set(obj.getPower());
    }
}
```

### Menu with Patterns

```java
public class PatternMenu extends Menu {
    
    @Override
    public void init() {
        MenuPattern pattern = MenuPattern.builder()
            .row("XXXXXXXXX")
            .row("X       X")
            .row("X   I   X")
            .row("X       X")
            .row("XXXXXXXXX")
            .item('X', MenuItem.placeholder(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)))
            .item('I', createInfoItem())
            .build();
            
        pattern.apply(this);
    }
}
```

### Dependency Injection

```java
@AutoRegister(Component.class)
@DependencyComponent({ConfigManager.class, GameManager.class})
public class ArenaManager implements Component {
    
    private final ConfigManager configManager;
    private final GameManager gameManager;
    
    // Dependencies automatically injected
    public ArenaManager(ConfigManager configManager, GameManager gameManager) {
        this.configManager = configManager;
        this.gameManager = gameManager;
    }
}
```

## 🔧 Configuration System

### Auto-Loading Configurations

```java
@LoadConfigurations({"config", "arenas", "rewards"})
public class MyPlugin extends Tools {
    // Configurations automatically loaded on startup
}
```

### Configuration with Custom Serializers

```yaml
# config.yml
spawn-location:
  world: world
  x: 100.5
  y: 64.0
  z: -50.5
  yaw: 90.0
  pitch: 0.0

reward-item:
  material: DIAMOND_SWORD
  amount: 1
  display-name: "&6Legendary Sword"
  lore:
    - "&7A powerful weapon"
    - "&7Damage: &c+10"
  enchantments:
    sharpness: 5
    unbreaking: 3
```

```java
Location spawn = config.get(Location.class, "spawn-location");
ItemStack reward = config.get(ItemStack.class, "reward-item");
```

## 🎨 GUI Features

### Menu Actions

```java
Menu menu = new ExampleMenu(player)
        .addOpenAction(p -> p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f))
        .addCloseAction(p -> p.sendMessage("Thanks for visiting!"))
        .setAllowItemMovement(false)
        .enableAutoRefresh(20); // Refresh every second
```

### Confirmation Dialogs

```java
ConfirmationMenu.builder(player)
    .title("Delete Arena?")
    .question("Are you sure you want to delete this arena?")
    .warning("This action cannot be undone!")
    .onConfirm(() ->{
        deleteArena();
        player.sendMessage("Arena deleted!");
    })
    .onCancel(() ->{
        player.sendMessage("Deletion cancelled.");
    })
    .build().open();
```

### Input Menus

```java
public class NameInputMenu extends InputMenu {
    
    @Override
    public void init() {
        setItem(13, MenuItem.of(nameItem, event -> {
            requestInput(
                "Enter a name:",
                input -> {
                    setName(input);
                    event.player().sendMessage("Name set to: " + input);
                    open(); // Reopen menu
                },
                input -> input.length() <= 16 // Validator
            );
        }));
    }
}
```

## 🛠️ Utility Classes

### Item Builder

```java
ItemStack item = ItemStackBuilder.of(Material.DIAMOND_SWORD)
        .name("&6Legendary Sword", true) // true = no italic
        .lore(true,
                "&7A powerful weapon",
                "&7Damage: &c+10",
                "&7Durability: &a1000"
        )
        .addEnchantment(Enchantment.SHARPNESS, 5)
        .addEnchantment(Enchantment.UNBREAKING, 3)
        .hideAllFlags()
        .unbreakable(true)
        .glintOverride(true)
        .build();
```

### Player Utilities

```java
// Reset player
PlayerUtil.fullResetPlayer(player);

// Heal and feed
PlayerUtil.healPlayer(player);
PlayerUtil.feedPlayer(player);

// Check inventory
boolean full = PlayerUtil.isInventoryFull(player);
boolean hasSpace = PlayerUtil.hasEmptySlots(player, 5);
```

### String Utilities

```java
String formatted = StringUtil.capitalize("hello world"); // "Hello World"
String corrected = StringUtil.punctuationCorrector("hello  ,  world!"); // "hello, world!"
```

### Number Formatting

```java
String formatted = NumberFormatter.formatWithSuffix(1500000); // "1.50M"
String number = NumberFormatter.formatSuffixInputToNumber("5.5k"); // "5,500"
```

### Time Formatting

```java
String readable = TimeFormatter.formatMillsToReadable(125000); // "2 minutes 5 seconds"
String timer = TimeFormatter.formatSecondsToMinSecWithoutText(125); // "02:05"
```

## 🔍 Known Limitations

1. **No Database Integration** - No built-in database support (SQL, MongoDB, etc.)
2. **No Permissions Wrapper** - No integration with LuckPerms, Vault, etc.
3. **No Localization System** - No built-in i18n/multi-language support
4. **No Packet Utilities** - No packet manipulation or protocol lib integration
5. **No Economy Integration** - No Vault economy hooks
6. **No Region Integration** - No WorldGuard/WorldEdit utilities
7. **No Placeholder Support** - No PlaceholderAPI integration
8. **No Metrics** - No bStats or analytics built-in
9. **No Update Checker** - No automatic update checking

These features can be added as separate modules or integrated into your plugin directly.

## 📖 Documentation

For more detailed documentation, please refer to the Javadocs included in the source code. Each class and method is
thoroughly documented with examples and best practices.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to
discuss what you would like to change.

### Development Setup

1. Clone the repository

```bash
git clone https://github.com/sun-mc-dev/SunTools.git
```

2. Build with Maven

```bash
cd Tools
mvn clean install
```

3. Import into your IDE (IntelliJ IDEA recommended)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **LiamDev06** - This library firstly coded by [@LiamDev06](https://github.com/LiamDev06/LiamTools)
- **Paper Team** - For the excellent Paper API
- **Jorel** - For the CommandAPI
- **SpongePowered** - For Configurate
- **Adventure Team** - For the Component API

## 📮 Support

- **Issues**: [GitHub Issues](https://github.com/sun-mc-dev/SunTools/issues)
- **Discussions**: [GitHub Discussions](https://github.com/sun-mc-dev/SunTools/discussions)

## 🗺️ Roadmap

- [ ] Database integration module
- [ ] Permissions system wrapper
- [ ] Localization/i18n system
- [ ] PlaceholderAPI integration
- [ ] Metrics/analytics module
- [ ] Update checker system
- [ ] More pre-built menu types
- [ ] More configuration serializers
- [ ] Performance optimization tools
- [ ] More utility classes

---

**Made with ❤️ by SunMC**
