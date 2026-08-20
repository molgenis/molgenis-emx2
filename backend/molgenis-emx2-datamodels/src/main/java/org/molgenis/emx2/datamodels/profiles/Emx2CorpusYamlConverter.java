package org.molgenis.emx2.datamodels.profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.Emx2YamlBundle;
import org.molgenis.emx2.io.readers.CsvTableReader;

/**
 * Reconverts the CSV model corpus ({@code data/_models/shared} + {@code data/_models/specific}) to
 * the YAML bundle format. Each source CSV is a "model": it is converted only when it maps cleanly,
 * gated by structural checks (single table definition, in-file inheritance, no cross-file table
 * split) and a per-model round-trip fidelity self-check (CSV to SchemaMetadata to YAML to
 * SchemaMetadata, rows equal order-insensitive). Models failing any check are skipped and recorded
 * in {@code SKIPPED.md} next to the output; nothing is converted lossily.
 */
public class Emx2CorpusYamlConverter {

  private static final String[] SUBDIRECTORIES = {"shared", "specific"};
  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String COLUMN_NAME = "columnName";
  private static final String COLUMN_TYPE = "columnType";
  private static final String TABLE_NAME = "tableName";
  private static final String TABLE_TYPE = "tableType";
  private static final String REQUIRED = "required";
  private static final String NOT_REQUIRED = "false";
  private static final String TABLE_EXTENDS = "tableExtends";
  private static final String REF_TABLE = "refTable";
  private static final String REF_SCHEMA = "refSchema";
  private static final String ONTOLOGY = "ontology";
  private static final String ONTOLOGY_ARRAY = "ontology_array";
  private static final String ONTOLOGIES = "ONTOLOGIES";
  private static final String CORPUS_VERSION = "1.0.0";
  private static final String CSV_SUFFIX = ".csv";
  private static final String YAML_SUFFIX = ".yaml";

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      throw new MolgenisException(
          "Usage: Emx2CorpusYamlConverter <inputModelsDir> <outputYamlDir>");
    }
    new Emx2CorpusYamlConverter().run(Path.of(args[0]), Path.of(args[1]));
  }

  public void run(Path modelsDirectory, Path outputDirectory) throws IOException {
    Map<String, List<Row>> modelRows = readModels(modelsDirectory);
    Map<String, List<String>> tableOwners = mapTableOwners(modelRows);
    Set<String> dataTables = dataTables(modelRows);
    Map<String, String> skipped = new TreeMap<>();
    for (Map.Entry<String, List<Row>> model : modelRows.entrySet()) {
      String relativeKey = model.getKey();
      String reason = structuralSkipReason(model.getValue(), relativeKey, tableOwners, dataTables);
      if (reason == null) {
        Path target = outputDirectory.resolve(relativeKey.replace(CSV_SUFFIX, YAML_SUFFIX));
        reason = convert(model.getValue(), target);
      }
      if (reason != null) {
        skipped.put(relativeKey, reason);
      }
    }
    writeSkippedManifest(outputDirectory, skipped);
  }

  private Map<String, List<Row>> readModels(Path modelsDirectory) throws IOException {
    Map<String, List<Row>> modelRows = new LinkedHashMap<>();
    for (String subdirectory : SUBDIRECTORIES) {
      Path directory = modelsDirectory.resolve(subdirectory);
      if (!Files.isDirectory(directory)) {
        continue;
      }
      try (Stream<Path> stream = Files.list(directory)) {
        for (Path file :
            stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(CSV_SUFFIX))
                .sorted()
                .toList()) {
          List<Row> rows = new ArrayList<>();
          CsvTableReader.read(file.toFile()).forEach(rows::add);
          modelRows.put(subdirectory + "/" + file.getFileName(), rows);
        }
      }
    }
    return modelRows;
  }

  private Map<String, List<String>> mapTableOwners(Map<String, List<Row>> modelRows) {
    Map<String, List<String>> owners = new TreeMap<>();
    for (Map.Entry<String, List<Row>> model : modelRows.entrySet()) {
      for (String tableName : definedTables(model.getValue())) {
        owners.computeIfAbsent(tableName, key -> new ArrayList<>()).add(model.getKey());
      }
    }
    return owners;
  }

  private List<String> definedTables(List<Row> rows) {
    List<String> names = new ArrayList<>();
    for (Row row : rows) {
      if (isTableDefinition(row)) {
        names.add(row.getString(TABLE_NAME));
      }
    }
    return names;
  }

  private Set<String> dataTables(Map<String, List<Row>> modelRows) {
    Set<String> result = new TreeSet<>();
    for (List<Row> rows : modelRows.values()) {
      for (Row row : rows) {
        if (isTableDefinition(row) && !ONTOLOGIES.equalsIgnoreCase(row.getString(TABLE_TYPE))) {
          result.add(row.getString(TABLE_NAME));
        }
      }
    }
    return result;
  }

  private boolean isTableDefinition(Row row) {
    String tableName = row.getString(TABLE_NAME);
    String columnName = row.getString(COLUMN_NAME);
    return tableName != null
        && !tableName.isBlank()
        && (columnName == null || columnName.isBlank());
  }

  private String structuralSkipReason(
      List<Row> rows,
      String relativeKey,
      Map<String, List<String>> tableOwners,
      Set<String> dataTables) {
    String multipleDefinition = multipleDefinitionReason(rows);
    if (multipleDefinition != null) {
      return multipleDefinition;
    }
    String externalParent = externalParentReason(rows);
    if (externalParent != null) {
      return externalParent;
    }
    String crossFile = crossFileReason(rows, relativeKey, tableOwners);
    if (crossFile != null) {
      return crossFile;
    }
    return ontologyReferenceReason(rows, dataTables);
  }

  private String ontologyReferenceReason(List<Row> rows, Set<String> dataTables) {
    TreeSet<String> conflicts = new TreeSet<>();
    for (Row row : rows) {
      String columnType = row.getString(COLUMN_TYPE);
      String refSchema = row.getString(REF_SCHEMA);
      String refTable = row.getString(REF_TABLE);
      boolean sameSchema = refSchema == null || refSchema.isBlank();
      if ((ONTOLOGY.equals(columnType) || ONTOLOGY_ARRAY.equals(columnType))
          && sameSchema
          && refTable != null
          && dataTables.contains(refTable)) {
        conflicts.add(refTable);
      }
    }
    if (conflicts.isEmpty()) {
      return null;
    }
    return "references data table(s) as ontology: " + String.join(", ", conflicts);
  }

  private String multipleDefinitionReason(List<Row> rows) {
    Map<String, Integer> counts = new TreeMap<>();
    for (String tableName : definedTables(rows)) {
      counts.merge(tableName, 1, Integer::sum);
    }
    TreeSet<String> duplicates = new TreeSet<>();
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      if (entry.getValue() > 1) {
        duplicates.add(entry.getKey());
      }
    }
    if (duplicates.isEmpty()) {
      return null;
    }
    return "multiple definition rows for table(s): " + String.join(", ", duplicates);
  }

  private String externalParentReason(List<Row> rows) {
    List<String> definedInFile = definedTables(rows);
    TreeSet<String> external = new TreeSet<>();
    for (Row row : rows) {
      if (!isTableDefinition(row)) {
        continue;
      }
      String parent = row.getString(TABLE_EXTENDS);
      if (parent != null && !parent.isBlank() && !definedInFile.contains(parent)) {
        external.add(row.getString(TABLE_NAME) + " -> " + parent);
      }
    }
    if (external.isEmpty()) {
      return null;
    }
    return "extends a table not defined in the same model: " + String.join(", ", external);
  }

  private String crossFileReason(
      List<Row> rows, String relativeKey, Map<String, List<String>> tableOwners) {
    TreeSet<String> shared = new TreeSet<>();
    for (String tableName : definedTables(rows)) {
      List<String> owners = tableOwners.get(tableName);
      if (owners != null && owners.size() > 1) {
        shared.add(tableName);
      }
    }
    if (shared.isEmpty()) {
      return null;
    }
    return "table(s) reused across bundles: "
        + String.join(", ", shared)
        + " — legitimate cross-bundle corpus usage; only the converter's whole-corpus single view"
        + " conflicts, per-bundle composition would disambiguate";
  }

  private String convert(List<Row> rows, Path target) throws IOException {
    SchemaMetadata beforeExport = Emx2.fromRowList(rows);
    String yaml =
        Emx2Yaml.toSingleFile(
            new Emx2YamlBundle(beforeExport, Emx2Yaml.SUPPORTED_FORMAT_VERSION, CORPUS_VERSION));
    SchemaMetadata afterImport = Emx2Yaml.fromBundleFiles(Map.of(MOLGENIS_YAML, yaml)).schema();
    if (!fidelityRows(beforeExport).equals(fidelityRows(afterImport))) {
      return "round-trip fidelity mismatch";
    }
    Files.createDirectories(target.getParent());
    Files.writeString(target, yaml);
    return null;
  }

  private List<String> fidelityRows(SchemaMetadata schema) {
    List<String> lines = new ArrayList<>();
    for (Row row : Emx2.toRowList(schema)) {
      Map<String, Object> sorted = new TreeMap<>(row.getValueMap());
      StringBuilder line = new StringBuilder();
      for (Map.Entry<String, Object> entry : sorted.entrySet()) {
        String text = render(entry.getValue());
        if (text.isEmpty() || (REQUIRED.equals(entry.getKey()) && NOT_REQUIRED.equals(text))) {
          continue;
        }
        line.append(entry.getKey()).append('=').append(text).append('|');
      }
      if (!isBareOntologyStub(line.toString())) {
        lines.add(line.toString());
      }
    }
    lines.sort(String::compareTo);
    return lines;
  }

  private boolean isBareOntologyStub(String line) {
    String[] parts = line.split("\\|");
    if (parts.length != 2) {
      return false;
    }
    boolean hasTableName = false;
    boolean isOntology = false;
    for (String part : parts) {
      if (part.startsWith(TABLE_NAME + "=")) {
        hasTableName = true;
      } else if (part.equals(TABLE_TYPE + "=" + ONTOLOGIES)) {
        isOntology = true;
      }
    }
    return hasTableName && isOntology;
  }

  private String render(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Object[] array) {
      return array.length == 0 ? "" : Arrays.toString(array);
    }
    return String.valueOf(value);
  }

  private void writeSkippedManifest(Path outputDirectory, Map<String, String> skipped)
      throws IOException {
    Files.createDirectories(outputDirectory);
    StringBuilder manifest = new StringBuilder();
    manifest.append("# Skipped corpus models\n\n");
    manifest.append(
        "Generated by "
            + Emx2CorpusYamlConverter.class.getSimpleName()
            + ". These CSV models were NOT emitted by the whole-corpus single-view converter. Most"
            + " reasons are genuine lossy mappings; the cross-bundle reuse reason is instead"
            + " legitimate corpus usage that only this single-view converter cannot express —"
            + " per-bundle composition (see the import-composition line) would disambiguate it. See"
            + " each reason below.\n\n");
    manifest.append("| Model | Reason |\n");
    manifest.append("|-------|--------|\n");
    for (Map.Entry<String, String> entry : skipped.entrySet()) {
      manifest.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue());
      manifest.append(" |\n");
    }
    Files.writeString(outputDirectory.resolve("SKIPPED.md"), manifest.toString());
  }
}
