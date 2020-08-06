package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

public class Emx2 {

  public static final String TABLE_NAME = "tableName";
  public static final String COLUMN_NAME = "columnName";
  public static final String DESCRIPTION = "description";
  public static final String TABLE_EXTENDS = "tableExtends";
  public static final String COLUMN_TYPE = "columnType";
  public static final String KEY = "key";
  public static final String REF = "ref";
  public static final String MAPPED_BY = "mappedBy";
  public static final String NULLABLE = "nullable";
  private static final String VALIDATION = "validation";

  public static SchemaMetadata fromRowList(List<Row> rows) {

    SchemaMetadata schema = new SchemaMetadata();
    int lineNo = 1;

    for (Row r : rows) {
      String tableName = r.getString(TABLE_NAME);
      if (tableName == null) {
        throw new MolgenisException(
            "Parsing of sheet molgenis failed",
            "Required column " + TABLE_NAME + " is empty on line " + lineNo);
      }

      if (schema.getTableMetadata(tableName) == null) {
        schema.create(table(tableName));
      }

      // load table metadata, this is when columnName is empty
      if (r.getString(COLUMN_NAME) == null) {
        schema.getTableMetadata(tableName).setDescription(r.getString(DESCRIPTION));
        schema.getTableMetadata(tableName).setInherit(r.getString(TABLE_EXTENDS));
      }

      // load column metadata
      else {
        if (r.getString(TABLE_EXTENDS) != null) {
          throw new MolgenisException(
              "Parsing of sheet molgenis failed",
              "Column " + TABLE_EXTENDS + " not supported for columns at " + lineNo);
        }

        Column column = column(r.getString(COLUMN_NAME));
        if (r.notNull(COLUMN_TYPE))
          column.type(ColumnType.valueOf(r.getString(COLUMN_TYPE).toUpperCase()));
        if (r.notNull(KEY)) column.key(r.getInteger(KEY));
        if (r.notNull(REF)) column.refTable(r.getString(REF));
        if (r.notNull(MAPPED_BY)) column.mappedBy(r.getString(MAPPED_BY));
        if (r.notNull(NULLABLE)) column.nullable(r.getBoolean(NULLABLE));
        if (r.notNull(DESCRIPTION)) column.setDescription(r.getString(DESCRIPTION));
        if (r.notNull(VALIDATION)) column.validation(r.getString(VALIDATION));

        schema.getTableMetadata(tableName).add(column);
      }
      lineNo++;
    }
    return schema;
  }

  public static List<Row> toRowList(SchemaMetadata schema) {
    List<Row> result = new ArrayList<>();

    // deterministic order (TODO make user define order)
    List<String> tableNames = new ArrayList<>();
    tableNames.addAll(schema.getTableNames());
    Collections.sort(tableNames);

    for (String tableName : tableNames) {
      TableMetadata t = schema.getTableMetadata(tableName);

      Row row = new Row();
      row.setString(TABLE_NAME, t.getTableName());
      if (t.getInherit() != null) row.setString(TABLE_EXTENDS, t.getInherit());
      if (t.getDescription() != null) row.setString(DESCRIPTION, t.getDescription());
      result.add(row);

      // deterministic order (TODO make user define order)
      List<String> columnNames = new ArrayList<>(t.getColumnNames());
      Collections.sort(columnNames);

      for (String columnName : columnNames) {

        Column c = t.getColumn(columnName);

        // only non-inherited
        if (c.getTableName().equals(t.getTableName())) {
          row = new Row();

          row.setString(TABLE_NAME, t.getTableName());
          row.setString(COLUMN_NAME, c.getName());
          if (!c.getColumnType().equals(STRING))
            row.setString(COLUMN_TYPE, c.getColumnType().toString().toLowerCase());
          if (c.isNullable() == true) row.setBool(NULLABLE, c.isNullable());
          if (c.getKey() > 0) row.setInt(KEY, c.getKey());
          if (c.getRefTableName() != null) row.setString(REF, c.getRefTableName());
          if (c.getMappedBy() != null) row.setString(MAPPED_BY, c.getMappedBy());
          if (c.getDescription() != null) row.set(DESCRIPTION, c.getDescription());
          if (c.getValidation() != null) row.set(VALIDATION, c.getValidation());

          result.add(row);
        }
      }
    }
    return result;
  }

  public static SchemaMetadata loadEmx2File(File file, Character separator) throws IOException {
    return fromRowList(CsvTableReader.readList(file, separator));
  }

  public static void toCsv(SchemaMetadata model, Writer writer, Character separator)
      throws IOException {
    CsvTableWriter.rowsToCsv(toRowList(model), writer, separator);
  }
}
