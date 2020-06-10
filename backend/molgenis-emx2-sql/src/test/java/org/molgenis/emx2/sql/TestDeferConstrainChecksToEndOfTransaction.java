package org.molgenis.emx2.sql;

import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;
import org.molgenis.emx2.MolgenisException;

import java.util.UUID;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;

public class TestDeferConstrainChecksToEndOfTransaction {
  Database database = TestDatabaseFactory.getTestDatabase();

  public TestDeferConstrainChecksToEndOfTransaction() {}

  @Test(expected = MolgenisException.class)
  public void DependencyOrderOutsideTransactionFails() {
    runTestCase(database);
  }

  public void runTestCase(Database db) {
    Schema schema = db.dropCreateSchema("TestDeffered");

    Table subjectTable = schema.create(table("Subject").add(column("ID").type(INT)).pkey("ID"));

    Table sampleTable =
        schema.create(
            table("Sample")
                .add(column("ID").type(INT))
                .add(column("subject").type(REF).refTable("Subject"))
                .pkey("ID"));

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

            Table subjectTable = schema.create(table("Subject"));

            Table sampleTable =
                schema.create(table("Sample").add(column("subject").type(REF).refTable("Subject")));

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
