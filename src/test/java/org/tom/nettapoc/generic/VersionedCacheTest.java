package org.tom.nettapoc.generic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VersionedCacheTest {

    private VersionedCache<TestEntity, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new VersionedCache<>();
    }

    private CacheDelta<TestEntity, Integer> deltaWithData(List<TestEntity> data, List<String> deleted, Integer version) {
        return new CacheDelta<>(data, deleted, version);
    }

    @Test
    void testInitialVersionIsNull() {
        assertNull(cache.getCurrentVersion());
    }

    @Test
    void testAddSingleEntitySetsVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), null, 10));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(10, cache.getCurrentVersion());
        assertEquals(1, delta.data().size());
        assertEquals("A", delta.data().get(0).getValue());
        assertTrue(delta.deleted().isEmpty());
    }

    @Test
    void testUpdateEntityIncreasesVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 5)), null, 5));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 8)), null, 8));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(8, cache.getCurrentVersion());
        assertEquals(1, delta.data().size());
        assertEquals("B", delta.data().get(0).getValue());
        assertTrue(delta.deleted().isEmpty());
    }

    @Test
    void testVersionRegressionOnUpdateThrows() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), null, 10));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 9)), null, 9)));
        assertTrue(ex.getMessage().contains("older than current"));
    }

    @Test
    void testVersionRegressionOnDeleteThrows() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), null, 10));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 9)));
        assertTrue(ex.getMessage().contains("older than current"));
    }

    @Test
    void testDeleteRemovesEntity() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 3)), null, 3));
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 4));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(4, cache.getCurrentVersion());
        assertTrue(delta.deleted().contains("1"));
        assertFalse(delta.data().stream().anyMatch(e -> e.getId().equals("1")));
        assertNull(cache.getById("1"));
    }

    @Test
    void testDeleteNonExistentAddsToDeletedIndex() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("999"), 5));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(5, cache.getCurrentVersion());
        assertTrue(delta.deleted().contains("999"));
    }

    @Test
    void testReAddAfterDelete() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 1)), null, 1));
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 2));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 3)), null, 3));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(3, cache.getCurrentVersion());
        assertFalse(delta.deleted().contains("1"));
        assertEquals("B", cache.getById("1").getValue());
    }

    @Test
    void testMultipleEntitiesSameVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(
                new TestEntity("1", "A", 5),
                new TestEntity("2", "B", 5)), null, 5));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(5, cache.getCurrentVersion());
        assertEquals(2, delta.data().size());
    }

    @Test
    void testGetDeltaFromFutureReturnsEmpty() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 1)), null, 1));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(10);
        assertTrue(delta.data().isEmpty());
        assertTrue(delta.deleted().isEmpty());
    }

    @Test
    void testDeletedRewrittenWithNewerVersion() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 5));
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 6)); // newer delete

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals(1, delta.deleted().size());
        assertEquals("1", delta.deleted().get(0));
    }

    @Test
    void testGetDeltaFromVersionBoundaryInclusive() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 5));

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(5);
        assertTrue(delta.deleted().contains("1"));
    }

    @Test
    void testAddUpdatesVersionIndexCorrectly() {
        cache.applyDeltaToCache(deltaWithData(List.of(
                new TestEntity("1", "A", 5),
                new TestEntity("2", "B", 10)), null, 10));
        cache.applyDeltaToCache(deltaWithData(List.of(
                new TestEntity("1", "C", 12)), null, 12)); // update existing

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(5);
        assertEquals(2, delta.data().size());
        assertTrue(delta.data().stream().anyMatch(e -> e.getValue().equals("B")));
        assertTrue(delta.data().stream().anyMatch(e -> e.getValue().equals("C")));
    }

    @Test
    void testMultipleDeletesSameIdKeepsOnlyLatest() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 5));
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 6));

        CacheDelta<TestEntity, Integer> deltaFrom5 = cache.getDelta(5);
        assertEquals(List.of("1"), deltaFrom5.deleted());

        CacheDelta<TestEntity, Integer> deltaFrom6 = cache.getDelta(6);
        assertEquals(List.of("1"), deltaFrom6.deleted());

        CacheDelta<TestEntity, Integer> deltaFrom7 = cache.getDelta(7);
        assertTrue(deltaFrom7.deleted().isEmpty(), "No deletions after version 6");
    }

    @Test
    void testUpdateAfterDeletionRemovesFromDeleted() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("1"), 5));
        CacheDelta<TestEntity, Integer> deltaBefore = cache.getDelta(0);
        assertTrue(deltaBefore.deleted().contains("1"));

        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "Restored", 6)), null, 6));
        CacheDelta<TestEntity, Integer> deltaAfter = cache.getDelta(0);
        assertFalse(deltaAfter.deleted().contains("1"));
        assertEquals("Restored", cache.getById("1").getValue());
    }

    @Test
    void testGetByIdReturnsNullForMissing() {
        assertNull(cache.getById("not-there"));
    }

    @Test
    void testReAddAfterDeleteRemovesFromDeletedIndex() {
        cache.applyDeltaToCache(deltaWithData(null, List.of("reAddMe"), 10));
        CacheDelta<TestEntity, Integer> deltaBefore = cache.getDelta(0);
        assertTrue(deltaBefore.deleted().contains("reAddMe"));

        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("reAddMe", "I’m Back", 11)), null, 11));
        CacheDelta<TestEntity, Integer> deltaAfter = cache.getDelta(0);
        assertFalse(deltaAfter.deleted().contains("reAddMe"));
        assertEquals("I’m Back", cache.getById("reAddMe").getValue());
    }

    @Test
    void testCheckVersionRejectsOlder() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), null, 10));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 9)), null, 9)));
        assertTrue(ex.getMessage().contains("older than current"));
    }

    @Test
    void testCheckVersionAcceptsEqualVersion() {
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "A", 10)), null, 10));
        cache.applyDeltaToCache(deltaWithData(List.of(new TestEntity("1", "B", 10)), null, 10)); // equal version allowed

        CacheDelta<TestEntity, Integer> delta = cache.getDelta(0);
        assertEquals("B", cache.getById("1").getValue());
    }
}
