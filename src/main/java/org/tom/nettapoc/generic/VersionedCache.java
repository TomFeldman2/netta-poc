package org.tom.nettapoc.generic;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe-versioned cache optimized for
 * single writer thread and multiple concurrent readers.
 * <p>
 * Enforces monotonic dataVersion updates: any new update or delete
 * must have version ≥ current cache version or else throws.
 *
 * @param <E> Entity type implementing VersionedEntity<V>
 * @param <V> Version type (must be Comparable)
 */
public class VersionedCache<E extends VersionedEntity<V>, V extends Comparable<V>> {

    private final Map<String, E> entitiesById = new HashMap<>();
    private final NavigableMap<V, Set<String>> versionIndex = new TreeMap<>();


    private final Map<String, V> deletedIdsToVersion = new HashMap<>();
    private final NavigableMap<V, Set<String>> deletedIndex = new TreeMap<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private volatile V currentVersion;

    public VersionedCache() {
        currentVersion = null;
    }

    public VersionedCache(V initialVersion) {
        currentVersion = initialVersion;
    }

    public void applyDeltaToCache(CacheDelta<E, V> delta) {
        V responseVersion = delta.nextDataVersion();
        checkVersion(responseVersion);

        rwLock.writeLock().lock();
        try {
            if (delta.data() != null) {
                delta.data().forEach(this::add);
            }

            if (delta.deleted() != null) {
                delta.deleted().forEach(id -> delete(id, responseVersion));
            }

            commit(responseVersion);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Add or update an entity with a version check.
     *
     * @throws IllegalArgumentException if entity's version < currentVersion
     */
    private void add(E entity) {
        String id = entity.getId();
        removeFromCache(id);

        entitiesById.put(id, entity);
        versionIndex.computeIfAbsent(entity.getDataVersion(), k -> new HashSet<>()).add(id);
    }

    /**
     * Delete an entity by id with a version check.
     *
     * @throws IllegalArgumentException if deletionVersion < currentVersion
     */
    private void delete(String id, V deletionVersion) {
        removeFromCache(id);

        deletedIdsToVersion.put(id, deletionVersion);
        deletedIndex.computeIfAbsent(deletionVersion, k -> new HashSet<>()).add(id);
    }

    private void removeFromCache(String id) {
        E oldEntity = entitiesById.remove(id);
        if (oldEntity != null) {
            V oldVersion = oldEntity.getDataVersion();
            Set<String> ids = versionIndex.get(oldVersion);
            ids.remove(id);
            if (ids.isEmpty()) {
                versionIndex.remove(oldVersion);
            }
            return;
        }

        V oldVersion = deletedIdsToVersion.remove(id);
        if (oldVersion == null) return;

        Set<String> ids = deletedIndex.get(oldVersion);
        ids.remove(id);
        if (ids.isEmpty()) {
            deletedIndex.remove(oldVersion);
        }
    }

    public CacheDelta<E, V> getDelta(V version) {
        rwLock.readLock().lock();
        try {
            return new CacheDelta<>(
                    getEntitiesFromVersion(version),
                    getDeletedFromVersion(version),
                    currentVersion
            );
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private List<E> getEntitiesFromVersion(V fromVersion) {
        return versionIndex.tailMap(fromVersion, true).values().stream()
                .flatMap(Set::stream)
                .map(entitiesById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getDeletedFromVersion(V fromVersion) {
        return deletedIndex.tailMap(fromVersion, true).values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    /**
     * Gets the current highest version in the cache.
     */
    public V getCurrentVersion() {
        return currentVersion;
    }

    public void commit(V newVersion) {
        currentVersion = newVersion;
    }

    public void checkVersion(V newVersion) {
        if (currentVersion != null && newVersion.compareTo(currentVersion) < 0) {
            throw new IllegalArgumentException(
                    "New version " + newVersion + " is older than current cache version " + currentVersion);
        }
    }

    public E getById(String id) {
        return entitiesById.get(id);
    }
}
