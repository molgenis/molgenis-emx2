package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.Column;
import org.molgenis.DatabaseException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.bean.SchemaBean;
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

public class MolgenisReader {

  public Schema readModelFromCsvFile(File f) throws IOException, DatabaseException {
    return readModelFromCsvReader(new FileReader(f));
  }

  public Schema readModelFromCsvReader(Reader in) throws IOException, DatabaseException {
    List<MolgenisFileRow> rows = readRowsFromCsvReader(in);
    return convertRowsToModel(rows);
  }

  public List<MolgenisFileRow> readRowsFromCsvFile(File f) throws IOException {
    return readRowsFromCsvReader(new FileReader(f));
  }

  public List<MolgenisFileRow> readRowsFromCsvReader(Reader in) throws MolgenisReaderException {
    try {
      Iterable<CSVRecord> records =
          CSVFormat.DEFAULT
              .withHeader(MolgenisFileHeader.class)
              .withFirstRecordAsHeader()
              .withIgnoreHeaderCase()
              .withIgnoreSurroundingSpaces()
              .withTrim()
              .withIgnoreEmptyLines(true)
              .parse(in);
      List<MolgenisFileRow> rows = new ArrayList<>();
      for (CSVRecord record : records) {
        rows.add(
            new MolgenisFileRow(
                record.get(MolgenisFileHeader.TABLE),
                record.get(MolgenisFileHeader.COLUMN),
                record.get(MolgenisFileHeader.DEFINITION)));
      }
      return rows;
    } catch (IOException e) {
      throw new MolgenisReaderException(e.getMessage());
    }
  }

  public Schema convertRowsToModel(List<MolgenisFileRow> rows)
      throws MolgenisReaderException, DatabaseException {
    Schema model = new SchemaBean();
    List<MolgenisReaderMessage> messages = new ArrayList<>();
    convertRowsToColumns(rows, model, messages);
    convertRowsToTables(rows, model, messages);
    return model;
  }

  private void convertRowsToTables(
      List<MolgenisFileRow> rows, Schema model, List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException, DatabaseException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && "".equals(columnName)) {
        if (model.getTable(tableName) == null) {
          model.createTable(tableName);
        }

        Table table = model.getTable(tableName);
        extractTableDefinition(messages, line, row, table, model);
      }
    }
  }

  private void extractTableDefinition(
      List<MolgenisReaderMessage> messages,
      int line,
      MolgenisFileRow row,
      Table table,
      Schema model)
      throws MolgenisReaderException {
    List<EmxDefinitionTerm> terms =
        new EmxDefinitionParser().parse(line, messages, row.getDefinition());
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
        case UNIQUE:
          try {
            List<String> uniques = term.getParameterList();
            table.addUnique(uniques.toArray(new String[uniques.size()]));
          } catch (Exception e) {
            throw new MolgenisReaderException(
                "error on line "
                    + line
                    + ": unique parsing in definition '"
                    + row.getDefinition()
                    + "' failed. "
                    + e.getMessage());
          }
          break;
        case EXTENDS:
          // TODO
          // table.setExtend(model.getTable(term.getParameterValue()));
          break;
        default:
          throw new MolgenisReaderException(
              "error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private void convertRowsToColumns(
      List<MolgenisFileRow> rows, Schema model, List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException, DatabaseException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && !"".equals(columnName)) {
        if (model.getTable(tableName) == null) {
          model.createTable(tableName);
        }
        Table table = model.getTable(tableName);
        if (table.getColumn(columnName) != null) {
          throw new MolgenisReaderException(
              "error on line "
                  + line
                  + ": duplicate column definition table='"
                  + tableName
                  + "', column='"
                  + columnName
                  + "'");
        }
        List<EmxDefinitionTerm> terms =
            new EmxDefinitionParser().parse(line, messages, row.getDefinition());
        Column column = table.addColumn(columnName, getType(terms));
        applyDefintionsToColumn(line, terms, column);
      }
    }
  }

  private void applyDefintionsToColumn(int line, List<EmxDefinitionTerm> terms, Column column)
      throws MolgenisReaderException, DatabaseException {
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
        case STRING:
        case INT:
        case LONG:
        case SELECT:
        case RADIO:
        case BOOL:
        case DECIMAL:
        case TEXT:
        case DATE:
        case DATETIME:
        case MSELECT:
        case CHECKBOX:
        case UUID:
        case HYPERLINK:
        case EMAIL:
        case HTML:
        case FILE:
        case ENUM:
          break;
        case NILLABLE:
          column.setNullable(true);
          break;
        case READONLY:
          // column.setReadonly(true);
          break;
        case DEFAULT:
          // column.setDefaultValue(term.getParameterValue());
          break;
        case UNIQUE:
          column.getTable().addUnique(column.getName());
          break;
        default:
          throw new MolgenisReaderException(
              "error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private Column.Type getType(List<EmxDefinitionTerm> terms) {
    // get type
    Column.Type type = Column.Type.STRING;
    for (EmxDefinitionTerm term : terms) {
      try {
        type = Column.Type.valueOf(term.name());
      } catch (Exception e) {
        // no problem,irrelevant term.
      }
    }
    return type;
  }
}
