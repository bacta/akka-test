package io.bacta.messages.creature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SetPropertiesMessage implements Map<String, Object> {
    private final Map<String, Object> property = new HashMap<>(50);

    @Override
    public int size() {
        return property.size();
    }

    @Override
    public boolean isEmpty() {
        return property.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return property.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return property.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return property.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return property.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return property.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        property.putAll(m);
    }

    @Override
    public void clear() {
        property.clear();
    }

    @Override
    public Set<String> keySet() {
        return property.keySet();
    }

    @Override
    public Collection<Object> values() {
        return property.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return property.entrySet();
    }
}
