package org.tom.nettapoc.person;

import org.tom.nettapoc.generic.VersionedEntity;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record Person(Long id, String name, Instant dataVersion) implements VersionedEntity<String> {

    public Person(Long id, String name, String dataVersion) {
        this(id, name, Instant.parse(dataVersion));
    }

    @Override
    public String getDataVersion() {
        return DateTimeFormatter.ISO_INSTANT.format(dataVersion);
    }

    @Override
    public String getId() {
        return id.toString();
    }
}

