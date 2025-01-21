package dev.compactmods.gander.render.pipeline;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class PipelineState {

    public record Item<T>(Class<T> type) {}

    private final Reference2ObjectOpenHashMap<Item<?>, Object> properties = new Reference2ObjectOpenHashMap<>();

    public <T> T get(Item<T> property) {
        Object val = properties.get(property);
        if(property.type.isInstance(val)) {
            return property.type.cast(val);
        }

        return null;
    }

    public <T> T getOrDefault(Item<T> property, T defaultValue) {
        var t = get(property);
        return t == null ? defaultValue : t;
    }

    public <T> void set(Item<T> property, T value) {
        this.properties.put(property, value);
    }
}
