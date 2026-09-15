package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class SqlTableMetadataTest {

  public static final String SCHEMA_NAME = SqlTableMetadataTest.class.getSimpleName();

  @Test
  void givenNonExistingColumn_whenDroppingNonExistentColumn_thenThrow() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    SqlTableMetadata table =
        (SqlTableMetadata) schema.create(TableMetadata.table("dropColumn")).getMetadata();

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> table.dropColumn("non-existent"));
    assertEquals("Drop column non-existent failed: column does not exist", exception.getMessage());
  }
}
