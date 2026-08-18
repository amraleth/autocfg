package dev.amraleth.autocfg;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.Nullable;

/**
 * Registry of converters translating between configuration nodes and values.
 *
 * <p>Types that are neither primitives, strings, enums nor records require a converter to be usable
 * as a configuration component. Common Bukkit and JDK types are registered by default; further types
 * are registered by the consumer:
 *
 * <pre>
 * {@code
 * ConfigConverters.register(UUID.class,
 *         node -> UUID.fromString(node.toString()),
 *         UUID::toString);
 * }
 * </pre>
 *
 * <p>A converter must be registered before the configuration that uses it is loaded.
 *
 * <p>The registry is static, and therefore scoped to the classloader holding this class: a copy
 * shaded into a plugin has a registry of its own, a server-wide installation shares one across all
 * plugins.
 *
 * @author amraleth
 * @since 1.0.0
 */
public final class ConfigConverters {

    /**
     * Represents a converter for translating values from and to nodes.
     *
     * @param fromNode The function for transforming from a node to a value.
     * @param toNode   The function for transforming from a value to a node.
     */
    record Converter(Function<Object, Object> fromNode,
                     Function<Object, Object> toNode) {
    }

    /**
     * The registered converters, keyed by the type they produce.
     */
    private static final Map<Class<?>, Converter> REGISTRY = new ConcurrentHashMap<>();

    static {
        register(Duration.class,
                node -> Duration.parse(node.toString()),
                Duration::toString);
        register(NamespacedKey.class,
                node -> key(node.toString()),
                NamespacedKey::toString);
        register(Material.class,
                node -> material(node.toString()),
                material -> material.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Prevents instantiation of this utility class.
     */
    private ConfigConverters() {
    }

    /**
     * Registers a converter for a type, replacing any converter previously registered for it.
     *
     * @param type     The type the converter produces.
     * @param fromNode The function for transforming from a node to a value.
     * @param toNode   The function for transforming from a value to a node.
     * @param <T>      The type the converter produces.
     */
    public static <T> void register(Class<T> type,
                                    Function<Object, T> fromNode,
                                    Function<T, Object> toNode) {
        REGISTRY.put(type, new Converter(fromNode::apply, value -> toNode.apply(type.cast(value))));
    }

    /**
     * Fetches the converter registered for a type.
     *
     * @param type The type to fetch the converter for.
     * @return The converter, or {@code null} if none is registered.
     */
    static @Nullable Converter converter(Class<?> type) {
        return REGISTRY.get(type);
    }

    /**
     * Parses a namespaced key from a literal.
     *
     * @param literal The literal.
     * @return The namespaced key.
     * @throws IllegalArgumentException If the literal is not a valid namespaced key.
     */
    private static NamespacedKey key(String literal) {
        NamespacedKey key = NamespacedKey.fromString(literal);
        if (key == null) {
            throw new IllegalArgumentException("Invalid namespaced key " + literal);
        }
        return key;
    }

    /**
     * Fetches a material from a literal.
     *
     * @param literal The literal.
     * @return The material.
     * @throws IllegalArgumentException If the material is unknown or a legacy material.
     */
    private static Material material(String literal) {
        Material material = Material.matchMaterial(literal);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material " + literal);
        }
        if (material.isLegacy()) {
            throw new IllegalArgumentException("Legacy material not permitted: " + literal);
        }
        return material;
    }
}
