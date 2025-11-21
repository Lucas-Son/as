package com.fiap.esoa.salesmind.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memória com suporte a TTL (Time To Live).
 * Thread-safe usando ConcurrentHashMap.
 * 
 * @param <K> Tipo da chave
 * @param <V> Tipo do valor
 */
public class CacheManager<K, V> {

    private final Map<K, CacheEntry<V>> cache;
    private final long ttlMillis;

    private static class CacheEntry<V> {
        private final V value;
        private final LocalDateTime expiresAt;

        public CacheEntry(V value, LocalDateTime expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        public V getValue() {
            return value;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * @param ttlMinutes Tempo de vida em minutos
     */
    public CacheManager(long ttlMinutes) {
        this.cache = new ConcurrentHashMap<>();
        this.ttlMillis = ttlMinutes * 60 * 1000;
    }

    /**
     * @param key Chave do cache
     * @param value Valor a ser armazenado
     */
    public void put(K key, V value) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlMillis / 1000);
        cache.put(key, new CacheEntry<>(value, expiresAt));
    }

    /**
     * @param key Chave do cache
     * @return Valor armazenado ou null se não encontrado ou expirado
     */
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.getValue();
    }

    /**
     * @param key Chave a ser invalidada
     */
    public void invalidate(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    /**
     * Remove entradas expiradas. Deve ser chamado periodicamente.
     */
    public void evictExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * @return Número de entradas no cache (incluindo expiradas)
     */
    public int size() {
        return cache.size();
    }

    /**
     * @param key Chave a verificar
     * @return true se existe e é válida, false caso contrário
     */
    public boolean containsKey(K key) {
        return get(key) != null;
    }
}
