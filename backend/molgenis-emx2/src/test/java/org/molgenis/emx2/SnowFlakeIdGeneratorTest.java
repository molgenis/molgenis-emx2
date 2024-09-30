package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.generator.SnowFlakeIdGenerator;

public class SnowFlakeIdGeneratorTest {

  @Test
  public void testIdGenerationIsUnique() {
    SnowFlakeIdGenerator generator = SnowFlakeIdGenerator.getInstance();
    Set<String> generatedIds = new HashSet<>();

    int totalIds = 100000;
    for (int i = 0; i < totalIds; i++) {
      String id = generator.generateId("tableId");
      assertFalse(generatedIds.contains(id), "Duplicate ID found: " + id);
      generatedIds.add(id);
    }
  }

  @Test
  public void testIdGenerationIsSorted() {
    SnowFlakeIdGenerator generator = SnowFlakeIdGenerator.getInstance();
    String[] generatedIds = new String[100000];

    for (int i = 0; i < generatedIds.length; i++) {
      generatedIds[i] = generator.generateId("tableId");
    }

    // Check if IDs are sorted
    for (int i = 1; i < generatedIds.length; i++) {
      assertTrue(
          generatedIds[i - 1].compareTo(generatedIds[i]) <= 0,
          "IDs are not sorted at index " + (i - 1) + " and " + i);
    }
  }

  @Test
  public void testUniqueIdsOnFourThreads() throws InterruptedException {
    SnowFlakeIdGenerator generator = SnowFlakeIdGenerator.getInstance();
    Set<String> uniqueIds = new HashSet<>();

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    for (int i = 0; i < 100000; i++) {
      executorService.submit(
          () -> {
            String id = generator.generateId("tableId");
            synchronized (uniqueIds) {
              uniqueIds.add(id);
            }
          });
    }

    // Shut down the executor and wait for tasks to finish
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    // Verify that the number of unique IDs matches the total number of generated IDs
    assertEquals(100000, uniqueIds.size(), "Not all generated IDs are unique.");
  }
}
