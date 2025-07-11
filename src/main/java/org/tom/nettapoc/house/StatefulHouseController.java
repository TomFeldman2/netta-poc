package org.tom.nettapoc.house;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tom.nettapoc.generic.VersionedExternalService;
import org.tom.nettapoc.generic.CacheDelta;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@Component
public class StatefulHouseController implements VersionedExternalService<House, Integer> {

    private final AtomicInteger currentVersion = new AtomicInteger(0);
    private final Map<String, House> houseStore = new HashMap<>();
    private final Map<String, Integer> deletedHouses = new HashMap<>();
    private final List<Long> possiblePersonIds = new ArrayList<>();

    // Fixed seed for reproducibility
    private final Random random;

    public StatefulHouseController() {
        long seed = 123456789L;  // fixed seed, change as needed
        this.random = new Random(seed);
        init();
    }

    public void init() {
        // Prepare person IDs from 1000 to 1999 (1000 persons)
        for (long i = 1000; i < 2000; i++) {
            possiblePersonIds.add(i);
        }

        // Initialize 20 houses with random 1-5 person IDs each
        for (int i = 1; i <= 20; i++) {
            List<Long> persons = randomSample(possiblePersonIds, random.nextInt(5) + 1);
            addOrUpdateHouse(new House("h" + i, persons, 0));
        }
    }

    private List<Long> randomSample(List<Long> source, int count) {
        List<Long> copy = new ArrayList<>(source);
        Collections.shuffle(copy, random);  // shuffle with fixed seed
        return new ArrayList<>(copy.subList(0, count));
    }

    /**
     * Adds or updates a house.
     * The dataVersion is assigned automatically (incremented global version).
     */
    public void addOrUpdateHouse(House house) {
        int newVersion = currentVersion.incrementAndGet();
        House updatedHouse = new House(house.getId(), house.getPersonIds(), newVersion);
        houseStore.put(house.getId(), updatedHouse);
        deletedHouses.remove(house.getId()); // If previously deleted, remove from deleted map

        System.out.printf("[Version %d] Added/Updated house %s with persons %s%n",
                newVersion, updatedHouse.getId(), updatedHouse.getPersonIds());
    }

    /**
     * Deletes a house by id.
     * Removes it from house store and records deletion version.
     */
    public void deleteHouse(String houseId) {
        if (houseStore.containsKey(houseId)) {
            houseStore.remove(houseId);
            int newVersion = currentVersion.incrementAndGet();
            deletedHouses.put(houseId, newVersion);
            System.out.printf("[Version %d] Deleted house %s%n", newVersion, houseId);
        }
    }

    /**
     * Fetches houses updated or deleted since given dataVersion.
     * Returns updated houses, deleted house IDs, and next data version.
     */
    @Override
    @GetMapping("/houses")
    public CacheDelta<House, Integer> fetchUpdates(@RequestParam(defaultValue = "0") Integer dataVersion) {
        List<House> updated = houseStore.values().stream()
                .filter(h -> h.getDataVersion() > dataVersion)
                .collect(Collectors.toList());

        List<String> deleted = deletedHouses.entrySet().stream()
                .filter(e -> e.getValue() > dataVersion)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int nextVersion = currentVersion.get();

        System.out.printf("Fetch request with version %d: returning %d updated and %d deleted entities, nextVersion=%d%n",
                dataVersion, updated.size(), deleted.size(), nextVersion);

        simulateChanges();

        return new CacheDelta<>(updated, deleted, nextVersion);
    }

    /**
     * Scheduled task runs every 5 seconds and performs 1-3 random simulation steps.
     * Simulation steps: add new house, update existing house, delete existing house.
     */
    public void simulateChanges() {
        int changesCount = random.nextInt(3) + 1; // 1 to 3 changes per run

        for (int i = 0; i < changesCount; i++) {
            int action = random.nextInt(3);
            switch (action) {
                case 0 -> addRandomHouse();
                case 1 -> updateRandomHouse();
                case 2 -> deleteRandomHouse();
            }
        }
    }

    private void addRandomHouse() {
        String id = "h" + (houseStore.size() + deletedHouses.size() + 1 + random.nextInt(1000));
        List<Long> persons = randomSample(possiblePersonIds, random.nextInt(5) + 1);
        addOrUpdateHouse(new House(id, persons, 0));
    }

    private void updateRandomHouse() {
        if (houseStore.isEmpty()) return;
        List<String> keys = new ArrayList<>(houseStore.keySet());
        String id = keys.get(random.nextInt(keys.size()));
        List<Long> persons = randomSample(possiblePersonIds, random.nextInt(5) + 1);
        addOrUpdateHouse(new House(id, persons, 0));
    }

    private void deleteRandomHouse() {
        if (houseStore.isEmpty()) return;
        List<String> keys = new ArrayList<>(houseStore.keySet());
        String id = keys.get(random.nextInt(keys.size()));
        deleteHouse(id);
    }
}