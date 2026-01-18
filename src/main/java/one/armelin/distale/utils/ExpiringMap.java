package one.armelin.distale.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiringMap<K, V> {
    private final Map<K, ValueWrapper<V>> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static class ValueWrapper<V> {
        final V value;
        final long expirationTime;

        ValueWrapper(V value, long ttlMillis) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    public ExpiringMap() {
        scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.SECONDS);
    }

    public void put(K key, V value, long ttlMillis) {
        map.put(key, new ValueWrapper<>(value, ttlMillis));
    }

    public V get(K key) {
        ValueWrapper<V> wrapper = map.get(key);
        if (wrapper == null || wrapper.isExpired()) {
            map.remove(key);
            return null;
        }
        return wrapper.value;
    }

    public void remove(K key) {
        map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    private void cleanup() {
        map.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public boolean containsKey(K key) {
        ValueWrapper<V> wrapper = map.get(key);
        if (wrapper == null || wrapper.isExpired()) {
            map.remove(key);
            return false;
        }
        return true;
    }

    public boolean containsValue(V value) {
        cleanup();
        return map.values().stream()
                .filter(wrapper -> !wrapper.isExpired())
                .anyMatch(wrapper -> wrapper.value.equals(value));
    }

    public int size() {
        cleanup();
        return map.size();
    }

    public boolean isEmpty() {
        cleanup();
        return map.isEmpty();
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}