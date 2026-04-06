package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.profiles.ResourceListing;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.readers.CsvTableReader;

class CsvToYamlConverterTest {

  private static final String SHARED_MODELS_DIR = "/_models/shared";
  private static final String SPECIFIC_MODELS_DIR = "/_models/specific";

  private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir")).resolve("../../");
  private static final Path PROFILES_DIR = REPO_ROOT.resolve("profiles");

  @TempDir Path tempDir;

  private SchemaMetadata readCsvModel(String... csvResourcePaths) throws IOException {
    List<Row> allRows = new ArrayList<>();
    for (String path : csvResourcePaths) {
      try (InputStream inputStream = getClass().getResourceAsStream(path)) {
        if (inputStream == null) {
          throw new IOException("Resource not found: " + path);
        }
        CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .forEach(allRows::add);
      }
    }
    return Emx2.fromRowList(allRows);
  }

  @Test
  void convertPetStore() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/petstore.csv");
    Path outputDir = tempDir.resolve("petstore");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "Pet store", "Pet store demo", outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertTypeTest() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/typetest.csv");
    Path outputDir = tempDir.resolve("typetest");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "Type test", "Type test data model", outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertPages() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/Pages.csv");
    Path outputDir = tempDir.resolve("pages");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "Pages", "CMS pages data model", outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertSharedModels() throws IOException, URISyntaxException {
    List<Row> allRows = new ArrayList<>();
    String[] sharedFiles = new ResourceListing().retrieve(SHARED_MODELS_DIR);
    for (String file : sharedFiles) {
      if (file.endsWith(".csv")) {
        try (InputStream inputStream =
            getClass().getResourceAsStream(SHARED_MODELS_DIR + "/" + file)) {
          if (inputStream == null) {
            throw new IOException("Resource not found: " + SHARED_MODELS_DIR + "/" + file);
          }
          CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
              .forEach(allRows::add);
        }
      }
    }
    try (InputStream inputStream =
        getClass().getResourceAsStream(SPECIFIC_MODELS_DIR + "/Catalogue aggregates.csv")) {
      if (inputStream == null) {
        throw new IOException(
            "Resource not found: " + SPECIFIC_MODELS_DIR + "/Catalogue aggregates.csv");
      }
      CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
          .forEach(allRows::add);
    }

    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path outputDir = tempDir.resolve("shared");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(
        schema,
        "MOLGENIS shared catalogue bundle",
        "Shared tables for all catalogue variants",
        outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertPatientRegistryDemo() throws Exception {
    Path csvPath = REPO_ROOT.resolve("data/patient_registry_demo/molgenis.csv");
    List<Row> allRows = new ArrayList<>();
    try (InputStreamReader reader =
        new InputStreamReader(Files.newInputStream(csvPath), StandardCharsets.UTF_8)) {
      CsvTableReader.read(reader).forEach(allRows::add);
    }
    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path outputDir = tempDir.resolve("patient_registry_demo");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(
        schema,
        "Patient Registry Demo",
        "Patient registry demo with disease group extensions",
        outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertCatalogueOntologies() throws Exception {
    SchemaMetadata schema = readCsvModel("/_models/specific/CatalogueOntologies/molgenis.csv");
    Path outputDir = tempDir.resolve("catalogue-ontologies");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(
        schema, "Catalogue ontologies", "Catalogue ontology tables", outputDir);
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Disabled("Manual regeneration only — run this locally to update profiles/ reference files")
  @Test
  void manualRegenerate_IGNORED() throws IOException, URISyntaxException {
    List<Row> allRows = new ArrayList<>();
    String[] sharedFiles = new ResourceListing().retrieve(SHARED_MODELS_DIR);
    for (String file : sharedFiles) {
      if (file.endsWith(".csv")) {
        try (InputStream inputStream =
            getClass().getResourceAsStream(SHARED_MODELS_DIR + "/" + file)) {
          if (inputStream != null) {
            CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .forEach(allRows::add);
          }
        }
      }
    }
    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path outputDir = PROFILES_DIR.resolve("shared");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "shared", null, outputDir);
  }

  private void assertRoundtripEqual(SchemaMetadata original, Path bundleDir) throws IOException {
    Emx2Yaml.BundleResult roundtrip = Emx2Yaml.fromBundle(bundleDir);
    SchemaMetadata roundtripSchema = roundtrip.getSchema();

    assertEquals(
        original.getTableNames().size(),
        roundtripSchema.getTableNames().size(),
        "Table count mismatch after roundtrip");

    for (String tableName : original.getTableNames()) {
      TableMetadata originalTable = original.getTableMetadata(tableName);
      TableMetadata roundtripTable = roundtripSchema.getTableMetadata(tableName);
      assertNotNull(roundtripTable, "Table '" + tableName + "' missing after roundtrip");

      List<org.molgenis.emx2.Column> originalCols = originalTable.getNonInheritedColumns();
      for (org.molgenis.emx2.Column originalCol : originalCols) {
        if (originalCol.isSystemColumn()) {
          continue;
        }
        org.molgenis.emx2.Column roundtripCol = roundtripTable.getColumn(originalCol.getName());
        assertNotNull(
            roundtripCol,
            "Column '"
                + originalCol.getName()
                + "' missing in table '"
                + tableName
                + "' after roundtrip");
        assertEquals(
            originalCol.getColumnType(),
            roundtripCol.getColumnType(),
            "Type mismatch for " + tableName + "." + originalCol.getName());
      }
    }
  }
}
