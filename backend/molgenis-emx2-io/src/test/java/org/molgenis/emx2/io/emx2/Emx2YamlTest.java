package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class Emx2YamlTest {

  @Test
  void testIndividuals() throws IOException {
    SchemaMetadata schema = loadFromResource("/yaml-model/tables/Individuals.yaml");

    assertEquals(1, schema.getTables().size());
    TableMetadata table = schema.getTableMetadata("Individuals");
    assertNotNull(table);
    assertEquals("Study participants", table.getDescription());

    List<Column> columns = table.getNonInheritedColumns();
    assertEquals(5, columns.size());

    Column individualId = findColumn(columns, "individual id");
    assertEquals(ColumnType.STRING, individualId.getColumnType());
    assertEquals(1, individualId.getKey());

    Column displayName = findColumn(columns, "display name");
    assertEquals(ColumnType.STRING, displayName.getColumnType());

    Column gender = findColumn(columns, "gender");
    assertEquals(ColumnType.ONTOLOGY, gender.getColumnType());
    assertEquals("Genders", gender.getRefTableName());

    Column yearOfBirth = findColumn(columns, "year of birth");
    assertEquals(ColumnType.INT, yearOfBirth.getColumnType());

    Column countryOfBirth = findColumn(columns, "country of birth");
    assertEquals(ColumnType.ONTOLOGY, countryOfBirth.getColumnType());
    assertEquals("Countries", countryOfBirth.getRefTableName());
  }

  @Test
  void testSamples() throws IOException {
    SchemaMetadata schema = loadFromResource("/yaml-model/tables/Samples.yaml");

    assertEquals(1, schema.getTables().size());
    TableMetadata table = schema.getTableMetadata("Samples");
    assertNotNull(table);

    List<Column> columns = table.getNonInheritedColumns();
    assertEquals(5, columns.size());

    Column individual = findColumn(columns, "individual");
    assertEquals(ColumnType.REF, individual.getColumnType());
    assertEquals("Individuals", individual.getRefTableName());
  }

  @Test
  void testExperiments() throws IOException {
    SchemaMetadata schema = loadFromResource("/yaml-model/tables/Experiments.yaml");

    assertEquals(6, schema.getTables().size());

    TableMetadata root = schema.getTableMetadata("Experiments");
    assertNotNull(root);
    List<Column> rootColumns = root.getNonInheritedColumns();
    assertEquals(4, rootColumns.size());

    Column experimentType = findColumn(rootColumns, "experiment type");
    assertEquals(ColumnType.EXTENSION, experimentType.getColumnType());
    assertEquals("true", experimentType.getRequired());
    assertTrue(
        Arrays.asList(experimentType.getProfiles()).contains("-core"),
        "profiles should contain -core");
    assertEquals("WGS", experimentType.getDefaultValue());

    TableMetadata wgs = schema.getTableMetadata("WGS");
    assertNotNull(wgs);
    assertArrayEquals(new String[] {"sampling", "sequencing"}, wgs.getInheritNames());
    assertTrue(Arrays.asList(wgs.getProfiles()).contains("wgs"));

    TableMetadata sampling = schema.getTableMetadata("sampling");
    assertNotNull(sampling);
    assertEquals(TableType.INTERNAL, sampling.getTableType());
    assertEquals(2, sampling.getNonInheritedColumns().size());

    Column sampleType = findColumn(sampling.getNonInheritedColumns(), "sample type");
    assertNotNull(sampleType);
    assertTrue(
        Arrays.asList(sampleType.getSemantics())
            .contains("http://purl.obolibrary.org/obo/OBI_0000747"),
        "semantics should contain OBI_0000747 URI");

    TableMetadata sequencing = schema.getTableMetadata("sequencing");
    assertNotNull(sequencing);
    assertEquals(TableType.INTERNAL, sequencing.getTableType());
    assertEquals(3, sequencing.getNonInheritedColumns().size());

    TableMetadata imaging = schema.getTableMetadata("Imaging");
    assertNotNull(imaging);
    assertArrayEquals(new String[] {"Experiments"}, imaging.getInheritNames());
  }

  @Test
  void testObservations() throws IOException {
    SchemaMetadata schema = loadFromResource("/yaml-model/tables/Observations.yaml");

    assertEquals(4, schema.getTables().size());

    TableMetadata root = schema.getTableMetadata("Observations");
    assertNotNull(root);

    Column observationTypes = findColumn(root.getNonInheritedColumns(), "observation types");
    assertEquals(ColumnType.EXTENSION_ARRAY, observationTypes.getColumnType());

    assertNotNull(schema.getTableMetadata("Dermatology"));
    assertNotNull(schema.getTableMetadata("Neurology"));
    assertNotNull(schema.getTableMetadata("Questionnaire"));

    TableMetadata dermatology = schema.getTableMetadata("Dermatology");
    assertArrayEquals(new String[] {"Observations"}, dermatology.getInheritNames());
  }

  @Test
  void testFromYamlDirectory() throws IOException, URISyntaxException {
    Path dir = Path.of(getClass().getResource("/yaml-model").toURI());
    SchemaMetadata schema = Emx2Yaml.fromYamlDirectory(dir);

    assertNotNull(schema.getTableMetadata("Individuals"));
    assertNotNull(schema.getTableMetadata("Samples"));
    assertNotNull(schema.getTableMetadata("Experiments"));
    assertNotNull(schema.getTableMetadata("Observations"));

    assertNotNull(schema.getTableMetadata("WGS"));
    assertNotNull(schema.getTableMetadata("RNA_seq"));
    assertNotNull(schema.getTableMetadata("Imaging"));
    assertNotNull(schema.getTableMetadata("sampling"));
    assertNotNull(schema.getTableMetadata("sequencing"));

    assertNotNull(schema.getTableMetadata("Dermatology"));
    assertNotNull(schema.getTableMetadata("Neurology"));
    assertNotNull(schema.getTableMetadata("Questionnaire"));

    assertEquals(12, schema.getTables().size());
  }

  @Test
  void testRoundtrip() throws Exception {
    Path dir = Path.of(getClass().getResource("/yaml-model").toURI());
    SchemaMetadata original = Emx2Yaml.fromYamlDirectory(dir);

    for (String rootName : List.of("Individuals", "Samples", "Experiments", "Observations")) {
      String yaml = Emx2Yaml.toYamlFile(original, rootName);
      SchemaMetadata reimported =
          Emx2Yaml.fromYamlFile(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

      for (TableMetadata origTable : original.getTables()) {
        TableMetadata reimportedTable = reimported.getTableMetadata(origTable.getTableName());
        if (reimportedTable == null) {
          continue;
        }
        assertEquals(
            origTable.getTableType(),
            reimportedTable.getTableType(),
            "tableType mismatch for " + origTable.getTableName());
        assertArrayEquals(
            origTable.getInheritNames(),
            reimportedTable.getInheritNames(),
            "inheritNames mismatch for " + origTable.getTableName());
        if (origTable.getProfiles() != null) {
          assertArrayEquals(
              origTable.getProfiles(),
              reimportedTable.getProfiles(),
              "profiles mismatch for " + origTable.getTableName());
        }
        for (Column origCol : origTable.getNonInheritedColumns()) {
          Column reimportedCol = reimportedTable.getColumn(origCol.getName());
          assertNotNull(
              reimportedCol,
              "Missing column '" + origCol.getName() + "' in " + origTable.getTableName());
          assertEquals(
              origCol.getColumnType(),
              reimportedCol.getColumnType(),
              "type mismatch for " + origTable.getTableName() + "." + origCol.getName());
          assertEquals(
              origCol.getKey(),
              reimportedCol.getKey(),
              "key mismatch for " + origTable.getTableName() + "." + origCol.getName());
          assertEquals(
              origCol.getRefTableName(),
              reimportedCol.getRefTableName(),
              "refTable mismatch for " + origTable.getTableName() + "." + origCol.getName());
          assertEquals(
              origCol.getRequired(),
              reimportedCol.getRequired(),
              "required mismatch for " + origTable.getTableName() + "." + origCol.getName());
          assertEquals(
              origCol.getDefaultValue(),
              reimportedCol.getDefaultValue(),
              "defaultValue mismatch for " + origTable.getTableName() + "." + origCol.getName());
          if (origCol.getSemantics() != null) {
            assertArrayEquals(
                origCol.getSemantics(),
                reimportedCol.getSemantics(),
                "semantics mismatch for " + origTable.getTableName() + "." + origCol.getName());
          }
          if (origCol.getProfiles() != null) {
            assertArrayEquals(
                origCol.getProfiles(),
                reimportedCol.getProfiles(),
                "profiles mismatch for " + origTable.getTableName() + "." + origCol.getName());
          }
          assertEquals(
              origCol.getDescriptions().get("en"),
              reimportedCol.getDescriptions().get("en"),
              "description mismatch for " + origTable.getTableName() + "." + origCol.getName());
        }
      }
    }
  }

  @Test
  void testTemplateRd3() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/rd3.yaml").toURI());
    Emx2Yaml.TemplateResult result = Emx2Yaml.fromYamlTemplate(template);

    assertEquals("RD3", result.getName());
    assertEquals("Rare Disease Data for Discovery", result.getDescription());

    SchemaMetadata schema = result.getSchema();
    assertNotNull(schema.getTableMetadata("Individuals"));
    assertNotNull(schema.getTableMetadata("Samples"));
    assertNotNull(schema.getTableMetadata("Experiments"));
    assertNotNull(schema.getTableMetadata("Observations"));
    assertEquals(12, schema.getTables().size());

    assertEquals(List.of("wgs", "rna"), result.getProfiles());

    assertEquals("Welcome to RD3", result.getSettings().get("landingPage"));

    assertEquals("Viewer", result.getPermissions().get("anonymous"));
    assertEquals("Editor", result.getPermissions().get("user"));
  }

  @Test
  void testTemplateFull() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/full.yaml").toURI());
    Emx2Yaml.TemplateResult result = Emx2Yaml.fromYamlTemplate(template);

    assertEquals("Full", result.getName());

    assertTrue(result.getProfiles().isEmpty());

    assertEquals(12, result.getSchema().getTables().size());
  }

  @Test
  void testTemplateRoundtrip() throws Exception {
    Path template = Path.of(getClass().getResource("/yaml-model/templates/rd3.yaml").toURI());
    Emx2Yaml.TemplateResult original = Emx2Yaml.fromYamlTemplate(template);

    String yaml = Emx2Yaml.toYamlTemplate(original);
    assertTrue(yaml.contains("name: "));
    assertTrue(yaml.contains("RD3"));
    assertTrue(yaml.contains("profiles:"));
    assertTrue(yaml.contains("wgs"));
  }

  private SchemaMetadata loadFromResource(String resourcePath) throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
      assertNotNull(inputStream, "Resource not found: " + resourcePath);
      return Emx2Yaml.fromYamlFile(inputStream);
    }
  }

  private Column findColumn(List<Column> columns, String name) {
    return columns.stream()
        .filter(c -> c.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Column not found: " + name));
  }
}
