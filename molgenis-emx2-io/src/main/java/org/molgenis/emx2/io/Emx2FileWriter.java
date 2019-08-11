package org.molgenis.emx2.io;

import org.molgenis.*;
import org.molgenis.Column;
import org.molgenis.emx2.io.emx2format.MolgenisFileRow;
import org.molgenis.emx2.io.emx2format.MolgenisPropertyList;
import org.simpleflatmapper.csv.CsvWriter;
import org.simpleflatmapper.util.CheckedConsumer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.molgenis.Row.MOLGENISID;

public class Emx2FileWriter {

  private Emx2FileWriter() {
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

    MolgenisPropertyList def = new MolgenisPropertyList();
    for (Unique u : table.getUniques()) {
      def.add("unique", u.getColumnNames());
    }
    if (table.getPrimaryKey().length > 0
        && !Arrays.equals(table.getPrimaryKey(), new String[] {MOLGENISID}))
      def.add("pkey", table.getPrimaryKey());

    if (!def.getTerms().isEmpty())
      rows.add(new MolgenisFileRow(table.getName(), "", def.toString()));
  }

  private static void writeColumnDefinitionRow(Column column, List<MolgenisFileRow> rows)
      throws MolgenisException {

    // ignore internal ID, is implied
    if (!MOLGENISID.equals(column.getName())) {
      MolgenisPropertyList def = new MolgenisPropertyList();
      switch (column.getType()) {
        case STRING:
          break;
        case REF:
        case REF_ARRAY:
          def.add(
              column.getType().toString().toLowerCase(),
              column.getRefTableName(),
              column.getRefColumnName());

          break;
        default:
          def.add(column.getType().toString().toLowerCase());
      }
      if (column.isNullable()) def.add("nullable");
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
