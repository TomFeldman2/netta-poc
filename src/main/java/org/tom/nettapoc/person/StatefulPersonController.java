package org.tom.nettapoc.person;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tom.nettapoc.generic.VersionedExternalService;
import org.tom.nettapoc.generic.VersionedServiceResponse;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@Component
public class StatefulPersonController implements VersionedExternalService<Person, String> {

    private final AtomicReference<Instant> currentVersion = new AtomicReference<>(Instant.EPOCH);
    private final Map<Long, Person> personStore = new HashMap<>();
    private final Map<Long, Instant> deletedPersons = new HashMap<>();

    private final Random random;

    public StatefulPersonController() {
        long seed = 123456789L; // Fixed seed for reproducibility
        this.random = new Random(seed);
        init();
    }

    public void init() {
        for (long i = 1000; i < 2000; i++) {
            String name = randomName();
            addOrUpdatePerson(new Person(i, name, Instant.EPOCH));
        }
    }

    private String randomName() {
        String[] firstNames = {"Alice", "Bob", "Carol", "Dave", "Eve", "Frank", "Grace", "Hank", "Ivy", "Jack"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"};
        return firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
    }

    private Instant nextVersion() {
        return currentVersion.updateAndGet(v -> v.plus(1, ChronoUnit.DAYS));
    }

    public void addOrUpdatePerson(Person person) {
        Instant newVersion = nextVersion();
        Person updatedPerson = new Person(person.id(), person.name(), newVersion);
        personStore.put(person.id(), updatedPerson);
        deletedPersons.remove(person.id());

        System.out.printf("[Version %s] Added/Updated person %d (%s)%n", newVersion, updatedPerson.id(), updatedPerson.name());
    }

    public void deletePerson(Long personId) {
        if (personStore.containsKey(personId)) {
            personStore.remove(personId);
            Instant newVersion = nextVersion();
            deletedPersons.put(personId, newVersion);
            System.out.printf("[Version %s] Deleted person %d%n", newVersion, personId);
        }
    }

    @Override
    @GetMapping("/persons")
    public VersionedServiceResponse<Person, String> fetchUpdates(@RequestParam(defaultValue = "1970-01-01T00:00:00Z") String dataVersionStr) {
        Instant dataVersion = Instant.parse(dataVersionStr);

        List<Person> updated = personStore.values().stream()
                .filter(p -> p.dataVersion().isAfter(dataVersion))
                .collect(Collectors.toList());

        List<String> deleted = deletedPersons.entrySet().stream()
                .filter(e -> e.getValue().isAfter(dataVersion))
                .map(e -> e.getKey().toString())
                .collect(Collectors.toList());

        Instant nextVer = currentVersion.get();
        String nextVerStr = DateTimeFormatter.ISO_INSTANT.format(nextVer);

        System.out.printf("Fetch persons since %s: %d updated, %d deleted, nextVersion=%s%n",
                dataVersionStr, updated.size(), deleted.size(), nextVerStr);

        simulateChanges();

        return new VersionedServiceResponse<>(updated, deleted, nextVerStr);
    }

    public void simulateChanges() {
        int changesCount = random.nextInt(3) + 1;

        for (int i = 0; i < changesCount; i++) {
            int action = random.nextInt(3);
            switch (action) {
                case 0 -> addRandomPerson();
                case 1 -> updateRandomPerson();
                case 2 -> deleteRandomPerson();
            }
        }
    }

    private void addRandomPerson() {
        long id = 2000 + random.nextInt(1000);
        String name = randomName();
        addOrUpdatePerson(new Person(id, name, Instant.EPOCH));
    }

    private void updateRandomPerson() {
        if (personStore.isEmpty()) return;
        List<Long> keys = new ArrayList<>(personStore.keySet());
        long id = keys.get(random.nextInt(keys.size()));
        String name = randomName();
        addOrUpdatePerson(new Person(id, name, Instant.EPOCH));
    }

    private void deleteRandomPerson() {
        if (personStore.isEmpty()) return;
        List<Long> keys = new ArrayList<>(personStore.keySet());
        long id = keys.get(random.nextInt(keys.size()));
        deletePerson(id);
    }
}
