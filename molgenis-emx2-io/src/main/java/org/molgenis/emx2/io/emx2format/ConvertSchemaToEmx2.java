package org.molgenis.emx2.io.emx2format;

import org.molgenis.*;
import org.molgenis.Column;
import org.simpleflatmapper.csv.CsvWriter;
import org.simpleflatmapper.util.CheckedConsumer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.molgenis.Row.MOLGENISID;

public class ConvertSchemaToEmx2 {

  private ConvertSchemaToEmx2() {
    // to prevent instantiation of this static method class
  }

  public static void toCsv(Schema model, Writer writer) throws IOException, MolgenisException {

    List<Emx2FileRow> rows = convertModelToMolgenisFileRows(model);

    CsvWriter.CsvWriterDSL<Emx2FileRow> writerDsl = CsvWriter.from(Emx2FileRow.class);
    CsvWriter<Emx2FileRow> csvWriter = writerDsl.to(writer);
    rows.forEach(CheckedConsumer.toConsumer(csvWriter::append));
  }

  public static List<Row> toRowList(Schema model) throws MolgenisException {
    List<Row> result = new ArrayList<>();
    for (Emx2FileRow r : convertModelToMolgenisFileRows(model)) {
      result.add(r.toRow());
    }
    return result;
  }

  private static List<Emx2FileRow> convertModelToMolgenisFileRows(Schema model)
      throws MolgenisException {
    List<Emx2FileRow> rows = new ArrayList<>();
    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      writeTableDefinitionRow(table, rows);
      for (Column column : table.getColumns()) {
        writeColumnDefinitionRow(column, rows);
      }
    }
    return rows;
  }

  private static void writeTableDefinitionRow(Table table, List<Emx2FileRow> rows) {

    Emx2PropertyList def = new Emx2PropertyList();
    for (Unique u : table.getUniques()) {
      def.add("unique", u.getColumnNames());
    }
    if (table.getPrimaryKey().length > 0
        && !Arrays.equals(table.getPrimaryKey(), new String[] {MOLGENISID}))
      def.add("pkey", table.getPrimaryKey());

    if (!def.getTerms().isEmpty()) rows.add(new Emx2FileRow(table.getName(), "", def.toString()));
  }

  private static void writeColumnDefinitionRow(Column column, List<Emx2FileRow> rows)
      throws MolgenisException {

    // ignore internal ID, is implied
    if (!MOLGENISID.equals(column.getName())) {
      Emx2PropertyList def = new Emx2PropertyList();
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
          new Emx2FileRow(
              column.getTable().getName(),
              column.getName(),
              def.toString(),
              column.getDescription()));
    }
  }
}
