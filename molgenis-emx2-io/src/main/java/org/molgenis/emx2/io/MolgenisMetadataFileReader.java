package org.molgenis.emx2.io;

import org.molgenis.*;
import org.molgenis.emx2.io.format.EmxDefinitionParser;
import org.molgenis.emx2.io.format.EmxDefinitionTerm;
import org.molgenis.emx2.io.format.MolgenisFileHeader;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.Column.Type.*;

public class MolgenisMetadataFileReader {

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

  public static void load(Schema schema, List<MolgenisFileRow> rows) throws MolgenisException {
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    convertRowsToColumns(rows, schema, messages);
    convertRowsToTables(rows, schema, messages);
    if (messages.size() > 0) {
      throw new MolgenisException("molgenis.csv reading failed", messages);
    }
  }

  private static void convertRowsToTables(
      List<MolgenisFileRow> rows, Schema model, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (tableName != null
          && !"".equals(tableName)
          && (columnName == null || "".equals(columnName.trim()))) {
        tableName = tableName.trim();
        if (model.getTable(tableName) == null) {
          model.createTable(tableName);
        }

        Table table = model.getTable(tableName);
        extractTableDefinition(messages, line, row, table, model);
      }
    }
  }

  private static void extractTableDefinition(
      List<MolgenisExceptionMessage> messages,
      int line,
      MolgenisFileRow row,
      Table table,
      Schema model)
      throws MolgenisException {
    List<EmxDefinitionTerm> terms =
        new EmxDefinitionParser().parse(line, messages, row.getDefinition());
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
        case UNIQUE:
          try {
            List<String> uniques = term.getParameterList();
            table.addUnique(uniques.toArray(new String[uniques.size()]));
          } catch (Exception e) {
            throw new MolgenisException(
                "error on line "
                    + line
                    + ": unique parsing in definition '"
                    + row.getDefinition()
                    + "' failed. "
                    + e.getMessage());
          }
          break;
        default:
          throw new MolgenisException("error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private static void convertRowsToColumns(
      List<MolgenisFileRow> rows, Schema model, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (tableName != null
          && !"".equals(tableName.trim())
          && columnName != null
          && !"".equals(columnName.trim())) {
        tableName = tableName.trim();
        columnName = columnName.trim();
        try {
          model.getTable(tableName);
        } catch (Exception e) {
          model.createTable(tableName);
        }
        Table table = model.getTable(tableName);
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
          List<EmxDefinitionTerm> terms =
              new EmxDefinitionParser().parse(line, messages, row.getDefinition());
          Column.Type type = getType(terms);
          Column column = table.addColumn(columnName, type);
          for (EmxDefinitionTerm t : terms) {
            if (EmxDefinitionTerm.REF.equals(t)) column.setRefTable(t.getParameterValue());
          }
          applyDefinitionsToColumn(line, terms, column);
        }
      }
    }
  }

  private static void applyDefinitionsToColumn(
      int line, List<EmxDefinitionTerm> terms, Column column) throws MolgenisException {
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
          // ignore the types
        case STRING:
        case INT:
        case BOOL:
        case DECIMAL:
        case TEXT:
        case DATE:
        case DATETIME:
        case UUID:
        case REF:
        case MREF:
          break;
        case NILLABLE:
          column.setNullable(true);
          break;
        case UNIQUE:
          column.getTable().addUnique(column.getName());
          break;
        case READONLY:
          column.setReadonly(true);
          break;
        case DEFAULT:
          column.setDefaultValue(term.getParameterValue());
          break;
        default:
          throw new MolgenisException("error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private static Column.Type getType(List<EmxDefinitionTerm> terms) {
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
        case STRING:
          return STRING;
        case INT:
          return INT;
        case BOOL:
          return BOOL;
        case DECIMAL:
          return DECIMAL;
        case TEXT:
          return TEXT;
        case DATE:
          return DATE;
        case DATETIME:
          return DATETIME;
        case UUID:
          return UUID;
        case REF:
          return REF;
        case MREF:
          return MREF;
      }
    }
    return STRING;
  }
}
