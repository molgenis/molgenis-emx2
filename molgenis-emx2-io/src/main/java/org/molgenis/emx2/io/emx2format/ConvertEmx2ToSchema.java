package org.molgenis.emx2.io.emx2format;

import org.molgenis.MolgenisException;
import org.molgenis.MolgenisExceptionMessage;
import org.molgenis.data.Row;
import org.molgenis.emx2.io.csv.CsvRowReader;
import org.molgenis.metadata.ColumnMetadata;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.metadata.Type.*;
import static org.molgenis.emx2.io.emx2format.Emx2PropertyList.NULLABLE;
import static org.molgenis.emx2.io.emx2format.Emx2PropertyList.PKEY;
import static org.molgenis.emx2.io.emx2format.Emx2PropertyList.UNIQUE;

public class ConvertEmx2ToSchema {

  protected ConvertEmx2ToSchema() {
    // hides constructor
  }

  public static void fromCsvFile(SchemaMetadata schema, File file)
      throws IOException, MolgenisException {
    fromRowList(schema, CsvRowReader.readList(new FileReader(file)));
  }

  public static void fromReader(SchemaMetadata schema, Reader reader)
      throws IOException, MolgenisException {
    fromRowList(schema, CsvRowReader.readList(reader));
  }

  public static void fromRowList(SchemaMetadata schema, List<Row> rows) throws MolgenisException {
    List<Emx2FileRow> typedRows = new ArrayList<>();
    for (Row r : rows) {
      typedRows.add(new Emx2FileRow(r));
    }
    executeLoadProcedure(schema, typedRows);
  }

  private static void executeLoadProcedure(SchemaMetadata schema, List<Emx2FileRow> rows)
      throws MolgenisException {
    List<MolgenisExceptionMessage> messages = new ArrayList<>();
    loadTablesFirst(rows, schema);
    loadColumns(rows, schema, messages);
    loadTableProperties(schema, rows, messages);
    if (!messages.isEmpty()) {
      throw new MolgenisException("molgenis.csv reading failed", messages);
    }
  }

  private static void loadTablesFirst(List<Emx2FileRow> rows, SchemaMetadata schema)
      throws MolgenisException {
    for (Emx2FileRow row : rows) {
      String tableName = row.getTable();
      if (!"".equals(tableName)) {
        tableName = tableName.trim();
        schema.createTableIfNotExists(tableName);
      }
    }
  }

  private static void loadTableProperties(
      SchemaMetadata model, List<Emx2FileRow> rows, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
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
      int line, Emx2FileRow row, TableMetadata table, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {

    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    for (String term : def.getTerms()) {
      switch (term) {
        case UNIQUE:
          table.addUnique(def.getParameterArray(term));
          break;
        case PKEY:
          table.setPrimaryKey(def.getParameterArray(term));
          break;
        default:
          messages.add(new MolgenisExceptionMessage(line, "term " + term + " not supported"));
      }
    }
  }

  private static void loadColumns(
      List<Emx2FileRow> rows, SchemaMetadata model, List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
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
      List<MolgenisExceptionMessage> messages)
      throws MolgenisException {
    Emx2PropertyList def = new Emx2PropertyList(row.getProperties());
    Type type = getType(def);

    if (REF.equals(type)) {
      try {
        String refTable = def.getParameterList(REF.toString().toLowerCase()).get(0);
        String refColumn = def.getParameterList(REF.toString().toLowerCase()).get(1);
        table.addRef(columnName, refTable, refColumn);
      } catch (Exception e) {
        messages.add(
            new MolgenisExceptionMessage(line, "Parsing of 'ref' failed. " + e.getMessage()));
        throw e;
      }
    } else if (REF_ARRAY.equals(type)) {
      String refTable = def.getParameterList(REF_ARRAY).get(0);
      String refColumn = def.getParameterList(REF_ARRAY).get(1);
      table.addRefArray(columnName, refTable, refColumn);
    } else {
      table.addColumn(columnName, type);
    }

    // other properties
    ColumnMetadata column = table.getColumn(columnName);
    if (def.contains(NULLABLE)) column.setNullable(true);
  }

  private static Type getType(Emx2PropertyList def) {
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
