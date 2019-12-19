package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.MolgenisExceptionDetail;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.*;

public class Emx2 {

  public static final String IMPORT_FAILED = "Import failed";

  private Emx2() {
    // hides constructor
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
      if (!"".equals(tableName)) {
        tableName = tableName.trim();
        schema.createTable(tableName);
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

      if (!"".equals(tableName) && "".equals(columnName.trim())) {

        TableMetadata table = model.getTableMetadata(tableName);
        extractTableDefinition(line, row, table, messages);
      }
    }
  }

  private static void extractTableDefinition(
      int line, Emx2FileRow row, TableMetadata table, List<MolgenisExceptionDetail> messages) {

    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    for (String term : def.getTerms()) {
      switch (term) {
        case Emx2PropertyList.UNIQUE:
          table.addUnique(def.getParameterArray(term));
          break;
        case Emx2PropertyList.PKEY:
          table.setPrimaryKey(def.getParamterValue(term));
          break;
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
        if (params.size() > 1) {
          table.addRef(columnName, params.get(0), params.get(1));
        } else {
          table.addRef(columnName, params.get(0));
        }
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
      table.addRefArray(columnName, refTable, refColumn);
    } else if (REFBACK.equals(columnType)) {
      // should have 2 or 3 parameters
      String refTable = def.getParameterList(REFBACK).get(0);
      String refColumn = null;
      String via = null;
      if (def.getParameterList(REFBACK).size() == 3) {
        refColumn = def.getParameterList(REFBACK).get(1);
        via = def.getParameterList(REFBACK).get(2);
      } else if (def.getParameterList(REFBACK).size() == 2) {
        via = def.getParameterList(REFBACK).get(1);
      } else {
        messages.add(
            new MolgenisExceptionDetail(
                line, "Parsing of 'refback' failed, wrong number of parameters."));
      }
      table.addRefBack(columnName, refTable, refColumn, via);
    } else {
      table.addColumn(columnName, columnType);
    }

    // other properties
    Column column = table.getColumn(columnName);
    if (def.contains(Emx2PropertyList.UNIQUE)) column.setUnique(true);
    if (def.contains(Emx2PropertyList.PKEY)) column.setPrimaryKey(true);
    if (def.contains(Emx2PropertyList.NULLABLE)) column.setNullable(true);
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
    for (String tableName : model.getTableNames()) {
      TableMetadata table = model.getTableMetadata(tableName);
      writeTableDefinitionRow(table, rows);
      for (Column column : table.getColumns()) {
        writeColumnDefinitionRow(column, rows);
      }
    }
    return rows;
  }

  private static void writeTableDefinitionRow(TableMetadata table, List<Emx2FileRow> rows) {

    Emx2PropertyList def = new Emx2PropertyList();
    // write multiple key constraints on table level, otherwise this will be done per column
    for (String[] u : table.getUniques()) {
      if (u.length > 1) def.add(Emx2PropertyList.UNIQUE, u);
    }
    // write table definition row, but only if not empty
    if (!def.getTerms().isEmpty())
      rows.add(new Emx2FileRow(table.getTableName(), "", def.toString()));
  }

  private static void writeColumnDefinitionRow(Column column, List<Emx2FileRow> rows) {

    // ignore internal ID, is implied
    Emx2PropertyList def = new Emx2PropertyList();
    switch (column.getColumnType()) {
      case STRING:
        break;
      case REF:
      case REF_ARRAY:
        if (column.getRefColumnName() != null) {
          def.add(
              column.getColumnType().toString().toLowerCase(),
              column.getRefTableName(),
              column.getRefColumnName());
        } else {
          def.add(column.getColumnType().toString().toLowerCase(), column.getRefTableName());
        }
        break;
      case REFBACK:
        if (column.getRefColumnName() != null) {
          def.add(
              column.getColumnType().toString().toLowerCase(),
              column.getRefTableName(),
              column.getRefColumnName(),
              column.getMappedBy());
        } else {
          def.add(
              column.getColumnType().toString().toLowerCase(),
              column.getRefTableName(),
              column.getMappedBy());
        }
        break;
      default:
        def.add(column.getColumnType().toString().toLowerCase());
    }
    if (Boolean.TRUE.equals(column.getNullable())) def.add("nullable");
    if (Boolean.TRUE.equals(column.getReadonly())) def.add("readonly");
    if (Boolean.TRUE.equals(column.isPrimaryKey())) def.add("pkey");
    if (Boolean.TRUE.equals(column.isUnique())) def.add("unique");
    if (column.getDefaultValue() != null) def.add("default", column.getDefaultValue());

    rows.add(
        new Emx2FileRow(
            column.getTable().getTableName(),
            column.getName(),
            def.toString(),
            column.getDescription()));
  }
}
