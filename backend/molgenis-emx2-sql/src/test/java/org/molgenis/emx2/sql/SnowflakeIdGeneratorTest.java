package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

public class SnowflakeIdGeneratorTest {

  private static SnowflakeIdGenerator generator;

  @BeforeAll
  public static void before() {
    if (!SnowflakeIdGenerator.hasInstance()) {
      generator = SnowflakeIdGenerator.init("123");
    } else {
      generator = SnowflakeIdGenerator.getInstance();
    }
  }

  @Test
  public void testIdGenerationIsUnique() {
    Set<String> generatedIds = new HashSet<>();

    int totalIds = 100000;
    for (int i = 0; i < totalIds; i++) {
      String id = generator.generateId();
      assertFalse(generatedIds.contains(id), "Duplicate ID found: " + id);
      generatedIds.add(id);
    }
  }

  @Test
  public void testIdGenerationIsSorted() {
    String[] generatedIds = new String[100000];

    for (int i = 0; i < generatedIds.length; i++) {
      generatedIds[i] = generator.generateId();
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
    Set<String> uniqueIds = new HashSet<>();

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    for (int i = 0; i < 100000; i++) {
      executorService.submit(
          () -> {
            String id = generator.generateId();
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

  @Test
  public void testExtractionInstanceIdFromSnowflake() {
    String snowflakeId = generator.generateId();

    String instanceIdFromSnowflake = SnowflakeIdGenerator.extractInstanceId(snowflakeId);
    assertEquals(generator.getInstanceId(), instanceIdFromSnowflake);
  }

  @Test
  public void testExtractTimestampFromSnowflake() {
    long currentTime = Instant.now().toEpochMilli();
    String snowflakeId = generator.generateId();

    long snowflakeTimestamp = SnowflakeIdGenerator.extractTimestamp(snowflakeId);
    // Allow a mismatch of 1ms
    assertTrue(Math.abs(currentTime - snowflakeTimestamp) <= 1);
  }
}
