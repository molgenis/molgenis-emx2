package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.utils.MolgenisExceptionDetail;
import org.molgenis.emx2.utils.TypeUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.io.readers.RowReaderJackson.read;

public class Emx2 {

  public static final String IMPORT_FAILED = "Import failed";

  private Emx2() {
    // hides constructor
  }

  public static SchemaMetadata loadEmx2File(File file, Character separator) throws IOException {
    List<Emx2FileRow> typedRows = new ArrayList<>();
    for (Row r : read(file, separator)) {
      typedRows.add(new Emx2FileRow(r));
    }
    return executeLoadProcedure(typedRows);
  }

  public static SchemaMetadata fromRowList(List<Row> rows) {
    List<Emx2FileRow> typedRows = new ArrayList<>();
    for (Row r : rows) {
      typedRows.add(new Emx2FileRow(r));
    }
    return executeLoadProcedure(typedRows);
  }

  private static SchemaMetadata executeLoadProcedure(List<Emx2FileRow> rows) {
    SchemaMetadata schema = new SchemaMetadata();
    List<MolgenisExceptionDetail> messages = new ArrayList<>();
    loadTablesFirst(rows, schema);
    loadColumns(rows, schema, messages);
    loadTableProperties(schema, rows, messages);
    if (!messages.isEmpty()) {
      throw new MolgenisException(IMPORT_FAILED, "molgenis.csv reading failed", messages);
    }
    return schema;
  }

  private static void loadTablesFirst(List<Emx2FileRow> rows, SchemaMetadata schema) {
    for (Emx2FileRow row : rows) {
      String tableName = row.getTable();
      if (!"".equals(tableName) && schema.getTableMetadata(tableName) == null) {
        tableName = tableName.trim();
        schema.create(table(tableName));
      }
    }
  }

  private static void loadTableProperties(
      SchemaMetadata model, List<Emx2FileRow> rows, List<MolgenisExceptionDetail> messages) {
    int line = 1;
    for (Emx2FileRow row : rows) {
      line++;

      String tableName = row.getTable().trim();
      String columnName = row.getColumn().trim();
      String description = row.getDescription().trim();

      if (!"".equals(tableName) && "".equals(columnName.trim())) {
        TableMetadata table = model.getTableMetadata(tableName);
        table.setDescription(description);
        extractTableDefinition(line, row, table, messages);
      }
    }
  }

  private static void extractTableDefinition(
      int line, Emx2FileRow row, TableMetadata table, List<MolgenisExceptionDetail> messages) {

    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    for (String term : def.getTerms()) {
      switch (term) {
        default:
          messages.add(new MolgenisExceptionDetail(line, "term " + term + " not supported"));
      }
    }
  }

  private static void loadColumns(
      List<Emx2FileRow> rows, SchemaMetadata model, List<MolgenisExceptionDetail> messages) {
    int line = 1;
    for (Emx2FileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && !"".equals(columnName)) {
        TableMetadata table = model.getTableMetadata(tableName);
        try {
          table.getColumn(columnName);
          throw new MolgenisException(
              IMPORT_FAILED,
              "Error on line "
                  + line
                  + ": duplicate column definition table='"
                  + tableName
                  + "', column='"
                  + columnName
                  + "'");
        } catch (Exception e) {
          loadColumn(row, columnName, table, line, messages);
        }
      }
    }
  }

  private static void loadColumn(
      Emx2FileRow row,
      String columnName,
      TableMetadata table,
      int line,
      List<MolgenisExceptionDetail> messages) {
    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    ColumnType columnType = getType(def);

    if (REF.equals(columnType)) {
      try {
        List<String> params = def.getParameterList(REF.toString().toLowerCase());
        if (params.isEmpty() || params.size() > 2) {
          throw new MolgenisException(IMPORT_FAILED, "Ref must have 1 or 2 parameter values");
        }
        table.add(column(columnName).type(REF).refTable(params.get(0)));
      } catch (Exception e) {
        messages.add(
            new MolgenisExceptionDetail(line, "Parsing of 'ref' failed. " + e.getMessage()));
        throw e;
      }
    } else if (REF_ARRAY.equals(columnType)) {
      String refTable = def.getParameterList(REF_ARRAY).get(0);
      String refColumn = null;
      if (def.getParameterList(REF_ARRAY).size() > 1) {
        refColumn = def.getParameterList(REF_ARRAY).get(1);
      }
      table.add(column(columnName).type(REF_ARRAY).refTable(refTable));
    } else if (REFBACK.equals(columnType)) {
      // should have 2 or 3 parameters
      String refTable = def.getParameterList(REFBACK).get(0);
      String refColumn = null;
      String mappedBy = null;
      if (def.getParameterList(REFBACK).size() == 3) {
        refColumn = def.getParameterList(REFBACK).get(1);
        mappedBy = def.getParameterList(REFBACK).get(2);
      } else if (def.getParameterList(REFBACK).size() == 2) {
        mappedBy = def.getParameterList(REFBACK).get(1);
      } else {
        messages.add(
            new MolgenisExceptionDetail(
                line, "Parsing of 'refback' failed, wrong number of parameters."));
      }
      table.add(column(columnName).type(REFBACK).refTable(refTable).mappedBy(mappedBy));
    } else {
      table.add(column(columnName).type(columnType));
    }

    // other properties
    Column column = table.getColumn(columnName);
    if (def.contains(Emx2PropertyList.KEY)) {
      column.key(TypeUtils.toInt(def.getParamterValue(Emx2PropertyList.KEY)));
    }
    if (def.contains(Emx2PropertyList.NULLABLE)) {
      column.nullable(true);
    }
    if (def.contains(Emx2PropertyList.CASCADE_DELETE)) {
      column.cascadeDelete(true);
    }
    if (def.contains(Emx2PropertyList.VALIDATE)) {
      column.validation(def.getParamterValue(Emx2PropertyList.VALIDATE));
    }
    if (row.getDescription() != null && !row.getDescription().equals("")) {
      column.setDescription(row.getDescription());
    }
    table.alterColumn(column);
  }

  private static ColumnType getType(Emx2PropertyList def) {
    ColumnType columnType = null;
    for (String term : def.getTerms()) {
      try {
        columnType = ColumnType.valueOf(term.toUpperCase());
      } catch (Exception e) {
        // also okay.
      }
    }
    return columnType != null ? columnType : STRING;
  }

  public static void toCsv(SchemaMetadata model, Writer writer, Character separator)
      throws IOException {
    CsvTableWriter.rowsToCsv(toRowList(model), writer, separator);
  }

  public static List<Row> toRowList(SchemaMetadata model) {
    List<Row> result = new ArrayList<>();
    for (Emx2FileRow r : convertModelToMolgenisFileRows(model)) {
      result.add(r.toRow());
    }
    return result;
  }

  private static List<Emx2FileRow> convertModelToMolgenisFileRows(SchemaMetadata model) {
    List<Emx2FileRow> rows = new ArrayList<>();
    List<String> tableNames = new ArrayList();
    // deterministic order (TODO make user define order)
    tableNames.addAll(model.getTableNames());
    Collections.sort(tableNames);
    for (String tableName : tableNames) {
      TableMetadata table = model.getTableMetadata(tableName);
      writeTableDefinitionRow(table, rows);
      List<String> columnNames = new ArrayList(table.getColumnNames());
      // deterministic order (TODO make user define order)
      Collections.sort(columnNames);
      for (String column : columnNames) {
        writeColumnDefinitionRow(table.getColumn(column), rows);
      }
    }
    return rows;
  }

  private static void writeTableDefinitionRow(TableMetadata table, List<Emx2FileRow> rows) {

    Emx2PropertyList def = new Emx2PropertyList();
    // write table definition row, but only if not empty
    if (!def.getTerms().isEmpty() || table.getDescription() != null)
      rows.add(new Emx2FileRow(table.getTableName(), "", def.toString(), table.getDescription()));
  }

  private static void writeColumnDefinitionRow(Column column, List<Emx2FileRow> rows) {

    // ignore internal ID, is implied
    Emx2PropertyList def = new Emx2PropertyList();
    switch (column.getColumnType()) {
      case STRING:
        break;
      case REF:
      case REF_ARRAY:
      case MREF:
        def.add(column.getColumnType().toString().toLowerCase(), column.getRefTableName());
        break;
      case REFBACK:
        def.add(
            column.getColumnType().toString().toLowerCase(),
            column.getRefTableName(),
            column.getMappedBy());
        break;
      default:
        def.add(column.getColumnType().toString().toLowerCase());
    }
    if (Boolean.TRUE.equals(column.isNullable())) def.add("nullable");
    if (Boolean.TRUE.equals(column.isReadonly())) def.add("readonly");
    if (Boolean.TRUE.equals(column.isCascadeDelete())) def.add("cascadeDelete");
    if (column.getKey() > 0) def.add("key", "" + column.getKey());
    if (column.getValidation() != null) def.add(Emx2PropertyList.VALIDATE, column.getValidation());
    if (column.getDefaultValue() != null) def.add("default", column.getDefaultValue());

    rows.add(
        new Emx2FileRow(
            column.getTable().getTableName(),
            column.getName(),
            def.toString(),
            column.getDescription()));
  }
}
