package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2 {

  public static final String TABLE_NAME = "tableName";
  public static final String COLUMN_NAME = "columnName";
  public static final String OLD_NAME = "oldName";
  public static final String DROP = "drop";
  public static final String DESCRIPTION = "description";
  public static final String TABLE_EXTENDS = "tableExtends";
  public static final String COLUMN_TYPE = "columnType";
  public static final String LABEL = "label";
  public static final String KEY = "key";
  public static final String REF_SCHEMA = "refSchema";
  public static final String REF_TABLE = "refTable";
  public static final String REF_LINK = "refLink";
  public static final String REF_JS_TEMPLATE = "refLabel";
  public static final String REF_BACK = "refBack";
  public static final String REQUIRED = "required";
  public static final String READ_ONLY = "readonly";
  public static final String DEFAULT_VALUE = "defaultValue";
  private static final String VALIDATION = "validation";
  private static final String VISIBLE = "visible";
  private static final String COMPUTED = "computed";
  private static final String SEMANTICS = "semantics";
  private static final String COLUMN_POSITION = "position";
  private static final String TABLE_TYPE = "tableType";
  private static final String PROFILES = "profiles";

  private Emx2() {
    // hidden
  }

  public static SchemaMetadata fromRowList(Iterable<Row> rows) {
    SchemaMetadata schema = new SchemaMetadata();
    int lineNo = 1; // use as position
    int columnPosition = 0;

    for (Row row : rows) {
      String tableName = row.getString(TABLE_NAME);
      if (tableName == null) {
        throw new MolgenisException(
            "Parsing of sheet molgenis failed: Required column "
                + TABLE_NAME
                + " is empty on line "
                + lineNo);
      }

      if (schema.getTableMetadata(tableName) == null) {
        schema.create(table(tableName));
      }

      // load table metadata, this is when columnName is empty
      if (row.getString(COLUMN_NAME) == null) {
        schema.getTableMetadata(tableName).setInheritName(row.getString(TABLE_EXTENDS));
        schema.getTableMetadata(tableName).setImportSchema(row.getString(REF_SCHEMA));
        schema.getTableMetadata(tableName).setSemantics(row.getStringArray(SEMANTICS, false));
        schema.getTableMetadata(tableName).setProfiles(row.getStringArray(PROFILES, false));
        if (row.getString(TABLE_TYPE) != null) {
          schema
              .getTableMetadata(tableName)
              .setTableType(TableType.valueOf(row.getString(TABLE_TYPE)));
        }

        if (row.getString(OLD_NAME) != null) {
          schema.getTableMetadata(tableName).setOldName(row.getString(OLD_NAME));
        }
        if (!row.isNull(DROP, BOOL) && row.getBoolean(DROP)) {
          schema.getTableMetadata(tableName).drop();
        }
        if (row.containsName(LABEL))
          schema.getTableMetadata(tableName).setLabel(row.getString(LABEL));
        // labels i18n
        row.getColumnNames().stream()
            .filter(name -> name.startsWith(LABEL + ":"))
            .forEach(
                value -> {
                  schema
                      .getTableMetadata(tableName)
                      .setLabel(row.getString(value, false), (value.split(":")[1]));
                });
        if (row.containsName(DESCRIPTION))
          schema.getTableMetadata(tableName).setDescription(row.getString(DESCRIPTION, false));
        // description i18n
        row.getColumnNames().stream()
            .filter(name -> name.startsWith(DESCRIPTION + ":"))
            .forEach(
                value -> {
                  schema
                      .getTableMetadata(tableName)
                      .setDescription(row.getString(value, false), (value.split(":")[1]));
                });
      }

      // load column metadata
      else {
        try {
          if (row.getString(TABLE_EXTENDS) != null) {
            throw new MolgenisException(
                "Parsing of sheet molgenis failed: Column "
                    + TABLE_EXTENDS
                    + " not supported for columns at "
                    + lineNo);
          }

          Column column = column(row.getString(COLUMN_NAME));
          if (row.notNull(COLUMN_TYPE))
            column.setType(ColumnType.valueOf(row.getString(COLUMN_TYPE).toUpperCase().trim()));
          if (row.notNull(KEY)) column.setKey(row.getInteger(KEY));
          if (row.notNull(REF_SCHEMA)) column.setRefSchemaName(row.getString(REF_SCHEMA));
          if (row.notNull(REF_TABLE)) column.setRefTable(row.getString(REF_TABLE));
          if (row.notNull(REF_LINK)) column.setRefLink(row.getString(REF_LINK));
          if (row.notNull(REF_BACK)) column.setRefBack(row.getString(REF_BACK));
          if (row.notNull(REQUIRED)) column.setRequired(row.getString(REQUIRED));
          if (row.notNull(DEFAULT_VALUE)) column.setDefaultValue(row.getString(DEFAULT_VALUE));
          if (row.notNull(DESCRIPTION)) column.setDescription(row.getString(DESCRIPTION));
          // description i18n
          row.getColumnNames().stream()
              .filter(name -> name.startsWith(DESCRIPTION + ":"))
              .forEach(
                  value -> {
                    column.setDescription(row.getString(value), (value.split(":")[1]));
                  });
          if (row.notNull(VALIDATION)) column.setValidation(row.getString(VALIDATION));
          if (row.notNull(VISIBLE)) column.setVisible(row.getString(VISIBLE));
          if (row.notNull(COMPUTED)) column.setComputed(row.getString(COMPUTED));
          if (row.notNull(SEMANTICS)) column.setSemantics(row.getStringArray(SEMANTICS));
          if (row.notNull(PROFILES)) column.setProfiles(row.getStringArray(PROFILES));
          if (row.notNull(REF_JS_TEMPLATE)) column.setRefLabel(row.getString(REF_JS_TEMPLATE));
          if (row.notNull(COLUMN_POSITION)) column.setPosition(row.getInteger(COLUMN_POSITION));
          if (row.notNull(OLD_NAME)) column.setOldName(row.getString(OLD_NAME));
          if (row.notNull(READ_ONLY)) column.setReadonly(getBoolAsBoolean(row));

          if (!row.isNull(DROP, BOOL) && row.getBoolean(DROP)) {
            column.drop();
          } else {
            column.setPosition(columnPosition++);
            // this ensures positions accross table hiearchy matches those in imported file
          }
          // get labels
          if (row.notNull(LABEL)) column.setLabel(row.getString(LABEL));
          // labels i18n
          row.getColumnNames().stream()
              .filter(name -> name.startsWith(LABEL + ":"))
              .forEach(
                  value -> {
                    column.setLabel(row.getString(value), (value.split(":")[1]));
                  });

          schema.getTableMetadata(tableName).add(column);
        } catch (Exception e) {
          throw new MolgenisException("Error on line " + lineNo + ":" + e.getMessage());
        }
      }
      lineNo++;
    }
    return schema;
  }

  private static boolean getBoolAsBoolean(Row row) {
    String readonlyString = row.getString(READ_ONLY);
    return readonlyString.equalsIgnoreCase("true");
  }

  public static void outputMetadata(TableStore store, Schema schema) {
    outputMetadata(store, schema.getMetadata());
  }

  public static void outputMetadata(TableStore store, SchemaMetadata schema) {
    // headers
    List<String> headers = getHeaders(schema);
    store.writeTable("molgenis", headers, toRowList(schema));
  }

  public static List getHeaders(SchemaMetadata schema) {
    List headers = new ArrayList();
    headers.addAll(
        List.of(
            TABLE_NAME,
            TABLE_EXTENDS,
            TABLE_TYPE,
            COLUMN_NAME,
            COLUMN_TYPE,
            KEY,
            REQUIRED,
            READ_ONLY,
            REF_SCHEMA,
            REF_TABLE,
            REF_LINK,
            REF_BACK,
            REF_JS_TEMPLATE,
            DEFAULT_VALUE,
            VALIDATION,
            VISIBLE,
            COMPUTED,
            SEMANTICS,
            PROFILES));
    // add label locales that are used
    schema
        .getLocales()
        .forEach(locale -> headers.add("en".equals(locale) ? LABEL : LABEL + ":" + locale));
    schema
        .getLocales()
        .forEach(
            locale -> headers.add("en".equals(locale) ? DESCRIPTION : DESCRIPTION + ":" + locale));
    return headers;
  }

  /** Outputs tables + columns. */
  public static List<Row> toRowList(SchemaMetadata schema) {

    // get the metadata in right order; exclude ontologies
    List<TableMetadata> tables =
        schema.getTables().stream()
            .filter(t -> !TableType.ONTOLOGIES.equals(t.getTableType()))
            .toList();
    List<Column> columns =
        tables.stream()
            .map(t -> t.getNonInheritedColumns())
            .flatMap(List::stream)
            .sorted(
                Comparator.comparing(Column::getRootTableName).thenComparing(Column::getPosition))
            .toList(); // NOSONAR cannot use toList because immutable

    List<Row> result = new ArrayList<>();
    for (TableMetadata table : tables) {

      Row row = new Row();
      // set null columns to ensure sensible order
      row.setString(TABLE_NAME, table.getTableName());
      row.setString(TABLE_EXTENDS, table.getInheritName());
      row.setString(TABLE_TYPE, null);
      row.setString(COLUMN_NAME, null);
      row.setString(COLUMN_TYPE, null);
      row.setString(KEY, null);
      row.setString(REQUIRED, null);
      row.setString(DEFAULT_VALUE, null);
      row.setString(READ_ONLY, null);
      row.setString(REF_SCHEMA, null);
      row.setString(REF_TABLE, null);
      row.setString(REF_LINK, null);
      row.setString(REF_BACK, null);
      row.setString(VALIDATION, null);
      row.setString(VISIBLE, null);
      row.setString(COMPUTED, null);
      if (table.getSemantics() != null) row.setStringArray(SEMANTICS, table.getSemantics());
      if (table.getProfiles() != null) row.setStringArray(PROFILES, table.getProfiles());
      for (Map.Entry<String, String> entry : table.getLabels().entrySet()) {
        if (entry.getKey().equals("en")) {
          row.set(LABEL, entry.getValue());
        } else {
          row.set(LABEL + ":" + entry.getKey(), entry.getValue());
        }
      }
      for (Map.Entry<String, String> entry : table.getDescriptions().entrySet()) {
        if (entry.getKey().equals("en")) {
          row.set(DESCRIPTION, entry.getValue());
        } else {
          row.set(DESCRIPTION + ":" + entry.getKey(), entry.getValue());
        }
      }

      result.add(row);
    }

    // output the columns
    for (Column column : columns) {
      if (!column.isSystemColumn()) {
        Row row = new Row();
        row.setString(TABLE_NAME, column.getTableName());
        row.setString(COLUMN_NAME, column.getName());
        if (!column.getColumnType().equals(STRING))
          row.setString(COLUMN_TYPE, column.getColumnType().toString().toLowerCase());
        if (column.getRequired() != null) row.setString(REQUIRED, column.getRequired());
        if (column.isReadonly()) row.setString(READ_ONLY, column.isReadonly().toString());
        if (column.getDefaultValue() != null)
          row.setString(DEFAULT_VALUE, column.getDefaultValue());
        if (column.getKey() > 0) row.setInt(KEY, column.getKey());
        if (column.getRefSchemaName() != null
            && !column.getRefSchemaName().equals(column.getSchemaName()))
          row.setString(REF_SCHEMA, column.getRefSchemaName());
        if (column.getRefTableName() != null) row.setString(REF_TABLE, column.getRefTableName());
        if (column.getRefLink() != null) row.setString(REF_LINK, column.getRefLink());
        if (column.getRefBack() != null) row.setString(REF_BACK, column.getRefBack());
        if (column.getRefLabel() != null) row.setString(REF_JS_TEMPLATE, column.getRefLabel());
        for (Map.Entry<String, String> label : column.getDescriptions().entrySet()) {
          if (label.getKey().equals("en")) {
            row.set(DESCRIPTION, label.getValue());
          } else {
            row.set(DESCRIPTION + ":" + label.getKey(), label.getValue());
          }
        }
        if (column.getValidation() != null) row.set(VALIDATION, column.getValidation());
        if (column.getComputed() != null) row.set(COMPUTED, column.getComputed());
        if (column.getVisible() != null) row.set(VISIBLE, column.getVisible());
        if (column.getSemantics() != null) row.set(SEMANTICS, column.getSemantics());
        if (column.getProfiles() != null) row.set(PROFILES, column.getProfiles());
        for (Map.Entry<String, String> label : column.getLabels().entrySet()) {
          if (label.getKey().equals("en")) {
            row.set(LABEL, label.getValue());
          } else {
            row.set(LABEL + ":" + label.getKey(), label.getValue());
          }
        }
        result.add(row);
      }
    }
    return result;
  }
}
