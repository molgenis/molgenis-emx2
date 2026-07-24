package org.molgenis.emx2.datamodels.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;

class SchemaFromProfileYamlTest {

  private static final String PETSTORE_PROFILE = "_profiles/PetStore.yaml";

  @Test
  void sliceParity() {
    SchemaMetadata csvSliced = new SchemaFromProfile(PETSTORE_PROFILE).create();
    SchemaMetadata yamlSliced = new SchemaFromProfileYaml(PETSTORE_PROFILE).create();

    assertEquals(normalize(csvSliced), normalize(yamlSliced));
  }

  /** Order-insensitive deep view of a schema: canonical string per metadata row, sorted. */
  private static List<String> normalize(SchemaMetadata schema) {
    List<String> lines = new ArrayList<>();
    for (Row row : Emx2.toRowList(schema)) {
      Map<String, Object> sorted = new TreeMap<>(row.getValueMap());
      StringBuilder line = new StringBuilder();
      for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        Object value = entry.getValue();
        String text = render(value);
        if (!text.isEmpty()) {
          line.append(entry.getKey()).append('=').append(text).append('|');
        }
      }
      lines.add(line.toString());
    }
    lines.sort(String::compareTo);
    return lines;
  }

  private static String render(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Object[] array) {
      return array.length == 0 ? "" : Arrays.toString(array);
    }
    return String.valueOf(value);
  }
}
