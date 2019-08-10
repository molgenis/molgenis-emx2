package org.molgenis.emx2.io;

import org.molgenis.*;
import org.molgenis.emx2.io.csv.RowReaderCsv;
import org.molgenis.emx2.io.format.MolgenisPropertyList;
import org.molgenis.emx2.io.format.MolgenisFileHeader;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.Type.*;

public class MolgenisEmx2FileReader {

  protected MolgenisEmx2FileReader() {
    // hides constructor
  }

  public static void load(Schema schema, File file) throws IOException, MolgenisException {
    load(schema, new FileReader(file));
  }

  public static void load(Schema schema, Reader reader) throws IOException, MolgenisException {
    List<MolgenisFileRow> rows = new ArrayList<>();
    for (Row r : RowReaderCsv.read(reader)) {
      rows.add(
          new MolgenisFileRow(
              r.getString(MolgenisFileHeader.TABLE.toString().toLowerCase()),
              r.getString(MolgenisFileHeader.COLUMN.toString().toLowerCase()),
              r.getString(MolgenisFileHeader.PROPERTIES.toString().toLowerCase())));
    }
    load(schema, rows);
  }

  private static void load(Schema schema, List<MolgenisFileRow> rows) throws MolgenisException {
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    loadTablesFirst(rows, schema);
    loadColumns(rows, schema, messages);
    loadTableProperties(schema, rows, messages);
    if (!messages.isEmpty()) {
      throw new MolgenisException("molgenis.csv reading failed", messages);
    }
  }

  private static void loadTablesFirst(List<MolgenisFileRow> rows, Schema schema)
      throws MolgenisException {
    for (MolgenisFileRow row : rows) {
      String tableName = row.getTable();
      if (!"".equals(tableName)) {
        tableName = tableName.trim();
        schema.createTableIfNotExists(tableName);
      }
    }
  }

  private static void loadTableProperties(
      Schema model, List<MolgenisFileRow> rows, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;

      String tableName = row.getTable().trim();
      String columnName = row.getColumn().trim();

      if (!"".equals(tableName) && "".equals(columnName.trim())) {

        Table table = model.getTable(tableName);
        extractTableDefinition(line, row, table, messages);
      }
    }
  }

  private static void extractTableDefinition(
      int line, MolgenisFileRow row, Table table, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {

    MolgenisPropertyList def = new MolgenisPropertyList(row.getProperties());
    for (String term : def.getTerms()) {
      switch (term) {
        case "unique":
          table.addUnique(def.getParameterArray(term));
          break;
        case "pkey":
          table.setPrimaryKey(def.getParameterArray(term));
          break;
        default:
          messages.add(new MolgenisExceptionMessage(line, "term " + term + " not supported"));
      }
    }
  }

  private static void loadColumns(
      List<MolgenisFileRow> rows, Schema model, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && !"".equals(columnName)) {
        Table table = model.createTableIfNotExists(tableName);
        try {
          table.getColumn(columnName);
          throw new MolgenisException(
              "error on line "
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
      MolgenisFileRow row,
      String columnName,
      Table table,
      int line,
      List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    MolgenisPropertyList def = new MolgenisPropertyList(row.getProperties());
    Type type = getType(def);

    if (REF.equals(type)) {
      try {
        String refTable = def.getParameterList("ref").get(0);
        String refColumn = def.getParameterList("ref").get(1);
        table.addRef(columnName, refTable, refColumn);
      } catch (Exception e) {
        messages.add(
            new MolgenisExceptionMessage(line, "Parsing of 'ref' failed. " + e.getMessage()));
        throw e;
      }
    } else if (REF_ARRAY.equals(type)) {
      String refTable = def.getParameterList("ref_array").get(0);
      String refColumn = def.getParameterList("ref_array").get(1);
      table.addRefArray(columnName, refTable, refColumn);
    } else {
      table.addColumn(columnName, type);
    }

    // other properties
    Column column = table.getColumn(columnName);
    if (def.contains("nullable")) column.nullable(true);
  }

  private static Type getType(MolgenisPropertyList def) {
    Type type = null;
    for (String term : def.getTerms()) {
      try {
        type = Type.valueOf(term.toUpperCase());
      } catch (Exception e) {
        // also okay.
      }
    }
    return type != null ? type : STRING;
  }
}
