package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

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
  public static final String REF_TABLE = "refTable";
  public static final String REF_FROM = "refFrom";
  public static final String REF_TO = "refTo";
  public static final String MAPPED_BY = "mappedBy";
  public static final String NULLABLE = "nullable";
  private static final String VALIDATION = "validation";
  private static final String RDF_TEMPLATE = "rdfTemplate";

  private Emx2() {
    // hidden
  }

  public static SchemaMetadata fromRowList(Iterable<Row> rows) {

    SchemaMetadata schema = new SchemaMetadata();
    int lineNo = 1;

    for (Row r : rows) {
      String tableName = r.getString(TABLE_NAME);
      if (tableName == null) {
        throw new MolgenisException(
            "Parsing of sheet molgenis failed: Required column "
                + TABLE_NAME
                + " is empty on line "
                + lineNo);
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
              "Parsing of sheet molgenis failed: Column "
                  + TABLE_EXTENDS
                  + " not supported for columns at "
                  + lineNo);
        }

        Column column = column(r.getString(COLUMN_NAME));
        if (r.notNull(COLUMN_TYPE))
          column.setType(ColumnType.valueOf(r.getString(COLUMN_TYPE).toUpperCase()));
        if (r.notNull(KEY)) column.setKey(r.getInteger(KEY));
        if (r.notNull(REF_TABLE)) column.setRefTable(r.getString(REF_TABLE));
        if (r.notNull(REF_TO)) column.setRefTo(r.getStringArray(REF_TO));
        if (r.notNull(REF_FROM)) column.setRefFrom(r.getStringArray(REF_FROM));
        if (r.notNull(MAPPED_BY)) column.setMappedBy(r.getString(MAPPED_BY));
        if (r.notNull(NULLABLE)) column.setNullable(r.getBoolean(NULLABLE));
        if (r.notNull(DESCRIPTION)) column.setDescription(r.getString(DESCRIPTION));
        if (r.notNull(VALIDATION)) column.setValidationScript(r.getString(VALIDATION));
        if (r.notNull(RDF_TEMPLATE)) column.setRdfTemplate(r.getString(RDF_TEMPLATE));

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

      List<String> columnNames = new ArrayList<>(t.getColumnNames());
      for (String columnName : columnNames) {

        Column c = t.getColumn(columnName);

        // only non-inherited
        if (c.getTableName().equals(t.getTableName())) {
          row = new Row();

          row.setString(TABLE_NAME, t.getTableName());
          row.setString(COLUMN_NAME, c.getName());
          if (!c.getColumnType().equals(STRING))
            row.setString(COLUMN_TYPE, c.getColumnType().toString().toLowerCase());
          if (c.isNullable()) row.setBool(NULLABLE, c.isNullable());
          if (c.getKey() > 0) row.setInt(KEY, c.getKey());
          if (c.getRefTableName() != null) row.setString(REF_TABLE, c.getRefTableName());
          if (c.getRefTo() != null) row.setStringArray(REF_TO, c.getRefTo());
          if (c.getRefFrom() != null) row.setStringArray(REF_FROM, c.getRefFrom());
          if (c.getMappedBy() != null) row.setString(MAPPED_BY, c.getMappedBy());
          if (c.getDescription() != null) row.set(DESCRIPTION, c.getDescription());
          if (c.getValidationScript() != null) row.set(VALIDATION, c.getValidationScript());

          result.add(row);
        }
      }
    }
    return result;
  }

  public static SchemaMetadata loadEmx2File(File file, Character separator) throws IOException {
    return fromRowList(CsvTableReader.read(file, separator));
  }

  public static void toCsv(SchemaMetadata model, Writer writer, Character separator)
      throws IOException {
    CsvTableWriter.write(toRowList(model), writer, separator);
  }
}
