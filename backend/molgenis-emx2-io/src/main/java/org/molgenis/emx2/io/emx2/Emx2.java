package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;

public class Emx2 {

  public static final String TABLE_NAME = "tableName";
  public static final String COLUMN_NAME = "columnName";
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
  public static final String DEFAULT_VALUE = "defaultValue";
  private static final String VALIDATION = "validation";
  private static final String VISIBLE = "visible";
  private static final String COMPUTED = "computed";
  private static final String SEMANTICS = "semantics";
  private static final String COLUMN_POSITION = "position";
  private static final String TABLE_TYPE = "tableType";

  private Emx2() {
    // hidden
  }

  public static SchemaMetadata fromRowList(Iterable<Row> rows) {
    SchemaMetadata schema = new SchemaMetadata();
    int lineNo = 1; // use as position
    int columnPosition = 0;

    for (Row r : rows) {
      String tableName = r.getString(TABLE_NAME);
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
      if (r.getString(COLUMN_NAME) == null) {
        schema.getTableMetadata(tableName).setInherit(r.getString(TABLE_EXTENDS));
        schema.getTableMetadata(tableName).setImportSchema(r.getString(REF_SCHEMA));
        schema.getTableMetadata(tableName).setSemantics(r.getStringArray(SEMANTICS, false));
        if (r.getString(TABLE_TYPE) != null) {
          schema
              .getTableMetadata(tableName)
              .setTableType(TableType.valueOf(r.getString(TABLE_TYPE)));
        }
        if (r.containsName(LABEL)) schema.getTableMetadata(tableName).setLabel(r.getString(LABEL));
        // labels i18n
        r.getColumnNames().stream()
            .filter(name -> name.startsWith(LABEL + ":"))
            .forEach(
                value -> {
                  schema
                      .getTableMetadata(tableName)
                      .setLabel(r.getString(value, false), (value.split(":")[1]));
                });
        if (r.containsName(DESCRIPTION))
          schema.getTableMetadata(tableName).setDescription(r.getString(DESCRIPTION, false));
        // description i18n
        r.getColumnNames().stream()
            .filter(name -> name.startsWith(DESCRIPTION + ":"))
            .forEach(
                value -> {
                  schema
                      .getTableMetadata(tableName)
                      .setDescription(r.getString(value, false), (value.split(":")[1]));
                });
      }

      // load column metadata
      else {
        try {
          if (r.getString(TABLE_EXTENDS) != null) {
            throw new MolgenisException(
                "Parsing of sheet molgenis failed: Column "
                    + TABLE_EXTENDS
                    + " not supported for columns at "
                    + lineNo);
          }

          Column column = column(r.getString(COLUMN_NAME));
          if (r.notNull(COLUMN_TYPE))
            column.setType(ColumnType.valueOf(r.getString(COLUMN_TYPE).toUpperCase().trim()));
          if (r.notNull(KEY)) column.setKey(r.getInteger(KEY));
          if (r.notNull(REF_SCHEMA)) column.setRefSchema(r.getString(REF_SCHEMA));
          if (r.notNull(REF_TABLE)) column.setRefTable(r.getString(REF_TABLE));
          if (r.notNull(REF_LINK)) column.setRefLink(r.getString(REF_LINK));
          if (r.notNull(REF_BACK)) column.setRefBack(r.getString(REF_BACK));
          if (r.notNull(REQUIRED)) column.setRequired(r.getBoolean(REQUIRED));
          if (r.notNull(DEFAULT_VALUE)) column.setDefaultValue(r.getString(DEFAULT_VALUE));
          if (r.notNull(DESCRIPTION)) column.setDescription(r.getString(DESCRIPTION));
          // description i18n
          r.getColumnNames().stream()
              .filter(name -> name.startsWith(DESCRIPTION + ":"))
              .forEach(
                  value -> {
                    column.setDescription(r.getString(value), (value.split(":")[1]));
                  });
          if (r.notNull(VALIDATION)) column.setValidation(r.getString(VALIDATION));
          if (r.notNull(VISIBLE)) column.setVisible(r.getString(VISIBLE));
          if (r.notNull(COMPUTED)) column.setComputed(r.getString(COMPUTED));
          if (r.notNull(SEMANTICS)) column.setSemantics(r.getStringArray(SEMANTICS));
          if (r.notNull(REF_JS_TEMPLATE)) column.setRefLabel(r.getString(REF_JS_TEMPLATE));
          if (r.notNull(COLUMN_POSITION)) column.setPosition(r.getInteger(COLUMN_POSITION));
          else
            column.setPosition(
                columnPosition++); // this ensures positions accross table hiearchy matches those in
          // imported
          // file
          // get labels
          if (r.notNull(LABEL)) column.setLabel(r.getString(LABEL));
          // labels i18n
          r.getColumnNames().stream()
              .filter(name -> name.startsWith(LABEL + ":"))
              .forEach(
                  value -> {
                    column.setLabel(r.getString(value), (value.split(":")[1]));
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
            REF_SCHEMA,
            REF_TABLE,
            REF_LINK,
            REF_BACK,
            REF_JS_TEMPLATE,
            DEFAULT_VALUE,
            VALIDATION,
            VISIBLE,
            COMPUTED,
            SEMANTICS));
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
    for (TableMetadata t : tables) {

      Row row = new Row();
      // set null columns to ensure sensible order
      row.setString(TABLE_NAME, t.getTableName());
      row.setString(TABLE_EXTENDS, t.getInherit());
      row.setString(TABLE_TYPE, null);
      row.setString(COLUMN_NAME, null);
      row.setString(COLUMN_TYPE, null);
      row.setString(KEY, null);
      row.setString(REQUIRED, null);
      row.setString(DEFAULT_VALUE, null);
      row.setString(REF_SCHEMA, null);
      row.setString(REF_TABLE, null);
      row.setString(REF_LINK, null);
      row.setString(REF_BACK, null);
      row.setString(VALIDATION, null);
      row.setString(VISIBLE, null);
      row.setString(COMPUTED, null);
      if (t.getSemantics() != null) row.setStringArray(SEMANTICS, t.getSemantics());
      for (Map.Entry<String, String> entry : t.getLabels().entrySet()) {
        if (entry.getKey().equals("en")) {
          row.set(LABEL, entry.getValue());
        } else {
          row.set(LABEL + ":" + entry.getKey(), entry.getValue());
        }
      }
      for (Map.Entry<String, String> entry : t.getDescriptions().entrySet()) {
        if (entry.getKey().equals("en")) {
          row.set(DESCRIPTION, entry.getValue());
        } else {
          row.set(DESCRIPTION + ":" + entry.getKey(), entry.getValue());
        }
      }

      result.add(row);
    }

    // output the columns
    for (Column c : columns) {
      if (!c.isSystemColumn()) {
        Row row = new Row();
        row.setString(TABLE_NAME, c.getTableName());
        row.setString(COLUMN_NAME, c.getName());
        if (!c.getColumnType().equals(STRING))
          row.setString(COLUMN_TYPE, c.getColumnType().toString().toLowerCase());
        if (c.isRequired()) row.setBool(REQUIRED, c.isRequired());
        if (c.getDefaultValue() != null) row.setString(DEFAULT_VALUE, c.getDefaultValue());
        if (c.getKey() > 0) row.setInt(KEY, c.getKey());
        if (!c.getRefSchema().equals(c.getSchemaName()))
          row.setString(REF_SCHEMA, c.getRefSchema());
        if (c.getRefTableName() != null) row.setString(REF_TABLE, c.getRefTableName());
        if (c.getRefLink() != null) row.setString(REF_LINK, c.getRefLink());
        if (c.getRefBack() != null) row.setString(REF_BACK, c.getRefBack());
        if (c.getRefLabel() != null) row.setString(REF_JS_TEMPLATE, c.getRefLabel());
        for (Map.Entry<String, String> label : c.getDescriptions().entrySet()) {
          if (label.getKey().equals("en")) {
            row.set(DESCRIPTION, label.getValue());
          } else {
            row.set(DESCRIPTION + ":" + label.getKey(), label.getValue());
          }
        }
        if (c.getValidation() != null) row.set(VALIDATION, c.getValidation());
        if (c.getComputed() != null) row.set(COMPUTED, c.getComputed());
        if (c.getVisible() != null) row.set(VISIBLE, c.getVisible());
        if (c.getSemantics() != null) row.set(SEMANTICS, c.getSemantics());
        for (Map.Entry<String, String> label : c.getLabels().entrySet()) {
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
