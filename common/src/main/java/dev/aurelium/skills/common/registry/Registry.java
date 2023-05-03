package dev.aurelium.skills.common.registry;

import dev.aurelium.skills.api.util.NamespacedId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class Registry<T, P> {

    private final Class<T> type;
    private final Class<P> propertyType;
    private final Map<NamespacedId, T> registryMap;
    private final Map<T, P> propertyMap;

    public Registry(Class<T> type, Class<P> propertyType) {
        this.type = type;
        this.propertyType = propertyType;
        this.registryMap = new HashMap<>();
        this.propertyMap = new HashMap<>();
    }

    public Class<T> getType() {
        return type;
    }

    public Class<P> getPropertyType() {
        return propertyType;
    }

    @NotNull
    public T get(NamespacedId id) {
        T type = registryMap.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Id " + id + " is not registered in registry " + this.getClass().getSimpleName());
        }
        return type;
    }

    @NotNull
    public P getProperties(T value) {
        P prop = propertyMap.get(value);
        if (prop == null) {
            throw new IllegalArgumentException("Value " + value + " is not registered in registry " + this.getClass().getSimpleName());
        }
        return prop;
    }

    public Collection<T> getValues() {
        return registryMap.values();
    }

    public void register(@NotNull NamespacedId id, @NotNull T value, @NotNull P properties) {
        registryMap.put(id, value);
        propertyMap.put(value, properties);
    }

    public void unregister(NamespacedId id) {
        propertyMap.remove(get(id));
        registryMap.remove(id);
    }

    public abstract void registerDefaults();

}
