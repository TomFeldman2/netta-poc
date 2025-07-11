package org.tom.nettapoc.house;

import org.tom.nettapoc.generic.VersionedEntity;

import java.util.List;
import java.util.Objects; // Import Objects for equals/hashCode if you override them in House

public class House extends VersionedEntity<Integer> {
    private final String id;
    private final List<Long> personIds;
    private final Integer dataVersion;

    public House(String id, List<Long> personIds, Integer dataVersion) {
        this.id = id;
        this.personIds = personIds;
        this.dataVersion = dataVersion;
    }

    @Override
    public Integer getDataVersion() {
        return dataVersion;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<Long> getPersonIds() {
        return personIds;
    }
}