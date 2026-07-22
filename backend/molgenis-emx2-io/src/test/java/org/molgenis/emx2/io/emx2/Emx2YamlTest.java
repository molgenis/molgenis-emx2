package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class Emx2YamlTest {

  private Path minimalBundleDir() throws Exception {
    return Path.of(getClass().getResource("/yamlbundle/minimal/molgenis.yaml").toURI()).getParent();
  }

  @Test
  void minimalBundleRoundTripsByteIdentical() throws Exception {
    Emx2YamlBundle parsed = Emx2Yaml.fromBundle(minimalBundleDir());

    SchemaMetadata schema = parsed.schema();
    assertEquals(2, schema.getTables().size());
    assertEquals("1.0.0", parsed.version());
    assertEquals("main", schema.getSettings().get("menu"));
    TableMetadata pet = schema.getTableMetadata("Pet");
    assertNotNull(pet);
    assertEquals("Pets", pet.getLabels().get("en"));
    assertEquals("card", pet.getSettings().get("row_style"));
    assertEquals(1, pet.getColumn("name").getKey());
    assertEquals("true", pet.getColumn("active").getRequired());

    Map<String, String> firstExport = Emx2Yaml.toBundleFiles(parsed);
    Map<String, String> secondExport =
        Emx2Yaml.toBundleFiles(Emx2Yaml.fromBundleFiles(firstExport));

    assertEquals(firstExport, secondExport);
  }

  @Test
  void unknownKeyError() {
    Map<String, String> files =
        Map.of(
            "molgenis.yaml", "tables:\n- file: tables/Bad.yaml\n",
            "tables/Bad.yaml",
                "name: Bad\ncolumns:\n- name: id\n  key: 1\n- name: broken\n  refTabel: Something\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("refTabel"), message);
    assertTrue(message.contains("tables/Bad.yaml"), message);
    assertTrue(message.contains("columns[1]"), message);
    assertTrue(message.contains("line"), message);
    assertTrue(message.contains("column"), message);
  }

  @Test
  void formatVersionSkew() {
    Map<String, String> files =
        Map.of("molgenis.yaml", "formatVersion: 999\ntables:\n- file: tables/Missing.yaml\n");

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromBundleFiles(files));

    String message = exception.getMessage();
    assertTrue(message.contains("999"), message);
    assertTrue(message.toLowerCase().contains("formatversion"), message);
    assertTrue(message.contains("newer"), message);
  }
}
