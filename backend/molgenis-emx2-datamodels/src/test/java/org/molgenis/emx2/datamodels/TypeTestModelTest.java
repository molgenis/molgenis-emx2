package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TypeTestModelTest {

  private static final String SCHEMA = TypeTestModelTest.class.getSimpleName();
  private static final String TYPES = "Types";

  private static Schema schema;

  @BeforeAll
  static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA);
    DataModels.Profile.TYPE_TEST.getImportTask(database, SCHEMA, "type test", true).run();
    schema = database.getSchema(SCHEMA);
  }

  @Test
  void typesTableHasEnumColumnsWithValuesInOrderWrittenAndDemoRow() {
    TableMetadata types = schema.getTable(TYPES).getMetadata();
    assertEquals(ColumnType.ENUM, types.getColumn("enum type").getColumnType());
    assertEquals(List.of("yes", "no", "unknown"), types.getColumn("enum type").getAllowedValues());
    assertEquals(ColumnType.ENUM_ARRAY, types.getColumn("enum array type").getColumnType());
    assertEquals(
        List.of("red", "green", "blue"), types.getColumn("enum array type").getAllowedValues());

    List<Row> rows = schema.getTable(TYPES).retrieveRows();
    assertFalse(rows.isEmpty());
    assertEquals("yes", rows.get(0).getString("enum type"));
    assertArrayEquals(new String[] {"red", "blue"}, rows.get(0).getStringArray("enum array type"));
  }
}
