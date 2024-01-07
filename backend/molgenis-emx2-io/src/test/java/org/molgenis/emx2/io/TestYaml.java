package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.yaml.Emx2YamlLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

  @Test
  void testYaml() throws IOException {
    String example =
        Files.readString(
            Paths.get("../../molgenis-emx2.schema.example.yaml"), StandardCharsets.UTF_8);

    Emx2YamlLoader loader = new Emx2YamlLoader();
    SchemaMetadata schema = loader.read(example);
    verifySchemaState(schema);

    // now a round trip
    String yaml = loader.write(schema);
    logger.info("\n" + yaml);
  }

  private static void verifySchemaState(SchemaMetadata schema) {
    // shedload of tests
    assertEquals("My schema", schema.getName());
    assertEquals("blue", schema.getSetting("theme"));
    assertEquals("some example schema", schema.getDescription());

    TableMetadata person = schema.getTableMetadata("Person");
    assertEquals("my first table", person.getDescription());
    assertNotNull(schema.getTableMetadata("Employee"));
    assertEquals("Employee", schema.getTableMetadata("Manager").getInheritName());

    assertEquals(ColumnType.AUTO_ID, person.getColumn("id").getColumnType());
    assertEquals(1, person.getColumn("id").getKey());
    assertTrue(person.getColumn("birthday").isRequired());

    assertEquals(ColumnType.ONTOLOGY, person.getColumn("gender").getColumnType());
    assertEquals("GenderOntology", person.getColumn("gender").getRefTableName());

    assertEquals("id", person.getColumn("birthday").getVisible());
    assertEquals("birthday < Date.now()", person.getColumn("birthday").getValidation());
    assertTrue(List.of(person.getColumn("birthday").getProfiles()).contains("test"));

    assertEquals("Person", person.getColumn("parent").getRefTableName());
    assertEquals("${id}", person.getColumn("parent").getRefLabel());

    assertNull(person.getColumn("teamlead"));
    assertNotNull(schema.getTableMetadata("Employee").getColumn("teamlead"));

    assertEquals("supervisor", schema.getTableMetadata("Manager").getColumn("team").getRefBack());
    assertEquals(
        ColumnType.REFBACK, schema.getTableMetadata("Manager").getColumn("team").getColumnType());
    assertEquals(
        "Employee", schema.getTableMetadata("Manager").getColumn("team").getRefTableName());
  }
}
