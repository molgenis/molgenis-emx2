package org.molgenis.emx2.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.molgenis.*;
import org.molgenis.emx2.io.format.EmxDefinitionList;
import org.molgenis.emx2.io.format.MolgenisFileRow;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.STRING;

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
      EmxDefinitionList def = new EmxDefinitionList();
      for (Unique u : table.getUniques()) {
        def.add("unique", u.getColumnNames());
      }
      rows.add(new MolgenisFileRow(table.getName(), "", def.toString()));
    }
  }

  private static void writeColumnDefinitionRow(Column column, List<MolgenisFileRow> rows)
      throws MolgenisException {

    // ignore internal ID, is implied
    if (!MOLGENISID.equals(column.getName())) {
      EmxDefinitionList def = new EmxDefinitionList();
      if (!STRING.equals(column.getType())) def.add(column.getType().toString().toLowerCase());
      if (column.isNullable()) def.add("nillable");
      if (column.isReadonly()) def.add("readonly");
      if (column.getDefaultValue() != null) def.add("default", column.getDefaultValue());
      if (column.isUnique()) def.add("unique");

      rows.add(
          new MolgenisFileRow(
              column.getTable().getName(),
              column.getName(),
              def.toString(),
              column.getDescription()));
    }
  }
}
