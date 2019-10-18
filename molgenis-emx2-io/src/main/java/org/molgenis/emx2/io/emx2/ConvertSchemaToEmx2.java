package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.io.readers.CsvRowWriter;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class ConvertSchemaToEmx2 {

  private ConvertSchemaToEmx2() {
    // to prevent instantiation of this static method class
  }

  public static void toCsv(SchemaMetadata model, Writer writer, Character separator)
      throws IOException {
    CsvRowWriter.writeCsv(toRowList(model), writer, separator);
  }

  public static List<Row> toRowList(SchemaMetadata model) {
    List<Row> result = new ArrayList<>();
    for (Emx2FileRow r : convertModelToMolgenisFileRows(model)) {
      result.add(r.toRow());
    }
    return result;
  }

  private static List<Emx2FileRow> convertModelToMolgenisFileRows(SchemaMetadata model) {
    List<Emx2FileRow> rows = new ArrayList<>();
    for (String tableName : model.getTableNames()) {
      TableMetadata table = model.getTableMetadata(tableName);
      writeTableDefinitionRow(table, rows);
      for (Column column : table.getColumns()) {
        writeColumnDefinitionRow(column, rows);
      }
    }
    return rows;
  }

  private static void writeTableDefinitionRow(TableMetadata table, List<Emx2FileRow> rows) {

    Emx2PropertyList def = new Emx2PropertyList();
    // write multiple key constraints on table level, otherwise this will be done per column
    for (String[] u : table.getUniques()) {
      if (u.length > 1) def.add(Emx2PropertyList.UNIQUE, u);
    }
    if (table.getPrimaryKey().length > 1) {
      def.add(Emx2PropertyList.PKEY, table.getPrimaryKey());
    }
    // write table definition row, but only if not empty
    if (!def.getTerms().isEmpty())
      rows.add(new Emx2FileRow(table.getTableName(), "", def.toString()));
  }

  private static void writeColumnDefinitionRow(Column column, List<Emx2FileRow> rows) {

    // ignore internal ID, is implied
    Emx2PropertyList def = new Emx2PropertyList();
    switch (column.getColumnType()) {
      case STRING:
        break;
      case REF:
      case REF_ARRAY:
        def.add(
            column.getColumnType().toString().toLowerCase(),
            column.getRefTableName(),
            column.getRefColumnName());

        break;
      default:
        def.add(column.getColumnType().toString().toLowerCase());
    }
    if (Boolean.TRUE.equals(column.getNullable())) def.add("nullable");
    if (Boolean.TRUE.equals(column.getReadonly())) def.add("readonly");
    if (Boolean.TRUE.equals(column.isPrimaryKey())) def.add("pkey");
    if (Boolean.TRUE.equals(column.isUnique())) def.add("unique");
    if (column.getDefaultValue() != null) def.add("default", column.getDefaultValue());

    rows.add(
        new Emx2FileRow(
            column.getTable().getTableName(),
            column.getColumnName(),
            def.toString(),
            column.getDescription()));
  }
}
