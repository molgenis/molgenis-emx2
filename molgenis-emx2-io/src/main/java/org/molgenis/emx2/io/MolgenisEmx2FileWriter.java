package org.molgenis.emx2.io;

import org.molgenis.*;
import org.molgenis.Column;
import org.molgenis.emx2.io.format.MolgenisFileRow;
import org.molgenis.emx2.io.format.MolgenisPropertyList;
import org.simpleflatmapper.csv.CsvWriter;
import org.simpleflatmapper.util.CheckedConsumer;

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

    List<MolgenisFileRow> rows = convertModelToMolgenisFileRows(model);

    CsvWriter.CsvWriterDSL<MolgenisFileRow> writerDsl = CsvWriter.from(MolgenisFileRow.class);
    CsvWriter<MolgenisFileRow> csvWriter = writerDsl.to(writer);
    rows.forEach(CheckedConsumer.toConsumer(csvWriter::append));
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
      MolgenisPropertyList def = new MolgenisPropertyList();
      for (Unique u : table.getUniques()) {
        def.add("unique", u.getColumnNames());
      }
      rows.add(new MolgenisFileRow(table.getName(), "", def.toString()));
    }
  }

  private static void writeColumnDefinitionRow(Column column, List<MolgenisFileRow> rows) {

    // ignore internal ID, is implied
    if (!MOLGENISID.equals(column.getName())) {
      MolgenisPropertyList def = new MolgenisPropertyList();
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
