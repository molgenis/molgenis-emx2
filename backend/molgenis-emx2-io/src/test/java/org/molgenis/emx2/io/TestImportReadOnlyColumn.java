package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestImportReadOnlyColumn {
  static Database db;
  private static final String SCHEMA_NAME = "TestImportReadOnly";
  private static final String TABLE_NAME = "TestImportReadOnlyTable";
  private static final String ID_FIELD_NAME = "readonly";
  private static final String READ_ONLY_COLUMN = "readOnlyColumn";
  private static final String TEST_CSV_FILENAME = "TestImportReadOnly.csv";
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    createTable();
  }

  @AfterAll
  public static void tearDown() {
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  public void testImportReadOnlyColumnCSV() {
    Iterable<Row> rows =
        new Iterable<>() {
          @NotNull
          @Override
          public Iterator<Row> iterator() {
            return null;
          }
        };
    SchemaMetadata schemaMetadata = Emx2.fromRowList(rows);
    TableMetadata tableMetadata = schemaMetadata.getTableMetadata(TABLE_NAME);
    List<Column> columns = tableMetadata.getColumns();
    Column column =
        columns.stream()
            .filter(col -> col.getName().equals(READ_ONLY_COLUMN))
            .findFirst()
            .orElse(null);
    assert column != null;
    assertTrue(column.isReadonly());
  }

  static void createTable() {
    schema.create(
        table(
            TABLE_NAME,
            column(ID_FIELD_NAME)
                .setPkey()
                .setType(ColumnType.AUTO_ID)
                .setComputed("TEST:${mg_autoid}"),
            column("testStringField").setType(ColumnType.STRING),
            column("testIntField").setType(ColumnType.INT)));
  }
}
