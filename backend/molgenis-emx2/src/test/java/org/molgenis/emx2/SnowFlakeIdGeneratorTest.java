package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.generator.SnowFlakeIdGenerator;

public class SnowFlakeIdGeneratorTest {

  @Test
  public void testIdGenerationIsUnique() {
    SnowFlakeIdGenerator generator = new SnowFlakeIdGenerator("testSchemaId");
    Set<String> generatedIds = new HashSet<>();

    int totalIds = 10000;
    for (int i = 0; i < totalIds; i++) {
      String id = generator.generateId();
      assertFalse(generatedIds.contains(id), "Duplicate ID found: " + id);
      generatedIds.add(id);
    }
  }

  @Test
  public void testIdGenerationIsSorted() {
    SnowFlakeIdGenerator generator = new SnowFlakeIdGenerator("testSchemaId");
    String[] generatedIds = new String[10000];

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
}
