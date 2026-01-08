package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestImportProfileTask {

  private Database db;

  @Test
  void whenCreateSchemaFailsWithMolgenisException_thenRollback() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists("testFail");
    ImportProfileTask task =
        new ImportProfileTask(
            db,
                "_profiles/PetStore.yaml",
            false,
            database -> {
              database.createSchema("testFail", "description");
              throw new MolgenisException("error message");
            });
    assertThrows(MolgenisException.class, task::run);
    assertNull(db.getSchema("testFail"));
  }

  @Test
  void whenCreateSchemaFailsWithRunTimeException_thenRollback() {
    db = TestDatabaseFactory.getTestDatabase();
    db.dropSchemaIfExists("testFail");
    ImportProfileTask task =
        new ImportProfileTask(
            db,
                "_profiles/PetStore.yaml",
            false,
            database -> {
              database.createSchema("testFail", "description");
              throw new RuntimeException("error message");
            });
    assertThrows(RuntimeException.class, task::run);
    assertNull(db.getSchema("testFail"));
  }
}
