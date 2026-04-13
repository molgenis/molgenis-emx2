package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class Emx2YamlTest {

  @TempDir Path tempDir;

  @Test
  void testTemplateRd3() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/rd3.yaml").toURI());
    Emx2Yaml.BundleParseResult result = Emx2Yaml.fromYamlBundle(template);

    assertEquals("RD3", result.getName());
    assertEquals("Rare Disease Data for Discovery", result.getDescription());

    org.molgenis.emx2.SchemaMetadata schema = result.getSchema();
    assertNotNull(schema.getTableMetadata("Individuals"));
    assertNotNull(schema.getTableMetadata("Samples"));
    assertNotNull(schema.getTableMetadata("Experiments"));
    assertNotNull(schema.getTableMetadata("Observations"));
    assertEquals(12, schema.getTables().size());

    assertEquals(List.of("wgs", "rna"), result.getActiveProfiles());

    assertEquals("Welcome to RD3", result.getSettings().get("landingPage"));

    assertEquals("Viewer", result.getPermissions().get("anonymous"));
    assertEquals("Editor", result.getPermissions().get("user"));
  }

  @Test
  void testTemplateFull() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/full.yaml").toURI());
    Emx2Yaml.BundleParseResult result = Emx2Yaml.fromYamlBundle(template);

    assertEquals("Full", result.getName());

    assertTrue(result.getActiveProfiles().isEmpty());

    assertEquals(12, result.getSchema().getTables().size());
  }

  @Test
  void testVariantExtendsOmittedWhenMatchesDefaultParent() throws Exception {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata root = schema.create(new TableMetadata("MyTable"));
    root.add(new Column("id").setKey(1));
    TableMetadata variant = schema.create(new TableMetadata("MyVariant"));
    variant.setExtendNames("MyTable");
    variant.add(new Column("extra").setType(ColumnType.STRING));

    Path outputFile = tempDir.resolve("test-variant.yaml");
    Emx2Yaml.toBundleSingleFile(schema, "TestBundle", null, outputFile, List.of());
    String yaml = Files.readString(outputFile);

    assertFalse(
        yaml.contains("extends:"), "extends: should be omitted when it matches the default parent");
  }

  @Test
  void testTemplateImportPathTraversalThrows() throws Exception {
    Path templateFile = tempDir.resolve("template.yaml");
    Files.writeString(templateFile, "name: Test\nimports:\n  - ../../../etc/passwd\n");
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromYamlBundle(templateFile));
    assertTrue(
        exception.getMessage().toLowerCase().contains("escapes"),
        "Error message should indicate path escapes base directory, but was: "
            + exception.getMessage());
  }

  @Test
  void testNumericLabelDoesNotThrowClassCastException() throws Exception {
    Path yamlFile = tempDir.resolve("numeric-label.yaml");
    Files.writeString(
        yamlFile,
        "name: TestBundle\ntables:\n  Persons:\n    columns:\n      - name: age\n        label: 2024\n");
    assertDoesNotThrow(
        () -> {
          Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(yamlFile);
          assertNotNull(result);
        });
  }

  @Test
  void testTemplateRoundtrip() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/rd3.yaml").toURI());
    Emx2Yaml.BundleParseResult original = Emx2Yaml.fromYamlBundle(template);

    String yaml = Emx2Yaml.toYamlBundle(original);
    assertTrue(yaml.contains("name: "));
    assertTrue(yaml.contains("RD3"));
    assertTrue(yaml.contains("activeProfiles:"));
    assertTrue(yaml.contains("wgs"));
  }

  @Test
  void testEnumColumnYamlRoundtrip() throws Exception {
    Path yamlFile = tempDir.resolve("enum-test.yaml");
    Files.writeString(
        yamlFile,
        "name: EnumTest\ntables:\n  Patients:\n    columns:\n"
            + "      - name: id\n        key: 1\n"
            + "      - name: smoking_status\n        type: enum\n"
            + "        values: [never, former, current]\n");

    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(yamlFile);
    Column col = result.getSchema().getTableMetadata("Patients").getColumn("smoking_status");
    assertNotNull(col);
    assertEquals(ColumnType.ENUM, col.getColumnType());
    assertArrayEquals(new String[] {"never", "former", "current"}, col.getValues());

    Path outputFile = tempDir.resolve("enum-output.yaml");
    Emx2Yaml.toBundleSingleFile(result.getSchema(), "EnumTest", null, outputFile, List.of());
    String outputYaml = Files.readString(outputFile);

    assertTrue(outputYaml.contains("type: enum"), "YAML should contain 'type: enum'");
    assertTrue(outputYaml.contains("values:"), "YAML should contain 'values:'");
    assertTrue(outputYaml.contains("never"), "YAML should contain 'never'");
    assertTrue(outputYaml.contains("former"), "YAML should contain 'former'");
    assertTrue(outputYaml.contains("current"), "YAML should contain 'current'");
  }

  @Test
  void testEnumArrayColumnYamlRoundtrip() throws Exception {
    Path yamlFile = tempDir.resolve("enum-array-test.yaml");
    Files.writeString(
        yamlFile,
        "name: EnumArrayTest\ntables:\n  Survey:\n    columns:\n"
            + "      - name: id\n        key: 1\n"
            + "      - name: diet\n        type: enum_array\n"
            + "        values: [vegan, vegetarian, omnivore]\n");

    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(yamlFile);
    Column col = result.getSchema().getTableMetadata("Survey").getColumn("diet");
    assertNotNull(col);
    assertEquals(ColumnType.ENUM_ARRAY, col.getColumnType());
    assertArrayEquals(new String[] {"vegan", "vegetarian", "omnivore"}, col.getValues());
  }
}
