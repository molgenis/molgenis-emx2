package org.molgenis.sql.psql.test;

import javafx.scene.paint.Stop;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;
import org.molgenis.utils.StopWatch;

import java.util.UUID;

import static junit.framework.TestCase.fail;

public class TestDeferred {

  @Test(expected = MolgenisException.class)
  public void test1_nondeferred() throws MolgenisException {
    test1(false);
  }

  @Test
  public void test1_defered() throws MolgenisException {
    test1(true);
  }

  public void test1(boolean deferred) throws MolgenisException {

    StopWatch.start("test1");

    Database database = DatabaseFactory.getDatabase();
    StopWatch.print("database created");

    database.transaction(
        db -> {
          Schema s = db.createSchema("TestDeffered");
          db.setDeferChecks(deferred);

          Table subject = s.createTable("Subject");

          Table sample = s.createTable("Sample");
          sample.addRef("subject", "Subject");

          StopWatch.print("schema created");

          Row sub1 = new RowBean();
          Row sam1 = new RowBean().setUuid("subject", sub1.getMolgenisid());

          sample.insert(sam1);
          subject.insert(sub1);

          StopWatch.print("data added (in wrong dependency order, how cool is that??)");
        });
    StopWatch.print("transaction committed)");
  }

  @Test(expected = MolgenisException.class)
  public void test2() throws MolgenisException {
    Database database = DatabaseFactory.getDatabase();
    // without transaction
    {
      Schema s = database.createSchema("TestDeffered2");

      Table subject = s.createTable("Subject");

      Table sample = s.createTable("Sample");
      sample.addRef("subject", "Subject");

      Row sub1 = new RowBean();
      Row sam1 = new RowBean().setUuid("subject", sub1.getMolgenisid());

      sample.insert(sam1);
      subject.insert(sub1);
    }
  }

  @Test
  public void test3() throws MolgenisException {
    StopWatch.start("test1");

    Database database = DatabaseFactory.getDatabase();
    StopWatch.print("database created");

    try {
      database.transaction(
          db -> {
            Schema s = db.createSchema("TestDeffered3");

            Table subject = s.createTable("Subject");

            Table sample = s.createTable("Sample");
            sample.addRef("subject", "Subject");

            StopWatch.print("schema created");

            Row sub1 = new RowBean();
            Row sam1 = new RowBean().setUuid("subject", UUID.randomUUID());
            Row sam2 = new RowBean().setUuid("subject", UUID.randomUUID());

            sample.insert(sam1, sam2);
            subject.insert(sub1);

            StopWatch.print("data added");
          });
      StopWatch.print("transaction committed)");
      fail("should have failed on wrong fkey");
    } catch (MolgenisException e) {
      StopWatch.print("errored correctly " + e.getCause().getMessage());
    }
  }
}
