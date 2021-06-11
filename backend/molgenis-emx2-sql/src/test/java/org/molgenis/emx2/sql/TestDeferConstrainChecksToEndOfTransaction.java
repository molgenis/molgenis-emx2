package org.molgenis.emx2.sql;

import static junit.framework.TestCase.fail;

import java.util.UUID;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

public class TestDeferConstrainChecksToEndOfTransaction {
  Database database = TestDatabaseFactory.getTestDatabase();

  public TestDeferConstrainChecksToEndOfTransaction() {}

  @Test(expected = MolgenisException.class)
  public void DependencyOrderOutsideTransactionFails() {
    runTestCase(database);
  }

  public void runTestCase(Database db) {
    Schema schema = db.dropCreateSchema("TestDeffered");

    Table subjectTable =
        schema.create(
            TableMetadata.table("Subject")
                .add(Column.column("ID").setType(ColumnType.INT).setPkey()));

    Table sampleTable =
        schema.create(
            TableMetadata.table("Sample")
                .add(Column.column("ID").setType(ColumnType.INT).setPkey())
                .add(Column.column("subject").setType(ColumnType.REF).setRefTable("Subject")));

    StopWatch.print("schema created");

    Row aSubjectRow = new Row();
    Row aSampleRow = new Row().setInt("subject", aSubjectRow.getInteger("ID"));

    sampleTable.insert(aSampleRow);
    subjectTable.insert(aSubjectRow);
  }

  @Test
  public void foreignKeysInTransactionsAraProtected() {
    StopWatch.start("foreignKeysInTransactionsAraProtected");

    try {
      database.tx(
          db -> {
            Schema schema = db.dropCreateSchema("TestDeffered3");

            Table subjectTable =
                schema.create(TableMetadata.table("Subject", Column.column("id").setPkey()));

            Table sampleTable =
                schema.create(
                    TableMetadata.table("Sample")
                        .add(
                            Column.column("subject")
                                .setType(ColumnType.REF)
                                .setRefTable("Subject")));

            StopWatch.print("schema created");

            Row subject1 = new Row().set("id", UUID.randomUUID());
            Row sample1 = new Row().setUuid("subject", UUID.randomUUID());
            Row sample2 = new Row().setUuid("subject", UUID.randomUUID());

            sampleTable.insert(sample1, sample2);
            subjectTable.insert(subject1);

            StopWatch.print("data added");
          });
      StopWatch.print("transaction committed)");
      fail("should have failed on wrong fkey");
    } catch (MolgenisException e) {
      StopWatch.print("errored correctly " + e.toString());
    }
  }
}
