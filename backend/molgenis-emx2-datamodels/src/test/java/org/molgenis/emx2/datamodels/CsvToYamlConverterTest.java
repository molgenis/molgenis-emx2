package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.profiles.ResourceListing;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.readers.CsvTableReader;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CsvToYamlConverterTest {

  private static final String SHARED_MODELS_DIR = "/_models/shared";
  private static final String SPECIFIC_MODELS_DIR = "/_models/specific";

  private static final Path TEMPLATES_DIR =
      Path.of(System.getProperty("user.dir")).resolve("../../data/templates");

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
  @Order(1)
  void convertPetStore() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/petstore.csv");
    Path outputDir = TEMPLATES_DIR.resolve("petstore");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(2)
  void convertTypeTest() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/typetest.csv");
    Path outputDir = TEMPLATES_DIR.resolve("typetest");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(3)
  void convertPages() throws IOException {
    SchemaMetadata schema = readCsvModel(SPECIFIC_MODELS_DIR + "/Pages.csv");
    Path outputDir = TEMPLATES_DIR.resolve("pages");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(4)
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
    Path outputDir = TEMPLATES_DIR.resolve("shared");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(5)
  void convertPatientRegistryDemo() throws Exception {
    Path csvPath = TEMPLATES_DIR.resolve("../patient_registry_demo/molgenis.csv");
    List<Row> allRows = new ArrayList<>();
    try (InputStreamReader reader =
        new InputStreamReader(Files.newInputStream(csvPath), StandardCharsets.UTF_8)) {
      CsvTableReader.read(reader).forEach(allRows::add);
    }
    SchemaMetadata schema = Emx2.fromRowList(allRows);
    Path outputDir = TEMPLATES_DIR.resolve("patient_registry_demo");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(6)
  void convertCatalogueOntologies() throws Exception {
    SchemaMetadata schema = readCsvModel("/_models/specific/CatalogueOntologies/molgenis.csv");
    Path outputDir = TEMPLATES_DIR.resolve("shared/ontologies");
    Files.createDirectories(outputDir);
    Emx2Yaml.toYamlDirectory(schema, outputDir);
    assertFalse(schema.getTableNames().isEmpty());
  }

  @Test
  @Order(7)
  void writeTemplateStubs() throws IOException {
    writeTemplate(
        "petstore", "Pet Store", "Example pet store data model", List.of("tables/*"), List.of());
    writeTemplate("typetest", "Type Test", "Type test data model", List.of("tables/*"), List.of());
    writeTemplate("pages", "Pages", "CMS pages data model", List.of("tables/*"), List.of());

    writeTemplate(
        "shared",
        "datacatalogue",
        "European Networks Health Data and Cohort Catalogue",
        "European Networks Health Data and Cohort Catalogue",
        List.of("tables/*"),
        List.of("DataCatalogueFlat"));
    writeTemplate(
        "shared",
        "cohortstaging",
        "CohortsStaging",
        "CohortsStaging",
        List.of("tables/*"),
        List.of("CohortsStaging"));
    writeTemplate(
        "shared",
        "integratecohorts",
        "INTEGRATECohorts",
        "INTEGRATECohorts",
        List.of("tables/*"),
        List.of("INTEGRATE"));
    writeTemplate(
        "shared",
        "networksstaging",
        "Networks data model",
        "Networks data model",
        List.of("tables/*"),
        List.of("NetworksStaging"));
    writeTemplate(
        "shared",
        "rwestaging",
        "Real world evidence data model",
        "Real world evidence data model",
        List.of("tables/*"),
        List.of("RWEStaging"));
    writeTemplate(
        "shared",
        "sharedstaging",
        "SharedStaging",
        "SharedStaging",
        List.of("tables/*"),
        List.of("SharedStaging"));
    writeTemplate(
        "shared",
        "studiesstaging",
        "Studies data model",
        "Studies data model",
        List.of("tables/*"),
        List.of("StudiesStaging"));
    writeTemplate(
        "shared",
        "umcgcohortsstaging",
        "UMCGCohortsStaging",
        "UMCGCohortsStaging",
        List.of("tables/*"),
        List.of("UMCGCohortsStaging"));
    writeTemplate(
        "shared",
        "umcucohorts",
        "Staging area for filling out UMCG cohort metadata",
        "Staging area for filling out UMCG cohort metadata",
        List.of("tables/*"),
        List.of("UMCUCohorts"));
    writeTemplate(
        "shared",
        "datacatalogueaggregates",
        "Aggregates for data collections in data catalogue.",
        "Aggregates for data collections in data catalogue.",
        List.of("tables/*"),
        List.of("DataCatalogueAggregates"));
    writeTemplate(
        "shared",
        "patientregistry",
        "Patient registry",
        "Patient registry",
        List.of("tables/*"),
        List.of("Patient registry", "DataCatalogueFlat"));
    writeTemplate(
        "shared",
        "fairgenomes",
        "FAIR Genomes metadata schema",
        "FAIR Genomes metadata schema",
        List.of("tables/*"),
        List.of("FAIR Genomes"));
    writeTemplate(
        "shared",
        "imagetest",
        "Image test",
        "Image test",
        List.of("tables/*"),
        List.of("ImageTest"));
    writeTemplate(
        "patient_registry_demo",
        "Patient Registry Demo",
        "Patient registry demo with disease group extensions",
        List.of("tables/*"),
        List.of());
  }

  private void writeTemplate(
      String dirName, String name, String description, List<String> imports, List<String> profiles)
      throws IOException {
    writeTemplate(dirName, dirName, name, description, imports, profiles);
  }

  private void writeTemplate(
      String dirName,
      String fileName,
      String name,
      String description,
      List<String> imports,
      List<String> profiles)
      throws IOException {
    Path dir = TEMPLATES_DIR.resolve(dirName);
    Files.createDirectories(dir);
    StringBuilder sb = new StringBuilder();
    sb.append("name: ").append(name).append("\n");
    sb.append("description: \"").append(description).append("\"\n");
    sb.append("\nimports:\n");
    for (String imp : imports) {
      sb.append("  - ").append(imp).append("\n");
    }
    if (!profiles.isEmpty()) {
      sb.append("\nprofiles:\n");
      for (String profile : profiles) {
        sb.append("  - ").append(profile).append("\n");
      }
    }
    Files.writeString(dir.resolve(fileName + ".yaml"), sb.toString(), StandardCharsets.UTF_8);
  }

  @Test
  @Order(8)
  void verifyPetStoreRoundtrip() throws IOException {
    SchemaMetadata fromCsv = readCsvModel(SPECIFIC_MODELS_DIR + "/petstore.csv");
    SchemaMetadata fromYaml = Emx2Yaml.fromYamlDirectory(TEMPLATES_DIR.resolve("petstore"));
    assertEquals(fromCsv.getTableNames().size(), fromYaml.getTableNames().size());
  }

  @Test
  @Order(9)
  void verifySharedRoundtrip() throws IOException, URISyntaxException {
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
      if (inputStream != null) {
        CsvTableReader.read(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .forEach(allRows::add);
      }
    }
    SchemaMetadata fromCsv = Emx2.fromRowList(allRows);
    SchemaMetadata fromYaml = Emx2Yaml.fromYamlDirectory(TEMPLATES_DIR.resolve("shared"));
    assertEquals(fromCsv.getTableNames().size(), fromYaml.getTableNames().size());
  }
}
