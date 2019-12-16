package org.molgenis.emx2.sql;

import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;
import org.molgenis.emx2.MolgenisException;

import java.util.UUID;

import static junit.framework.TestCase.fail;

public class TestDeferConstrainChecksToEndOfTransaction {
  Database database = DatabaseFactory.getTestDatabase();

  public TestDeferConstrainChecksToEndOfTransaction() {}

  @Test(expected = MolgenisException.class)
  public void DependencyOrderOutsideTransactionFails() {
    runTestCase(database);
  }

  public void runTestCase(Database db) {
    Schema schema = db.createSchema("TestDeffered");

    Table subjectTable = schema.createTableIfNotExists("Subject");
    subjectTable.getMetadata().addColumn("ID", ColumnType.INT).primaryKey();

    Table sampleTable = schema.createTableIfNotExists("Sample");
    sampleTable.getMetadata().addColumn("ID", ColumnType.INT).primaryKey();
    sampleTable.getMetadata().addRef("subject", "Subject");

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
            Schema schema = db.createSchema("TestDeffered3");

            Table subjectTable = schema.createTableIfNotExists("Subject");

            Table sampleTable = schema.createTableIfNotExists("Sample");
            sampleTable.getMetadata().addRef("subject", "Subject");

            StopWatch.print("schema created");

            Row subject1 = new Row();
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
