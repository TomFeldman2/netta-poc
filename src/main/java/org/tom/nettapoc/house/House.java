package org.tom.nettapoc.house;

import org.tom.nettapoc.generic.VersionedEntity;

import java.util.List;

public record House(String id, List<Long> personIds, Integer dataVersion) implements VersionedEntity<Integer> {
    @Override
    public Integer getDataVersion() {
        return dataVersion;
    }

    @Override
    public String getId() {
        return id;
    }
}
