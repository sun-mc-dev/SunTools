package me.sunmc.tools.registry;

import me.sunmc.tools.Tools;
import me.sunmc.tools.registry.component.AutoRegisteringFeature;
import me.sunmc.tools.utils.java.SinglePointInitiator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced factory for instantiating classes and registering their instances.
 *
 * <p>Features:
 * - Automatic class discovery via reflection
 * - Constructor dependency injection
 * - Singleton pattern support
 * - Lazy initialization
 * - Thread-safe instance caching
 * - Multiple constructor support
 * - Interface implementation lookup
 * - Performance metrics
 *
 * @version 1.0.0
 */
public class RegistryFactory extends SinglePointInitiator {

    private final @NonNull Reflections reflections;
    private final @NonNull Map<Class<?>, Object> registry;
    private final @NonNull Map<String, Object> namedRegistry;
    private final @NonNull Object mainClassInstance;
    private final @NonNull Class<?> mainClass;
    private final @NonNull Set<AutoRegisteringFeature> autoRegisteringComponents;
    private final @NonNull Map<Class<?>, Long> instantiationTimes;

    private boolean trackPerformance = false;

    public <T extends Tools> RegistryFactory(@NonNull Reflections reflections, @NonNull T mainClassInstance) {
        this.reflections = reflections;
        this.registry = new ConcurrentHashMap<>();
        this.namedRegistry = new ConcurrentHashMap<>();
        this.mainClassInstance = mainClassInstance;
        this.mainClass = mainClassInstance.getClass();
        this.autoRegisteringComponents = new LinkedHashSet<>();
        this.instantiationTimes = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new instance of the provided class or returns an existing instance.
     * Thread-safe and supports dependency injection.
     *
     * @param clazz The {@link Class} to create an instance of, or return an existing instance.
     * @return The newly created or already existing instance.
     */
    public @Nullable Object createEffectiveInstance(@NonNull Class<?> clazz) {
        Object classInstance = this.getClassInstance(clazz);
        if (classInstance != null) {
            return classInstance;
        }
        return this.createInstance(clazz);
    }

    /**
     * Creates a new instance by initializing the constructor of the provided class.
     * Supports multiple constructor types and automatic dependency resolution.
     *
     * @param clazz The {@link Class} to create an instance of.
     * @return The created instance.
     */
    public @Nullable Object createInstance(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        // Check if class is abstract or interface
        if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
            Tools.LOG.warn("Cannot instantiate abstract class or interface: {}", clazz.getSimpleName());
            return null;
        }

        final String displayName = clazz.getSimpleName() + " (" + clazz.getPackageName() + ")";
        long startTime = this.trackPerformance ? System.nanoTime() : 0;

        try {
            Constructor<?>[] constructors = clazz.getConstructors();
            if (constructors.length == 0) {
                throw new NoSuchMethodException("Class " + displayName + " has no public constructor.");
            }

            Object instance = null;

            // Try to find and use the best matching constructor
            for (Constructor<?> constructor : constructors) {
                try {
                    instance = this.tryCreateInstance(constructor);
                    if (instance != null) {
                        break;
                    }
                } catch (Exception e) {
                    // Try next constructor
                    continue;
                }
            }

            if (instance == null) {
                throw new RuntimeException("No suitable constructor found for " + displayName);
            }

            // Register instance
            if (this.registry.putIfAbsent(clazz, instance) != null) {
                throw new UnsupportedOperationException("Duplicate class registration of class " + displayName + ".");
            }

            // Track performance if enabled
            if (this.trackPerformance) {
                long duration = System.nanoTime() - startTime;
                this.instantiationTimes.put(clazz, duration);
            }

            return instance;

        } catch (Exception exception) {
            Tools.LOG.error("Registry Factory could not create an instance for class {}", clazz.getSimpleName(), exception);
            return null;
        }
    }

    /**
     * Tries to create an instance using the provided constructor.
     */
    private @Nullable Object tryCreateInstance(@NonNull Constructor<?> constructor) throws Exception {
        int paramCount = constructor.getParameterCount();

        if (paramCount == 0) {
            // No-arg constructor
            return constructor.newInstance();
        } else if (paramCount == 1) {
            Class<?> paramType = constructor.getParameterTypes()[0];

            // Plugin parameter
            if (paramType.isAssignableFrom(this.mainClass)) {
                return constructor.newInstance(this.mainClassInstance);
            }

            // Try to resolve dependency
            Object dependency = this.getClassInstance(paramType);
            if (dependency != null) {
                return constructor.newInstance(dependency);
            }
        } else {
            // Multiple parameters - try to resolve all
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i].isAssignableFrom(this.mainClass)) {
                    params[i] = this.mainClassInstance;
                } else {
                    params[i] = this.getClassInstance(paramTypes[i]);
                    if (params[i] == null) {
                        return null; // Can't resolve all dependencies
                    }
                }
            }

            return constructor.newInstance(params);
        }

        return null;
    }

    /**
     * Registers an instance with a custom name.
     *
     * @param name The name to register under.
     * @param instance The instance to register.
     */
    public void registerNamed(@NonNull String name, @NonNull Object instance) {
        this.namedRegistry.put(name, instance);
        Tools.LOG.debug("Registered named instance: {}", name);
    }

    /**
     * Gets a named instance.
     *
     * @param name The name of the instance.
     * @return The instance, or null if not found.
     */
    public @Nullable Object getNamedInstance(@NonNull String name) {
        return this.namedRegistry.get(name);
    }

    /**
     * Gets an instance by its class name.
     *
     * @param className The fully qualified class name.
     * @return The instance, or null if not found.
     */
    public @Nullable Object getInstance(@NonNull String className) {
        return this.registry.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(className))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the provided parameter class with {@link AutoRegister} annotation present has
     * {@code shouldLog} set to {@code true} or {@code false}.
     *
     * @param clazz The target {@link Class} to check for.
     * @return If the target class with {@link AutoRegister} present should log or not.
     */
    public boolean isLoggingEnabled(@NonNull Class<?> clazz) {
        if (clazz.isAnnotationPresent(AutoRegister.class)) {
            return clazz.getAnnotation(AutoRegister.class).shouldLog();
        }
        return false;
    }

    /**
     * Registers an {@link AutoRegisteringFeature}.
     *
     * @param component Instance of the component that supports auto registering its managed features.
     */
    public void registerAutoRegisteringComponent(@NonNull AutoRegisteringFeature component) {
        this.autoRegisteringComponents.add(component);
    }

    /**
     * Executes the implementation of all {@link AutoRegisteringFeature} which instantiates all auto registering classes.
     */
    public void executeAllAutoRegistering() {
        this.autoRegisteringComponents.forEach(component -> component.executeAutoRegistering(this));
    }

    /**
     * Enables performance tracking.
     *
     * @param track True to enable, false to disable.
     */
    public void setTrackPerformance(boolean track) {
        this.trackPerformance = track;
    }

    /**
     * Gets instantiation time for a class (if performance tracking is enabled).
     *
     * @param clazz The class to get instantiation time for.
     * @return Instantiation time in nanoseconds, or -1 if not tracked.
     */
    public long getInstantiationTime(@NonNull Class<?> clazz) {
        return this.instantiationTimes.getOrDefault(clazz, -1L);
    }

    /**
     * Gets all instantiation times.
     *
     * @return Map of class to instantiation time in nanoseconds.
     */
    public @NonNull Map<Class<?>, Long> getAllInstantiationTimes() {
        return new HashMap<>(this.instantiationTimes);
    }

    /**
     * @param clazz The class whose instance to get.
     * @return The instance of the provided {@param clazz}. Returns {@code null} if non is present.
     */
    public @Nullable Object getClassInstance(@NonNull Class<?> clazz) {
        return this.registry.get(clazz);
    }

    /**
     * Gets all registered instances.
     *
     * @return Unmodifiable map of all registered instances.
     */
    public @NonNull Map<Class<?>, Object> getAllInstances() {
        return Collections.unmodifiableMap(this.registry);
    }

    /**
     * Checks if a class has been instantiated.
     *
     * @param clazz The class to check.
     * @return True if instantiated, false otherwise.
     */
    public boolean isInstantiated(@NonNull Class<?> clazz) {
        return this.registry.containsKey(clazz);
    }

    /**
     * Gets the number of registered instances.
     *
     * @return The count of registered instances.
     */
    public int getInstanceCount() {
        return this.registry.size();
    }

    /**
     * Gets all classes annotated with {@link AutoRegister} and then
     * filters out to only return the ones that matches the method parameter input class.
     *
     * @param registerClass The type of auto registered classes to return.
     * @return Set of classes that are marked with {@link AutoRegister} and has the input class type.
     */
    public @NonNull Set<Class<?>> getClassesWithRegistryType(@NonNull Class<?> registerClass) {
        return this.reflections.getTypesAnnotatedWith(AutoRegister.class)
                .stream()
                .filter(foundClass -> Arrays.stream(foundClass.getAnnotation(AutoRegister.class).value()).toList().contains(registerClass))
                .filter(foundClass -> registerClass.isAssignableFrom(foundClass) || foundClass.equals(registerClass))
                .collect(Collectors.toSet());
    }

    /**
     * Gets all classes annotated with {@link AutoRegister} and that extends the parameter {@code registerClass}.
     * The method also filters out to only return the ones that matches the input registering class.
     *
     * @param registerClass The type of auto registered classes to return.
     * @param extendClass   The {@link Class<T>} that the found auto registered classes also must extend.
     * @return Set of classes that are marked with {@link AutoRegister}, extends {@link Class<T>} and has the {@param type} type value.
     */
    public <T> @NonNull Set<Class<? extends T>> getClassesWithRegistryType(@NonNull Class<?> registerClass, @NonNull Class<T> extendClass) {
        return this.getClassesWithRegistryType(registerClass)
                .stream()
                .filter(extendClass::isAssignableFrom)
                .map(foundClass -> (Class<? extends T>) foundClass)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all classes annotated with parameter {@code annotation}.
     *
     * @param annotation The {@link Annotation} to filter classes to return.
     * @return Set of classes that are annotated with the parameter {@code annotation}.
     */
    public @NonNull Set<Class<?>> getClassesWithAnnotation(@NonNull Class<? extends Annotation> annotation) {
        return this.reflections.getTypesAnnotatedWith(annotation);
    }

    /**
     * Gets all classes annotated with the parameter {@code annotation}.
     *
     * @param annotation  The {@link Annotation} to filter classes to return.
     * @param extendClass The {@link Class} the classes marked with the annotation also must extend.
     * @return Set of classes that are marked with the parameter {@code annotation} and that extend the specific extend class.
     */
    public <T> @NonNull Set<Class<? extends T>> getClassesWithAnnotation(@NonNull Class<? extends Annotation> annotation, @NonNull Class<T> extendClass) {
        return this.getClassesWithAnnotation(annotation)
                .stream()
                .filter(extendClass::isAssignableFrom)
                .map(foundClass -> (Class<? extends T>) foundClass)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all classes that implement the provided interface.
     *
     * @param interfaceClass Interface class to implement.
     * @return Set of classes that implement the {@code interfaceClass}.
     */
    public <T> @NonNull Set<Class<? extends T>> getClassesImplementing(@NonNull Class<T> interfaceClass) {
        return this.reflections.getSubTypesOf(interfaceClass);
    }

    /**
     * Finds all classes implementing a specific interface.
     * Alternative method name for clarity.
     *
     * @param interfaceClass The interface to search for.
     * @return Set of classes implementing the interface.
     */
    public <T> @NonNull Set<Class<? extends T>> findClassesImplementingInterface(@NonNull Class<T> interfaceClass) {
        return this.getClassesImplementing(interfaceClass);
    }

    /**
     * Clears all cached instances.
     * Warning: This will not call any cleanup methods!
     */
    public void clearCache() {
        this.registry.clear();
        this.namedRegistry.clear();
        this.instantiationTimes.clear();
        Tools.LOG.warn("Registry cache cleared");
    }
}