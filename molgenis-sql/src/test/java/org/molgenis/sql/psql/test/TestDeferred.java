package org.molgenis.sql.psql.test;

import javafx.scene.paint.Stop;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.RowBean;
import org.molgenis.utils.StopWatch;

public class TestDeferred {

  @Test
  public void test1() throws MolgenisException {
    StopWatch.start("test1");

    Database database = DatabaseFactory.getDatabase();
    StopWatch.print("database created");

    database.transaction(
        db -> {
          Schema s = db.createSchema("TestDeffered");

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
}
