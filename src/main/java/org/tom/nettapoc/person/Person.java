package org.tom.nettapoc.person;

import org.tom.nettapoc.generic.VersionedEntity;

import java.time.Instant;

public class Person extends VersionedEntity<String> {

    private Long id;
    private String name;
    private Instant dataVersion;

    public Person(Long id, String name, String dataVersion) {
        this(id, name, Instant.parse(dataVersion));
    }

    public Person(Long id, String name, Instant dataVersion) {
        this.id = id;
        this.name = name;
        this.dataVersion = dataVersion;
    }

    @Override
    public String getDataVersion() {
        return dataVersion.toString();
    }

    @Override
    public String getId() {
        return id.toString();
    }

    public String getName() {
        return name;
    }
}

