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
    Emx2Yaml.TemplateResult result = Emx2Yaml.fromYamlTemplate(template);

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
    Emx2Yaml.TemplateResult result = Emx2Yaml.fromYamlTemplate(template);

    assertEquals("Full", result.getName());

    assertTrue(result.getActiveProfiles().isEmpty());

    assertEquals(12, result.getSchema().getTables().size());
  }

  @Test
  void testLegacyProfilesKeyThrowsError() throws Exception {
    Path legacyTemplate = tempDir.resolve("legacy.yaml");
    Files.writeString(legacyTemplate, "name: Legacy\nprofiles:\n  - wgs\n");
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromYamlTemplate(legacyTemplate));
    assertTrue(
        exception.getMessage().contains("'profiles' is no longer supported"),
        "Error message should name the deprecated key");
    assertTrue(
        exception.getMessage().contains("activeSubsets"),
        "Error message should name the replacement key");
  }

  @Test
  void testVariantExtendsOmittedWhenMatchesDefaultParent() throws Exception {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata root = schema.create(new TableMetadata("MyTable"));
    root.add(new Column("id").setKey(1));
    TableMetadata variant = schema.create(new TableMetadata("MyVariant"));
    variant.setInheritNames("MyTable");
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
        assertThrows(MolgenisException.class, () -> Emx2Yaml.fromYamlTemplate(templateFile));
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
    Emx2Yaml.TemplateResult original = Emx2Yaml.fromYamlTemplate(template);

    String yaml = Emx2Yaml.toYamlTemplate(original);
    assertTrue(yaml.contains("name: "));
    assertTrue(yaml.contains("RD3"));
    assertTrue(yaml.contains("activeSubsets:"));
    assertTrue(yaml.contains("wgs"));
  }
}
