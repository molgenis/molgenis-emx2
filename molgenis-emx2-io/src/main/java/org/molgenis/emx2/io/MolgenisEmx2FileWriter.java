package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.molgenis.*;
import org.molgenis.emx2.io.format.EmxDefinitionTerm;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.emx2.io.format.EmxDefinitionTerm.*;

public class MolgenisEmx2FileWriter {

  private MolgenisEmx2FileWriter() {
    // to prevent instantiation of this static method class
  }

  public static void writeCsv(Schema model, Writer writer) throws IOException, MolgenisException {
    CSVPrinter csvPrinter =
        new CSVPrinter(
            writer, CSVFormat.DEFAULT.withHeader("table", "column", "definition", "description"));
    for (MolgenisFileRow row : convertModelToMolgenisFileRows(model)) {
      csvPrinter.printRecord(
          row.getTable(), row.getColumn(), row.getDefinition(), row.getDescription());
    }
    csvPrinter.close();
  }

  public static List<MolgenisFileRow> convertModelToMolgenisFileRows(Schema model)
      throws MolgenisException {
    List<MolgenisFileRow> rows = new ArrayList<>();
    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      writeTableDefinitionRow(table, rows);
      for (Column column : table.getColumns()) {
        writeColumnDefinitionRow(column, rows);
      }
    }
    return rows;
  }

  private static void writeTableDefinitionRow(Table table, List<MolgenisFileRow> rows) {
    if (!table.getUniques().isEmpty()) {
      List<EmxDefinitionTerm> def = new ArrayList<>();
      for (Unique u : table.getUniques()) {
        def.add(UNIQUE.setParameterList(u.getColumnNames()));
      }
      rows.add(new MolgenisFileRow(table.getName(), "", join(def, " ")));
    }
  }

  private static void writeColumnDefinitionRow(Column column, List<MolgenisFileRow> rows)
      throws MolgenisException {
    // ignore internal ID
    if (!MOLGENISID.equals(column.getName())) {
      rows.add(
          new MolgenisFileRow(
              column.getTable().getName(),
              column.getName(),
              getColumnDefinitionString(column),
              column.getDescription()));
    }
  }

  private static String getColumnDefinitionString(Column col) throws MolgenisException {
    List<EmxDefinitionTerm> def = new ArrayList<>();

    if (!Type.STRING.equals(col.getType())) {
      EmxDefinitionTerm d = EmxDefinitionTerm.valueOf(col.getType());
      switch (d) {
        case STRING:
          break;
        case INT:
          break;
        case BOOL:
          break;
        case DECIMAL:
          break;
        case TEXT:
          break;
        case DATE:
          break;
        case DATETIME:
          break;
        case NILLABLE:
          break;
        case UNIQUE:
          break;
        case UUID:
          break;
        case REF:
          d.setParameterValue(col.getRefTableName());
          break;
        case MREF:
          break;
        default:
          throw new MolgenisException("unknown type " + d + " this is a coding error!");
      }

      def.add(d);
    }
    if (col.isNullable()) def.add(NILLABLE);
    if (col.isReadonly()) def.add(READONLY);
    if (col.getDefaultValue() != null) def.add(DEFAULT.setParameterValue(col.getDefaultValue()));
    if (col.isUnique()) def.add(UNIQUE.setParameterValue(null));

    return join(def, " ");
  }

  private static String join(Collection collection, String delimiter) {
    return (String)
        collection.stream().map(Object::toString).collect(Collectors.joining(delimiter));
  }
}
