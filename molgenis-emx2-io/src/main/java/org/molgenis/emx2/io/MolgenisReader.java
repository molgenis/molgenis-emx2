package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.*;
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

  public EmxModel readModelFromCsvFile(File f) throws IOException, EmxException {
    return readModelFromCsvReader(new FileReader(f));
  }

  public EmxModel readModelFromCsvReader(Reader in) throws IOException, EmxException {
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

  public EmxModel convertRowsToModel(List<MolgenisFileRow> rows)
      throws MolgenisReaderException, EmxException {
    EmxModel model = new EmxModel();
    List<MolgenisReaderMessage> messages = new ArrayList<>();
    convertRowsToColumns(rows, model, messages);
    convertRowsToTables(rows, model, messages);
    return model;
  }

  private void convertRowsToTables(
      List<MolgenisFileRow> rows, EmxModel model, List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException, EmxException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && "".equals(columnName)) {
        if (model.getTable(tableName) == null) {
          model.addTable(tableName);
        }

        EmxTable table = model.getTable(tableName);
        extractTableDefinition(messages, line, row, table, model);
      }
    }
  }

  private void extractTableDefinition(
      List<MolgenisReaderMessage> messages,
      int line,
      MolgenisFileRow row,
      EmxTable table,
      EmxModel model)
      throws MolgenisReaderException, EmxException {
    List<EmxDefinitionTerm> terms =
        new EmxDefinitionParser().parse(line, messages, row.getDefinition());
    for (EmxDefinitionTerm term : terms) {
      switch (term) {
        case UNIQUE:
          try {
            table.addUnique(term.getParameterList());
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
          table.setExtend(model.getTable(term.getParameterValue()));
          break;
        default:
          throw new MolgenisReaderException(
              "error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private void convertRowsToColumns(
      List<MolgenisFileRow> rows, EmxModel model, List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException, EmxException {
    int line = 1;
    for (MolgenisFileRow row : rows) {
      line++;
      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && !"".equals(columnName)) {
        if (model.getTable(tableName) == null) {
          model.addTable(tableName);
        }
        EmxTable table = model.getTable(tableName);
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
        EmxColumn column = table.addColumn(columnName, getType(terms));
        applyDefintionsToColumn(line, terms, column);
      }
    }
  }

  private void applyDefintionsToColumn(int line, List<EmxDefinitionTerm> terms, EmxColumn column)
      throws EmxException, MolgenisReaderException {
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
          column.setNillable(true);
          break;
        case READONLY:
          column.setReadonly(true);
          break;
        case DEFAULT:
          column.setDefaultValue(term.getParameterValue());
          break;
        case UNIQUE:
          column.setUnique(true);
          break;
        default:
          throw new MolgenisReaderException(
              "error on line" + line + ": unknown definition term " + term);
      }
    }
  }

  private EmxType getType(List<EmxDefinitionTerm> terms) {
    // get type
    EmxType type = EmxType.STRING;
    for (EmxDefinitionTerm term : terms) {
      try {
        type = EmxType.valueOf(term.name());
      } catch (Exception e) {
        // no problem,irrelevant term.
      }
    }
    return type;
  }
}
