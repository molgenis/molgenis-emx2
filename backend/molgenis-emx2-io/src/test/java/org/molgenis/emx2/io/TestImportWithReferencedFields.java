package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestImportWithReferencedFields {

  private final Database database = TestDatabaseFactory.getTestDatabase();
  private Schema schema;

  @BeforeEach
  void setup() {
    schema = database.dropCreateSchema(TestImportWithReferencedFields.class.getSimpleName());
    schema.create(
        table("Hospital", column("name").setPkey(), column("city")),
        table(
            "Doctor",
            column("id").setPkey(),
            column("name"),
            column("hospital").setType(REF).setRefTable("Hospital"),
            column("hospital city").setComputed("hospital.city")));
  }

  @Test
  void computesFromReferencedTableImportedInTheSameUpload() {
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    store.setCsvString("Doctor", "id,name,hospital\nd1,Dr. Bob,UMCG\nd2,Dr. Ann,Erasmus MC");
    store.setCsvString("Hospital", "name,city\nUMCG,Groningen\nErasmus MC,Rotterdam");

    new ImportSchemaTask(store, schema, false).run();

    assertEquals("Groningen", getHospitalCity("d1"));
    assertEquals("Rotterdam", getHospitalCity("d2"));
  }

  private String getHospitalCity(String doctorId) {
    return schema
        .getTable("Doctor")
        .where(f("id", EQUALS, doctorId))
        .retrieveRows()
        .getFirst()
        .getString("hospital city");
  }
}
