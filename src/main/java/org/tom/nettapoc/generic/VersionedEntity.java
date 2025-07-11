package org.tom.nettapoc.generic;

public interface VersionedEntity<T extends Comparable<T>> extends Comparable<VersionedEntity<T>>{
    T getDataVersion();
    String getId();

    @Override
    default int compareTo(VersionedEntity<T> other) {
        if (this.getDataVersion() == null && other.getDataVersion() == null) return 0;
        if (this.getDataVersion() == null) return -1;
        if (other.getDataVersion() == null) return 1;
        return this.getDataVersion().compareTo(other.getDataVersion());
    }
}
