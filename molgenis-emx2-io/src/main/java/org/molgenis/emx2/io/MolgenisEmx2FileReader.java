package org.molgenis.emx2.io;

import org.molgenis.*;
import org.molgenis.emx2.io.csv.RowReaderCsv;
import org.molgenis.emx2.io.format.EmxDefinitionList;
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
              r.getString(MolgenisFileHeader.DEFINITION.toString().toLowerCase())));
    }
    load(schema, rows);
  }

  private static void load(Schema schema, List<MolgenisFileRow> rows) throws MolgenisException {
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    loadTablesFirst(rows, schema);
    loadColumns(rows, schema, messages);
    loadTables(schema, rows, messages);
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

  private static void loadTables(
      Schema model, List<MolgenisFileRow> rows, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;

      String tableName = row.getTable().trim();
      String columnName = row.getColumn().trim();

      if (tableName != null
          && !"".equals(tableName)
          && (columnName == null || "".equals(columnName.trim()))) {

        Table table = model.getTable(tableName);
        extractTableDefinition(line, row, table, messages);
      }
    }
  }

  private static void extractTableDefinition(
      int line, MolgenisFileRow row, Table table, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {

    EmxDefinitionList def = new EmxDefinitionList(row.getDefinition());
    for (String term : def.getTerms()) {
      switch (term) {
        case "unique":
          List<String> uniques = def.getParameterList(term);
          table.addUnique(uniques.toArray(new String[uniques.size()]));
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
          EmxDefinitionList def = new EmxDefinitionList(row.getDefinition());
          Type type = getType(def);
          Column column = null;
          String refTable = "";
          String refColumn = "";
          if (REF.equals(type)) {
            column = table.addRef(columnName, refTable, refColumn);
          } else if (REF_ARRAY.equals(type)) {
            column = table.addRefArray(columnName, refTable, refColumn);
          } else {
            column = table.addColumn(columnName, type);
          }
          applyDefinitionsToColumn(line, def, column);
        }
      }
    }
  }

  private static void applyDefinitionsToColumn(int line, EmxDefinitionList def, Column column) {}

  private static Type getType(EmxDefinitionList def) {
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
