package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
public class TestImportEmptyAutoId {

  static Database db;
  private static final String SCHEMA_NAME = "TestImportAutoId";
  private static final String TABLE_NAME = "TestImportAutoIdTable";
  private static final String ID_FIELD_NAME = "testTableId";
  private static final String TEST_XLS_FILENAME = "TestImportEmptyAutoId.xlsx";
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    createTable();
  }

  @Test
  public void testImportEmptyAutoIdFieldXls() {
    ClassLoader classLoader = getClass().getClassLoader();
    File xlsFile = new File(classLoader.getResource(TEST_XLS_FILENAME).getFile());

    MolgenisIO.importFromExcelFile(xlsFile.toPath(), schema, true);

    List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();

    assertFalse(rows.isEmpty());
    assertNotNull(rows.get(0).getString(ID_FIELD_NAME));

    db.dropSchemaIfExists(SCHEMA_NAME);
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
