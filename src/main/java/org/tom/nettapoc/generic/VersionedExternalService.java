package org.tom.nettapoc.generic;

public interface VersionedExternalService<E extends VersionedEntity<V>, V extends Comparable<V>> {

    /**
     * Fetches entities updated or deleted since the given dataVersion.
     *
     * @param dataVersion The version to fetch updates since.
     * @return VersionedServiceResponse containing updated entities, deleted entity IDs, and next version.
     */
    CacheDelta<E, V> fetchUpdates(V dataVersion);
}
