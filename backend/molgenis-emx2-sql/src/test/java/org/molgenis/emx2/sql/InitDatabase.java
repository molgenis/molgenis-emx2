package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

/* used by gradle*/
public class InitDatabase {
  public static void main(String[] args) {
    System.out.println("INITIALIZING DATABASE");
    new SqlDatabase(true);
  }

  @Test
  void testJitIsOff() {
    Database db = TestDatabaseFactory.getTestDatabase();
    org.jooq.Record record = ((SqlDatabase) db).getJooq().fetch("show jit").get(0);
    if (!"off".equals(record.get(0))) {
      fail("ensure that postgresql SET jit='off' is applied");
    }
  }
}
