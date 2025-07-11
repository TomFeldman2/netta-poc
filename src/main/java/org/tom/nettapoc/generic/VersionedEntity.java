package org.tom.nettapoc.generic;

import java.util.Objects;

public abstract class VersionedEntity<T extends Comparable<T>> implements Comparable<VersionedEntity<T>> {
    public abstract T getDataVersion();

    public abstract String getId();

    @Override
    public int compareTo(VersionedEntity<T> other) {
        if (this.getDataVersion() == null && other.getDataVersion() == null) return 0;
        if (this.getDataVersion() == null) return -1;
        if (other.getDataVersion() == null) return 1;
        return this.getDataVersion().compareTo(other.getDataVersion());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        return Objects.equals(((VersionedEntity<?>) other).getId(),  getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
