package org.tom.nettapoc.generic;

public class TestEntity extends VersionedEntity<Integer> {
    private String id;
    private String value;
    private Integer dataVersion;

    public TestEntity(String id, String value, Integer dataVersion) {
        this.id = id;
        this.value = value;
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

    public String getValue() {
        return value;
    }
}
