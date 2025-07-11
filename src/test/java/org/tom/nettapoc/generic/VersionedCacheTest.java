package org.tom.nettapoc.generic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class VersionedCacheTest {

    private VersionedCache<TestEntity, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new VersionedCache<>();
    }

    private CacheDelta<TestEntity, Integer> deltaWithData(List<TestEntity> data, Integer version) {
        return new CacheDelta<>(data, Collections.emptyList(), version);
    }

    private CacheDelta<TestEntity, Integer> deltaWithDeletes(List<String> deleted, Integer version) {
        return new CacheDelta<>(Collections.emptyList(), deleted, version);
    }

    private CacheDelta<TestEntity, Integer> deltaWithDataAndDeletes(List<TestEntity> data, List<String> deleted, Integer version) {
        return new CacheDelta<>(data, deleted, version);
    }

    @Test
    void initialVersionIsNull() {
        assertNull(cache.getCurrentVersion());
    }

    @Test
    void applyDeltaAddsEntitiesAndUpdatesVersion() {
        var d = deltaWithData(List.of(new TestEntity("1", "A", 10)), 10);
        cache.applyDeltaToCache(d);
        assertEquals(10, cache.getCurrentVersion());
        assertEquals("A", cache.getById("1").getValue());
    }

    @Test
    void applyDeltaUpdatesEntitiesAndVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 5)), 5));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 8)), 8));
        assertEquals("B", cache.getById("1").getValue());
        assertEquals(8, cache.getCurrentVersion());
    }

    @Test
    void applyDeltaThrowsOnVersionRegressionForAdds() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), 10));
        var ex = assertThrows(IllegalArgumentException.class,
                () -> cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 9)), 9)));
        assertTrue(ex.getMessage().contains("older than current"));
    }

    @Test
    void applyDeltaAllowsEqualVersionForAdds() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), 10));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 10)), 10));
        assertEquals("B", cache.getById("1").getValue());
    }

    @Test
    void applyDeltaDeletesEntitiesAndUpdatesVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 3)), 3));
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 4));
        assertNull(cache.getById("1"));
        assertTrue(cache.getDeletedFromVersion(0).contains("1"));
        assertEquals(4, cache.getCurrentVersion());
    }

    @Test
    void applyDeltaThrowsOnVersionRegressionForDeletes() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), 10));
        assertThrows(IllegalArgumentException.class,
                () -> cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 9)));
    }

    @Test
    void applyDeltaDeletesNonExistentAddsToDeletedIndex() {
        cache.applyDeltaToCache(deltaWithDeletes(List.of("999"), 5));
        assertTrue(cache.getDeletedFromVersion(0).contains("999"));
        assertEquals(5, cache.getCurrentVersion());
    }

    @Test
    void applyDeltaReAddsAfterDeleteRemovesFromDeletedIndex() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 1)), 1));
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 2));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 3)), 3));

        assertEquals("B", cache.getById("1").getValue());
        assertFalse(cache.getDeletedFromVersion(0).contains("1"));
        assertEquals(3, cache.getCurrentVersion());
    }

    @Test
    void applyDeltaWithMultipleEntitiesSameVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(
                new TestEntity("1", "A", 5),
                new TestEntity("2", "B", 5)
        ), 5));
        var entities = cache.getEntitiesFromVersion(5);
        assertEquals(2, entities.size());
    }

    @Test
    void getEntitiesFromFutureVersionReturnsEmpty() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 1)), 1));
        assertTrue(cache.getEntitiesFromVersion(10).isEmpty());
    }

    @Test
    void multipleDeletesOnSameIdKeepOnlyLatest() {
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 5));
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 6));

        assertEquals(List.of("1"), cache.getDeletedFromVersion(5));
        assertEquals(List.of("1"), cache.getDeletedFromVersion(6));
        assertTrue(cache.getDeletedFromVersion(7).isEmpty());
    }

    @Test
    void getDeletedFromVersionIsInclusive() {
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 5));
        assertTrue(cache.getDeletedFromVersion(5).contains("1"));
    }

    @Test
    void applyDeltaUpdatesVersionIndexCorrectly() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 5)), 5));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("2", "B", 10)), 10));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "C", 12)), 12)); // update existing

        var allEntities = cache.getEntitiesFromVersion(5);
        assertEquals(2, allEntities.size());
        assertTrue(allEntities.stream().anyMatch(e -> e.getValue().equals("B")));
        assertTrue(allEntities.stream().anyMatch(e -> e.getValue().equals("C")));
    }

    @Test
    void updateAfterDeletionRemovesFromDeletedIndex() {
        cache.applyDeltaToCache(deltaWithDeletes(List.of("1"), 5));
        assertTrue(cache.getDeletedFromVersion(0).contains("1"));

        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "Restored", 6)), 6));
        assertFalse(cache.getDeletedFromVersion(0).contains("1"));
        assertEquals("Restored", cache.getById("1").getValue());
    }

    @Test
    void getByIdReturnsNullForMissingEntity() {
        assertNull(cache.getById("missing"));
    }

    @Test
    void reAddAfterDeleteRemovesFromDeletedIndex() {
        cache.applyDeltaToCache(deltaWithDeletes(List.of("reAddMe"), 10));
        assertTrue(cache.getDeletedFromVersion(0).contains("reAddMe"));

        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("reAddMe", "I’m Back", 11)), 11));
        assertFalse(cache.getDeletedFromVersion(0).contains("reAddMe"));
        assertEquals("I’m Back", cache.getById("reAddMe").getValue());
    }
}
