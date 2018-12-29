package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.io.beans.EmxColumnBean;
import org.molgenis.emx2.io.beans.EmxModelBean;
import org.molgenis.emx2.io.beans.EmxTableBean;
import org.molgenis.emx2.io.format.EmxDefinitionParser;
import org.molgenis.emx2.io.format.EmxDefinitionTerm;
import org.molgenis.emx2.io.format.MolgenisFileHeader;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class MolgenisReader {

  public EmxModel readModelFromCsv(File f) throws IOException {
    return readModelFromCsv(new FileReader(f));
  }

  public EmxModel readModelFromCsv(Reader in) throws IOException {
    List<MolgenisFileRow> rows = readRowsFromCsv(in);
    return convertRowsToModel(rows);
  }

  public List<MolgenisFileRow> readRowsFromCsv(File f) throws IOException {
    return readRowsFromCsv(new FileReader(f));
  }

  public List<MolgenisFileRow> readRowsFromCsv(Reader in) throws MolgenisReaderException {
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

  public EmxModel convertRowsToModel(List<MolgenisFileRow> rows) throws MolgenisReaderException {

    Map<String, EmxTableBean> tables = new LinkedHashMap<>();
    List<MolgenisReaderMessage> messages = new ArrayList<>();
    convertRowsToColumns(rows, tables, messages);
    convertRowsToTables(rows, tables, messages);

    return new EmxModelBean(tables);
  }

  private void convertRowsToTables(
      List<MolgenisFileRow> rows,
      Map<String, EmxTableBean> tables,
      List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException {
    int line = 0;
    for (MolgenisFileRow row : rows) {
      line++;

      String tableName = row.getTable();
      String columnName = row.getColumn();
      if (!"".equals(tableName) && "".equals(columnName)) {
        tables.computeIfAbsent(tableName, k -> new EmxTableBean(tableName));
        EmxTableBean table = tables.get(tableName);
        List<EmxDefinitionTerm> terms =
            new EmxDefinitionParser().parse(line, messages, row.getDefinition());
        for (EmxDefinitionTerm term : terms) {
          switch (term) {
            case UNIQUE:
              try {
                table.addUnique(Arrays.asList(term.getParameterValue().split(",")));
              } catch (Exception e) {
                throw new MolgenisReaderException(
                    "error on line " + line + ": unique parsing failed. " + e.getMessage());
              }
          }
        }
      }
    }
  }

  private void convertRowsToColumns(
      List<MolgenisFileRow> rows,
      Map<String, EmxTableBean> tables,
      List<MolgenisReaderMessage> messages)
      throws MolgenisReaderException {
    int line = 0;
    for (MolgenisFileRow row : rows) {
      line++;

      String tableName = row.getTable();
      String columnName = row.getColumn();

      if (!"".equals(tableName) && !"".equals(columnName)) {
        tables.computeIfAbsent(tableName, k -> new EmxTableBean(tableName));
        EmxTableBean table = tables.get(tableName);

        // if table != null and column== null, this definition is for a table
        // if table != null and column != null, this definition if for a column
        if (columnName != null) {
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
          EmxColumnBean column = table.addColumn(columnName, getType(terms));
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
              default:
                throw new MolgenisReaderException(
                    "error on line" + line + ": unknown definition term " + term);
            }
          }
        }
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
