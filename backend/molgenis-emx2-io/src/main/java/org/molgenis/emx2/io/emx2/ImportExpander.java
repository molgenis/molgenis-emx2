package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.molgenis.emx2.MolgenisException;

class ImportExpander {

  private static final String FIELD_IMPORTS = "imports";
  private static final String FIELD_TABLES = "tables";
  private static final String FIELD_TABLE = "table";

  private ImportExpander() {}

  @SuppressWarnings("unchecked")
  static void assertNoImports(Map<String, Object> tree, String yamlSource) {
    if (tree.containsKey(FIELD_IMPORTS)) {
      throw new MolgenisException(
          "imports: not allowed in single-file bundle (parsed from stream, no filesystem base directory). "
              + "If imports are needed, use a directory bundle or ZIP upload. Source: "
              + yamlSource);
    }
    for (Map.Entry<String, Object> entry : tree.entrySet()) {
      if (entry.getValue() instanceof Map) {
        assertNoImports((Map<String, Object>) entry.getValue(), yamlSource);
      } else if (entry.getValue() instanceof List) {
        for (Object item : (List<Object>) entry.getValue()) {
          if (item instanceof Map) {
            assertNoImports((Map<String, Object>) item, yamlSource);
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> expandImports(
      Map<String, Object> root, Path baseDir, Set<Path> visited) throws IOException {

    Map<String, Object> result = new LinkedHashMap<>(root);

    Object rawImports = result.get(FIELD_IMPORTS);
    if (!(rawImports instanceof List)) {
      rawImports = null;
    } else {
      result.remove(FIELD_IMPORTS);
    }
    List<String> topImports = (List<String>) rawImports;
    if (topImports != null && !topImports.isEmpty()) {
      Map<String, Object> tablesMap =
          (Map<String, Object>) result.computeIfAbsent(FIELD_TABLES, k -> new LinkedHashMap<>());
      for (String importPath : topImports) {
        List<Path> files = resolveImportPaths(baseDir, importPath);
        for (Path file : files) {
          Path canonical = file.toRealPath();
          if (visited.contains(canonical)) {
            List<String> cycle = buildCycleDescription(visited, canonical);
            throw new MolgenisException("Import cycle detected: " + String.join(" -> ", cycle));
          }
          Map<String, Object> fileContent = loadYaml(file);
          Set<Path> childVisited = new LinkedHashSet<>(visited);
          childVisited.add(canonical);
          Map<String, Object> expandedFile =
              expandImports(fileContent, file.getParent(), childVisited);
          mergeTableFileIntoTablesMap(tablesMap, expandedFile, file);
        }
      }
    }

    for (Map.Entry<String, Object> entry : result.entrySet()) {
      if (entry.getValue() instanceof Map) {
        Map<String, Object> nested = (Map<String, Object>) entry.getValue();
        if (FIELD_TABLES.equals(entry.getKey())) {
          entry.setValue(expandImportsInsideTablesMap(nested, baseDir, visited));
        } else {
          entry.setValue(expandImports(nested, baseDir, visited));
        }
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> expandImportsInsideTablesMap(
      Map<String, Object> tablesMap, Path baseDir, Set<Path> visited) throws IOException {

    Map<String, Object> result = new LinkedHashMap<>();
    List<String> pendingImports = null;

    for (Map.Entry<String, Object> entry : tablesMap.entrySet()) {
      if (FIELD_IMPORTS.equals(entry.getKey()) && entry.getValue() instanceof List) {
        pendingImports = (List<String>) entry.getValue();
      } else {
        Object val = entry.getValue();
        if (val instanceof Map) {
          result.put(entry.getKey(), expandImports((Map<String, Object>) val, baseDir, visited));
        } else {
          result.put(entry.getKey(), val);
        }
      }
    }

    if (pendingImports != null) {
      for (String importPath : pendingImports) {
        List<Path> files = resolveImportPaths(baseDir, importPath);
        for (Path file : files) {
          Path canonical = file.toRealPath();
          if (visited.contains(canonical)) {
            List<String> cycle = buildCycleDescription(visited, canonical);
            throw new MolgenisException("Import cycle detected: " + String.join(" -> ", cycle));
          }
          Map<String, Object> fileContent = loadYaml(file);
          Set<Path> childVisited = new LinkedHashSet<>(visited);
          childVisited.add(canonical);
          Map<String, Object> expandedFile =
              expandImports(fileContent, file.getParent(), childVisited);
          mergeTableFileIntoTablesMap(result, expandedFile, file);
        }
      }
    }

    return result;
  }

  @SuppressWarnings("unchecked")
  private static void mergeTableFileIntoTablesMap(
      Map<String, Object> tablesMap, Map<String, Object> fileContent, Path sourceFile) {
    String tableName = (String) fileContent.get(FIELD_TABLE);
    if (tableName == null) {
      throw new MolgenisException(
          "Imported file '" + sourceFile + "' missing required 'table:' field");
    }
    if (tablesMap.containsKey(tableName)) {
      throw new MolgenisException(
          "Import key collision: table '"
              + tableName
              + "' from '"
              + sourceFile
              + "' already exists in the tables map");
    }
    Map<String, Object> tableBody = new LinkedHashMap<>(fileContent);
    tableBody.remove(FIELD_TABLE);
    Map<String, Object> siblingTables = (Map<String, Object>) tableBody.remove(FIELD_TABLES);
    tablesMap.put(tableName, tableBody);
    if (siblingTables != null) {
      for (Map.Entry<String, Object> sibling : siblingTables.entrySet()) {
        if (tablesMap.containsKey(sibling.getKey())) {
          throw new MolgenisException(
              "Import key collision: table '"
                  + sibling.getKey()
                  + "' (transitively imported via '"
                  + sourceFile
                  + "') already exists in the tables map");
        }
        tablesMap.put(sibling.getKey(), sibling.getValue());
      }
    }
  }

  private static List<Path> resolveImportPaths(Path baseDir, String importPath) throws IOException {
    if (Path.of(importPath).isAbsolute()) {
      throw new MolgenisException("Import path must be relative: '" + importPath + "'");
    }
    String trimmed = importPath;
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    if (trimmed.endsWith("/**")) {
      trimmed = trimmed.substring(0, trimmed.length() - 3);
    } else if (trimmed.endsWith("/*")) {
      trimmed = trimmed.substring(0, trimmed.length() - 2);
    }
    Path baseReal = baseDir.toRealPath();
    Path resolved = baseReal.resolve(trimmed).normalize();
    if (!resolved.startsWith(baseReal)) {
      throw new MolgenisException(
          "Import path escapes bundle base directory: '" + importPath + "'");
    }
    if (!Files.exists(resolved)) {
      throw new MolgenisException(
          "Import path not found: '" + importPath + "' (resolved to: " + resolved + ")");
    }
    if (!resolved.toRealPath().startsWith(baseReal)) {
      throw new MolgenisException(
          "Import path escapes bundle base directory via symlink: '" + importPath + "'");
    }
    if (Files.isDirectory(resolved)) {
      List<Path> result = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolved, "*.yaml")) {
        for (Path path : stream) {
          result.add(path);
        }
      }
      result.sort(Comparator.naturalOrder());
      return result;
    }
    return List.of(resolved);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> loadYaml(Path file) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try (InputStream inputStream = Files.newInputStream(file)) {
      return mapper.readValue(inputStream, Map.class);
    }
  }

  private static List<String> buildCycleDescription(Set<Path> visited, Path cycleRoot) {
    List<String> all = new ArrayList<>();
    for (Path path : visited) {
      all.add(path.toString());
    }
    int startIdx = all.indexOf(cycleRoot.toString());
    List<String> cycle = startIdx >= 0 ? all.subList(startIdx, all.size()) : all;
    List<String> result = new ArrayList<>(cycle);
    result.add(cycleRoot.toString());
    return result;
  }
}
