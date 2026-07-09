package org.molgenis.emx2.yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.util.*;
import lombok.Builder;
import org.molgenis.emx2.*;
import org.molgenis.emx2.yaml.format.*;

// todo incomplete
public class YamlFactory {
  public static final String TABLES_PATH = "schema/";
  static ObjectMapper mapper =
      new ObjectMapper(YAMLFactory.builder().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).build())
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  @Builder
  public record Options(String packageName, Boolean tableAsImports, String profile) {
    public Options {
      if (packageName == null) {
        packageName = "molgenis";
      }
      if (tableAsImports == null) {
        tableAsImports = false;
      }
    }
  }

  public Map<String, String> toYamlBundle(
      YamlPackageDescription packageDescription, SchemaMetadata schemaMetadata, Options options) {
    try {
      // get the raw bundle content
      Map<String, String> packageYamlBundle = new LinkedHashMap<>();
      packageYamlBundle.put(
          options.packageName + ".yaml",
          mapper.writeValueAsString(toYamlPackage(packageDescription, schemaMetadata, options)));

      // get each table as seperate package file if desired
      if (options.tableAsImports) {
        addTablesToBundle(packageYamlBundle, schemaMetadata, options);
      }

      // put the profiles in import files
      Set<String> profiles = getAllProfiles(schemaMetadata);
      for (String profile : profiles) {
        Options profileOptions =
            Options.builder().packageName(options.packageName()).profile(profile).build();
        packageYamlBundle.put(
            "profiles/" + profile + ".yaml",
            mapper.writeValueAsString(toYamlProfilePackage(schemaMetadata, profileOptions)));
      }

      return packageYamlBundle;
    } catch (JsonProcessingException e) {
      throw new MolgenisException("Yaml creation failed", e);
    }
  }

  private void addTablesToBundle(
      Map<String, String> packageYamlBundle, SchemaMetadata schemaMetadata, Options options)
      throws JsonProcessingException {
    for (TableMetadata tableMetadata :
        schemaMetadata.getTables().stream()
            .filter(table -> table.getInheritName() == null)
            .toList()) {
      YamlMolgenisPackage table =
          YamlMolgenisPackage.builder().table(toYamlTable(tableMetadata, options)).build();
      packageYamlBundle.put(
          TABLES_PATH + tableMetadata.getIdentifier() + ".yaml", mapper.writeValueAsString(table));
    }
  }

  Set<String> getAllProfiles(SchemaMetadata schemaMetadata) {
    Set<String> result = new LinkedHashSet<>();
    schemaMetadata
        .getTables()
        .forEach(
            tableMetadata -> {
              result.addAll(List.of(tableMetadata.getProfiles()));
              tableMetadata
                  .getColumns()
                  .forEach(column -> result.addAll(List.of(column.getProfiles())));
            });

    return result;
  }

  public YamlMolgenisPackage toYamlPackage(
      YamlPackageDescription description, SchemaMetadata schemaMetadata, Options options) {
    if (options.tableAsImports()) {
      List<YamlTable> yamlImportList =
          schemaMetadata.getTables().stream()
              .map(
                  tableMetadata ->
                      YamlTable.builder()
                          .imports(TABLES_PATH + tableMetadata.getIdentifier() + ".yaml")
                          .build())
              .toList();
      return YamlMolgenisPackage.builder().description(description).schema(yamlImportList).build();
    } else {
      List<YamlTable> yamlTableList;
      yamlTableList =
          schemaMetadata.getTables().stream()
              .map(tableMetadata -> toYamlTable(tableMetadata, options))
              .toList();
      return YamlMolgenisPackage.builder().description(description).schema(yamlTableList).build();
    }
  }

  public YamlMolgenisPackage toYamlProfilePackage(SchemaMetadata schemaMetadata, Options options) {
    List<YamlInclude> includes =
        schemaMetadata.getTables().stream()
            .filter(
                tableMetadata ->
                    tableMetadata.getProfiles() == null
                        || List.of(tableMetadata.getProfiles()).contains(options.profile()))
            .map(tableMetadata -> toYamlProfileInclude(tableMetadata, options))
            .toList();
    YamlTable tableImports =
        YamlTable.builder()
            .imports("../" + options.packageName() + ".yaml")
            .include(includes)
            .build();
    return YamlMolgenisPackage.builder().schema(List.of(tableImports)).build();
  }

  private YamlInclude toYamlProfileInclude(TableMetadata tableMetadata, Options options) {
    String[] columnNames =
        tableMetadata.getColumns().stream()
            .filter(
                column ->
                    column.getProfiles() == null
                        || List.of(column.getProfiles()).contains(options.profile()))
            .map(Column::getName)
            .toArray(String[]::new);
    return YamlInclude.builder().table(tableMetadata.getTableName()).columns(columnNames).build();
  }

  public YamlTable toYamlTable(TableMetadata tableMetadata, Options options) {
    List<YamlColumn> yamlColumnList =
        tableMetadata.getColumnsIncludingSubclasses().stream()
            .filter(
                column ->
                    options.profile() == null
                        || column.getProfiles() == null
                        || List.of(column.getProfiles()).contains(options.profile()))
            .map(column -> toYamlColumn(tableMetadata, column, options))
            .toList();
    List<YamlSubclass> subclasses =
        tableMetadata.getSubclassTables().stream()
            .map(
                subclass ->
                    YamlSubclass.builder()
                        .name(subclass.getTableName())
                        .description(subclass.getDescription())
                        .inherits(
                            subclass.getInheritName().equals(tableMetadata.getTableName())
                                ? null
                                : List.of(subclass.getInheritName()))
                        .build())
            .toList();
    return YamlTable.builder()
        .table(tableMetadata.getTableName())
        .subclasses(subclasses)
        .description(tableMetadata.getDescription())
        .columns(yamlColumnList)
        .build();
  }

  private YamlColumn toYamlColumn(TableMetadata tableMetadata, Column column, Options options) {
    return YamlColumn.builder()
        .name(column.getName())
        .key(column.getKey() == 0 ? null : column.getKey())
        .description(column.getDescriptions().get("en")) // oh no, forgot about language
        .semantics(column.getSemantics())
        .type(
            column.getColumnType().equals(ColumnType.STRING)
                ? null
                : column.getColumnType().name().toLowerCase())
        .subclass(
            tableMetadata.getTableName().equals(column.getTableName())
                ? null
                : column.getTableName())
        .refSchema(column.getRefSchemaName())
        .refTable(column.getRefTableName())
        .refBack(column.getRefBack())
        .build();
  }
}
