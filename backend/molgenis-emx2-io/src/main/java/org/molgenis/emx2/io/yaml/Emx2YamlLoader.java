package org.molgenis.emx2.io.yaml;

import static org.molgenis.emx2.ColumnType.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import org.molgenis.emx2.*;

/** Conform to molgenis-emx2.schema.json */
public class Emx2YamlLoader {

  public SchemaMetadata read(String yamlString) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    String[] documents = yamlString.split("---");

    SchemaMetadata schema = new SchemaMetadata();
    Pattern schemaRegexp = Pattern.compile(".*schema\\s*:.*", Pattern.DOTALL);
    for (String document : documents) {
      if (schemaRegexp.matcher(document).matches()) {
        YamlSchema yamlSchema = mapper.readValue(document, YamlSchema.class);
        setSchemaMetadata(yamlSchema, schema);
      } else {
        YamlTable yamlTable = mapper.readValue(document, YamlTable.class);
        setTableMetadata(yamlTable, schema);
      }
    }

    return schema;
  }

  public String write(SchemaMetadata schema) throws JsonProcessingException {
    StringBuilder builder = new StringBuilder();
    YamlSchema yamlSchema =
        new YamlSchema(
            schema.getName(),
            schema.getDescription(),
            null, // todo version
            null, // todo license
            null, // todo prefixes
            schema.getSettings(),
            null // todo authors
            );

    ObjectMapper mapper =
        new ObjectMapper(
            new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .disable((YAMLGenerator.Feature.SPLIT_LINES)));
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

    builder.append(mapper.writeValueAsString(yamlSchema));

    for (TableMetadata table : schema.getTables()) {
      // we nest inherited columns from same schema underneath
      if (table.getTableType().equals(TableType.DATA) && table.getInheritName() == null
          || table.getImportSchema() != null) {
        YamlTable yamlTable =
            new YamlTable(
                table.getTableName(),
                null, // todo uri
                table.getDescription(),
                getYamlSubclassMap(table),
                getYamlColumnMap(table),
                table.getSettings(),
                table.getProfiles(),
                null // todo ref_label
                );
        builder.append("\n---\n\n" + mapper.writeValueAsString(yamlTable));
      }
    }

    // clean up 'key : null' to 'key :' statements and return
    return builder.toString().replace(": null\n", ":\n");
  }

  private Map<String, YamlSubclass> getYamlSubclassMap(TableMetadata table) {
    List<TableMetadata> subclasses =
        table.getSchema().getTables().stream()
            .filter(
                t ->
                    t.getInheritName() != null && t.getRootTableName().equals(table.getTableName()))
            .toList();
    if (subclasses.size() > 0) {
      Map<String, YamlSubclass> result = new HashMap<>();
      for (TableMetadata subclass : subclasses) {
        YamlSubclass yamlSubclass =
            new YamlSubclass(
                subclass.getInheritName().equals(table.getTableName())
                    ? null
                    : subclass.getInheritName(),
                subclass.getDescription(),
                null // todo uri
                );
        if (checkFieldsAreNull(yamlSubclass)) {
          result.put(subclass.getTableName(), null);
        } else {
          result.put(subclass.getTableName(), yamlSubclass);
        }
      }
      return result;
    } else {
      return null;
    }
  }

  private Map<String, YamlColumn> getYamlColumnMap(TableMetadata table) {
    Map<String, YamlColumn> result = new LinkedHashMap<>();

    List<Column> allColumnsIncludingInherited =
        table.getSchema().getTables().stream()
            .filter(t -> t.getRootTableName().equals(table.getTableName()))
            .map(t -> t.getNonInheritedColumns())
            .flatMap(List::stream)
            .filter(c -> !c.getName().startsWith("mg_"))
            .sorted(Comparator.comparing(Column::getPosition))
            .toList();

    for (Column column : allColumnsIncludingInherited) {
      // we only do current schema
      if (column.getTable().getImportSchema() == null) {
        YamlColumn yamlColumn =
            new YamlColumn(
                column.getLabel().equals(column.getName()) ? null : column.getLabel(),
                column.getDescriptions().get("en"),
                getYamlColumnType(column.getColumnType()),
                column.getKey() > 0 ? column.getKey() : null,
                column.isRequired() ? true : null,
                getYamlRefString(REF, column),
                getYamlRefString(REF_ARRAY, column),
                getYamlRefString(ONTOLOGY, column),
                getYamlRefString(ONTOLOGY_ARRAY, column),
                getYamlRefBackString(column),
                column.getRefLabel(),
                column.getDefaultValue(),
                column.getVisible(),
                column.getValidation(),
                null,
                column.getTableName().equals(table.getTableName()) ? null : column.getTableName(),
                // todo uri
                // todo create getDescription()
                column.getProfiles());
        if (checkFieldsAreNull(yamlColumn)) {
          result.put(column.getName(), null);
        } else {
          result.put(column.getName(), yamlColumn);
        }
      }
    }

    return result;
  }

  private String getYamlRefBackString(Column column) {
    if (column.isRefback()) {
      if (!column.getTable().getSchemaName().equals(column.getRefSchemaName())) {
        return column.getRefSchemaName()
            + "."
            + column.getRefTableName()
            + "."
            + column.getRefBack();
      } else {
        return column.getRefTableName() + "." + column.getRefBack();
      }
    }
    return null;
  }

  private String getYamlRefString(ColumnType columnType, Column column) {
    if (column.getColumnType().equals(columnType)) {
      if (!column.getTable().getSchemaName().equals(column.getRefSchemaName())) {
        return column.getRefSchemaName() + "." + column.getRefTableName();
      } else {
        return column.getRefTableName();
      }
    }
    return null;
  }

  private String getYamlColumnType(ColumnType columnType) {
    if (columnType.isReference() || columnType.equals(STRING)) {
      return null;
    } else {
      return columnType.name().toLowerCase();
    }
  }

  private void setTableMetadata(YamlTable yamlTable, SchemaMetadata schema) {

    // main table
    TableMetadata tableMetadata = new TableMetadata(yamlTable.table);
    tableMetadata.setDescription(yamlTable.description);
    tableMetadata.setSettings(yamlTable.settings);
    tableMetadata.setSemantics(yamlTable.uri);
    setColumnMetadata(yamlTable, tableMetadata);
    schema.create(tableMetadata);

    // subclass table
    if (yamlTable.subclasses != null) {
      for (Map.Entry<String, YamlSubclass> entry : yamlTable.subclasses.entrySet()) {
        YamlSubclass yamlSubclass = entry.getValue();
        TableMetadata subclassMetadata = new TableMetadata(entry.getKey());
        subclassMetadata.setInheritName(tableMetadata.getTableName());
        if (yamlSubclass != null) {
          subclassMetadata.setDescription(yamlSubclass.description);
          subclassMetadata.setSemantics(yamlSubclass.uri);
          if (yamlSubclass.inherits != null) {
            subclassMetadata.setInheritName(yamlSubclass.inherits);
          }
          if (yamlSubclass.inherits != null) {
            subclassMetadata.setInheritName(yamlSubclass.inherits);
          } else {
            subclassMetadata.setInheritName(tableMetadata.getInheritName());
          }
        }
        setColumnMetadata(yamlTable, subclassMetadata);
        schema.create(subclassMetadata);
      }
    }
  }

  private static void setColumnMetadata(YamlTable yamlTable, TableMetadata tableMetadata) {
    for (Map.Entry<String, YamlColumn> entry : yamlTable.columns.entrySet()) {
      YamlColumn yamlColumn = entry.getValue();
      Column column = new Column(entry.getKey());
      if (yamlColumn != null) {
        column.setDescription(yamlColumn.description);
        if (yamlColumn.type != null) {
          column.setType(ColumnType.valueOf(yamlColumn.type.toUpperCase()));
        }
        column.setLabel(yamlColumn.label);
        if (yamlColumn.key != null) column.setKey(yamlColumn.key);
        if (yamlColumn.required != null) column.setRequired(yamlColumn.required);
        column.setValidation(yamlColumn.validIf);
        column.setVisible(yamlColumn.visibleIf);
        column.setProfiles(yamlColumn.profiles);

        if (yamlColumn.ontology != null) {
          column.setType(ColumnType.ONTOLOGY);
          setRefParameters(column, yamlColumn.ontology);
        } else if (yamlColumn.ontology_array != null) {
          column.setType(ColumnType.ONTOLOGY_ARRAY);
          setRefParameters(column, yamlColumn.ontology_array);
        } else if (yamlColumn.ref != null) {
          column.setType(REF);
          setRefParameters(column, yamlColumn.ref);
        } else if (yamlColumn.ref_array != null) {
          column.setType(ColumnType.REF_ARRAY);
          setRefParameters(column, yamlColumn.ref_array);
        } else if (yamlColumn.ref_back != null) {
          column.setType(ColumnType.REFBACK);
          setRefBackParameters(column, yamlColumn.ref_back);
        }

        column.setRefLabel(yamlColumn.ref_label);
      }

      if (yamlColumn == null
          || yamlColumn.subclass == null
          || tableMetadata.getTableName().equals(yamlColumn.subclass)) {
        tableMetadata.add(column);
      }
    }
  }

  private static void setRefParameters(Column column, String ref) {
    if (ref.contains(".")) {
      String[] qualifiedRef = ref.split("\\.");
      column.setRefSchemaName(qualifiedRef[0]);
      column.setRefTable(qualifiedRef[1]);
    } else {
      column.setRefTable(ref);
    }
  }

  private static void setRefBackParameters(Column column, String ref) {
    String[] qualifiedRef = ref.split("\\.");
    if (qualifiedRef.length == 3) {
      column.setRefSchemaName(qualifiedRef[0]);
      column.setRefTable(qualifiedRef[1]);
      column.setRefBack(qualifiedRef[2]);
    } else {
      column.setRefTable(qualifiedRef[0]);
      column.setRefBack(qualifiedRef[1]);
    }
  }

  private void setSchemaMetadata(YamlSchema yamlSchema, SchemaMetadata schema) {
    schema.setName(yamlSchema.schema);
    schema.setDescription(yamlSchema.description);
    schema.setSettings(yamlSchema.settings);
    // todo version
    // todo prefixes
    // todo license
    // todo authors
  }

  private record YamlSchema(
      String schema,
      String description,
      String version,
      YamlLicense license,
      Map<String, String> prefixes,
      Map<String, String> settings,
      Map<String, YamlAuthor> authors) {}

  private record YamlTable(
      String table,
      String uri,
      String description,
      @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, YamlSubclass> subclasses,
      @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, YamlColumn> columns,
      Map<String, String> settings,
      String[] profiles,
      String ref_label) {}

  private record YamlColumn(
      String label,
      String description,
      String type,
      Integer key,
      Boolean required,
      String ref,
      String ref_array,
      String ontology,
      String ontology_array,
      String ref_back,
      String ref_label,
      @JsonProperty("default") String defaultValue,
      String visibleIf,
      String validIf,
      String uri,
      String subclass,
      String[] profiles) {}

  private record YamlAuthor(String email, String orcid) {}

  private record YamlLicense(String name, String uri) {}

  private record YamlSubclass(
      @JsonProperty("extends") String inherits, String description, String uri) {}

  private static boolean checkFieldsAreNull(Record record) {
    for (Field field : record.getClass().getDeclaredFields()) {
      try {
        if (field.get(record) != null) {
          return false;
        }
      } catch (IllegalAccessException e) {
        // Handle exception as needed
        e.printStackTrace();
      }
    }
    return true;
  }
}
