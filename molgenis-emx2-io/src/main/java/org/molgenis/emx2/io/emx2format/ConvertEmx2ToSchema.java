package org.molgenis.emx2.io.emx2format;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.utils.MolgenisExceptionMessage;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.ColumnType.*;

public class ConvertEmx2ToSchema {

  protected ConvertEmx2ToSchema() {
    // hides constructor
  }

  public static SchemaMetadata fromCsvFile(File file) throws IOException {
    return fromRowList(CsvRowReader.readList(new FileReader(file)));
  }

  public static SchemaMetadata fromReader(Reader reader) throws IOException {
    return fromRowList(CsvRowReader.readList(reader));
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
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    loadTablesFirst(rows, schema);
    loadColumns(rows, schema, messages);
    loadTableProperties(schema, rows, messages);
    if (!messages.isEmpty()) {
      throw new MolgenisException("molgenis.readers reading failed", messages);
    }
    return schema;
  }

  private static void loadTablesFirst(List<Emx2FileRow> rows, SchemaMetadata schema) {
    for (Emx2FileRow row : rows) {
      String tableName = row.getTable();
      if (!"".equals(tableName)) {
        tableName = tableName.trim();
        schema.createTableIfNotExists(tableName);
      }
    }
  }

  private static void loadTableProperties(
      SchemaMetadata model, List<Emx2FileRow> rows, List<MolgenisExceptionMessage> messages) {
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
      int line, Emx2FileRow row, TableMetadata table, List<MolgenisExceptionMessage> messages) {

    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    for (String term : def.getTerms()) {
      switch (term) {
        case Emx2PropertyList.UNIQUE:
          table.addUnique(def.getParameterArray(term));
          break;
        case Emx2PropertyList.PKEY:
          table.setPrimaryKey(def.getParameterArray(term));
          break;
        default:
          messages.add(new MolgenisExceptionMessage(line, "term " + term + " not supported"));
      }
    }
  }

  private static void loadColumns(
      List<Emx2FileRow> rows, SchemaMetadata model, List<MolgenisExceptionMessage> messages) {
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
              "duplicate_column",
              "Duplicate column definition",
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
      List<MolgenisExceptionMessage> messages) {
    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    ColumnType columnType = getType(def);

    if (REF.equals(columnType)) {
      try {
        String refTable = def.getParameterList(REF.toString().toLowerCase()).get(0);
        String refColumn = def.getParameterList(REF.toString().toLowerCase()).get(1);
        table.addRef(columnName, refTable, refColumn);
      } catch (Exception e) {
        messages.add(
            new MolgenisExceptionMessage(line, "Parsing of 'ref' failed. " + e.getMessage()));
        throw e;
      }
    } else if (REF_ARRAY.equals(columnType)) {
      String refTable = def.getParameterList(REF_ARRAY).get(0);
      String refColumn = def.getParameterList(REF_ARRAY).get(1);
      table.addRefArray(columnName, refTable, refColumn);
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
}
