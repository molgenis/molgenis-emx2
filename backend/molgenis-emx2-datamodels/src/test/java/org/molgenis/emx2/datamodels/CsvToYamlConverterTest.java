package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.profiles.ResourceListing;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.bundle.ProfileDef;
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
    Emx2Yaml.toBundleDirectory(schema, "Pet store", "Pet store demo", outputDir, List.of());
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertTypeTest() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/typetest.csv");
    Path outputDir = tempDir.resolve("typetest");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "Type test", "Type test data model", outputDir, List.of());
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertPages() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/Pages.csv");
    Path outputDir = tempDir.resolve("pages");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(schema, "Pages", "CMS pages data model", outputDir, List.of());
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
        outputDir,
        List.of());
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
        outputDir,
        List.of());
    assertFalse(schema.getTableNames().isEmpty());
    assertRoundtripEqual(schema, outputDir);
  }

  @Test
  void convertCatalogueOntologies() throws Exception {
    SchemaMetadata schema = readCsvModel("/_models/specific/CatalogueOntologies/molgenis.csv");
    Path outputDir = tempDir.resolve("catalogue-ontologies");
    Files.createDirectories(outputDir);
    Emx2Yaml.toBundleDirectory(
        schema, "Catalogue ontologies", "Catalogue ontology tables", outputDir, List.of());
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
    Emx2Yaml.toBundleDirectory(schema, "shared", null, outputDir, List.of());
  }

  @Disabled("Manual regeneration only — run this locally to update profiles/ reference files")
  @Test
  void manualRegenerateAll_IGNORED() throws IOException, URISyntaxException {
    regenerateSharedTables();
    regeneratePagesTables();
    regenerateCatalogueOntologiesTables();
    regeneratePatientRegistryDemoTables();
  }

  private void regenerateSharedTables() throws IOException, URISyntaxException {
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
    try (InputStream inputStream =
        getClass().getResourceAsStream(SPECIFIC_MODELS_DIR + "/Catalogue aggregates.csv")) {
      if (inputStream != null) {
        CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .forEach(allRows::add);
      }
    }
    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path sharedDir = PROFILES_DIR.resolve("shared");
    List<ProfileDef> profileDefs = loadProfileDefs(sharedDir);
    regenerateTablesOnly(
        schema,
        "MOLGENIS shared catalogue bundle",
        "Shared tables for all catalogue variants",
        sharedDir,
        profileDefs);
  }

  private void regeneratePagesTables() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/Pages.csv");
    regenerateTablesOnly(
        schema, "Pages", "CMS pages data model", PROFILES_DIR.resolve("pages"), List.of());
  }

  private void regenerateCatalogueOntologiesTables() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/CatalogueOntologies/molgenis.csv");
    regenerateTablesOnly(
        schema,
        "CatalogueOntologies",
        "Shared ontology schema for catalogue profiles",
        PROFILES_DIR.resolve("catalogue-ontologies"),
        List.of());
  }

  private void regeneratePatientRegistryDemoTables() throws IOException {
    Path csvPath = REPO_ROOT.resolve("data/patient_registry_demo/molgenis.csv");
    List<Row> allRows = new ArrayList<>();
    try (InputStreamReader reader =
        new InputStreamReader(Files.newInputStream(csvPath), StandardCharsets.UTF_8)) {
      CsvTableReader.read(reader).forEach(allRows::add);
    }
    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path patientDir = PROFILES_DIR.resolve("patient_registry_demo");
    List<ProfileDef> profileDefs = loadProfileDefs(patientDir);
    regenerateTablesOnly(
        schema,
        "Patient Registry Demo",
        "Patient registry demo with disease group extensions",
        patientDir,
        profileDefs);
  }

  private List<ProfileDef> loadProfileDefs(Path profileDir) throws IOException {
    Path molgenisYaml = profileDir.resolve("molgenis.yaml");
    if (!Files.exists(molgenisYaml)) {
      return List.of();
    }
    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(profileDir);
    return result.getBundle().profiles();
  }

  private void regenerateTablesOnly(
      SchemaMetadata schema,
      String bundleName,
      String bundleDescription,
      Path profileDir,
      List<ProfileDef> profileDefs)
      throws IOException {
    Path tablesDir = profileDir.resolve("tables");
    Path tempBundleDir = Files.createTempDirectory("emx2-regen-");
    try {
      Emx2Yaml.toBundleDirectory(schema, bundleName, bundleDescription, tempBundleDir, profileDefs);
      Path tempTablesDir = tempBundleDir.resolve("tables");
      if (Files.exists(tablesDir)) {
        try (java.util.stream.Stream<Path> oldFiles = Files.list(tablesDir)) {
          for (Path oldFile : oldFiles.toList()) {
            Files.delete(oldFile);
          }
        }
      } else {
        Files.createDirectories(tablesDir);
      }
      try (java.util.stream.Stream<Path> newFiles = Files.list(tempTablesDir)) {
        for (Path newFile : newFiles.toList()) {
          Files.copy(newFile, tablesDir.resolve(newFile.getFileName()));
        }
      }
    } finally {
      deleteDirectory(tempBundleDir);
    }
  }

  private void deleteDirectory(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (java.util.stream.Stream<Path> entries = Files.walk(dir)) {
      List<Path> sorted = entries.sorted(java.util.Comparator.reverseOrder()).toList();
      for (Path entry : sorted) {
        Files.delete(entry);
      }
    }
  }

  @Disabled(
      "Manual fix only — run locally to normalise hand-crafted profiles that have no CSV source")
  @Test
  void manualFixHandCraftedProfiles_IGNORED() throws IOException {
    fixDirectoryBundleTablesOnly(
        PROFILES_DIR.resolve("biobank-directory"),
        "biobank-directory",
        "BBMRI-ERIC Biobank Directory schema including biobanks, collections, networks, persons, quality standards and DCAT/FDP metadata");
    fixDirectoryBundleTablesOnly(
        PROFILES_DIR.resolve("dashboard"),
        "dashboard",
        "Dashboard schema with pages, charts, data points and color palettes");
    fixDirectoryBundleTablesOnly(
        PROFILES_DIR.resolve("ui_dashboards"),
        "ui_dashboards",
        "UI dashboards schema with inclusion criteria, data providers, statistics, organisations, users, files and studies");
    fixSingleFileBundle(PROFILES_DIR.resolve("petstore.yaml"));
  }

  private void fixDirectoryBundleTablesOnly(Path profileDir, String bundleName, String bundleDesc)
      throws IOException {
    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(profileDir);
    List<ProfileDef> profileDefs = result.getBundle().profiles();
    regenerateTablesOnly(result.getSchema(), bundleName, bundleDesc, profileDir, profileDefs);
  }

  @SuppressWarnings("unchecked")
  private void fixSingleFileBundle(Path yamlFile) throws IOException {
    ObjectMapper mapper =
        new ObjectMapper(
            YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build());
    Map<String, Object> originalDoc;
    try (InputStream inputStream = Files.newInputStream(yamlFile)) {
      originalDoc = mapper.readValue(inputStream, Map.class);
    }
    Emx2Yaml.BundleResult result = Emx2Yaml.fromBundle(yamlFile);
    List<ProfileDef> profileDefs = result.getBundle().profiles();
    Path tempFile = Files.createTempFile("petstore-regen-", ".yaml");
    try {
      Emx2Yaml.toBundleSingleFile(
          result.getSchema(), result.getName(), result.getDescription(), tempFile, profileDefs);
      Map<String, Object> regeneratedDoc;
      try (InputStream inputStream = Files.newInputStream(tempFile)) {
        regeneratedDoc = mapper.readValue(inputStream, Map.class);
      }
      Map<String, Object> mergedDoc = new LinkedHashMap<>(originalDoc);
      mergedDoc.put("tables", regeneratedDoc.get("tables"));
      Files.writeString(yamlFile, mapper.writeValueAsString(mergedDoc));
    } finally {
      Files.deleteIfExists(tempFile);
    }
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
