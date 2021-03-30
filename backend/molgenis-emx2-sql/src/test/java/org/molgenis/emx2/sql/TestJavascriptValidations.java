package org.molgenis.emx2.sql;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;

public class TestJavascriptValidations {

  static Database database;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestJavascriptValidations.class.getSimpleName());
    PetStoreExample.create(schema.getMetadata());
  }

  @Test
  public void testValidation() {
    try {
      schema
          .getTable(PetStoreExample.USER)
          .insert(new Row().set("username", "test").set(PetStoreExample.EMAIL, "not an email"));
      fail("should have failed on email validation");
    } catch (MolgenisException e) {
      System.out.println("Correct error: " + e);
    }

    try {
      schema.getTable(PetStoreExample.ORDER).insert(new Row().set(PetStoreExample.QUANTITY, 0));
      fail("should have failed on quantity validation");
    } catch (MolgenisException e) {
      System.out.println("Correct error: " + e);
    }

    // should pass
    schema
        .getTable(PetStoreExample.USER)
        .insert(new Row().set("username", "test").set(PetStoreExample.EMAIL, "valid@email.com"));
  }

  //  @Test
  //  public void test() {
  //    Row row = new Row().set("small", 1).set("large", 100).set("email", "m.a.swertz@gmail.com");
  //    Row rowError = new Row().set("small", 100).set("large", 1).set("email", "m.a.swertz@gmail");
  //
  //    String script = "if(row.small > row.large) 'error';if(row.small > row.large) 'error';";
  //    System.out.println("Script is:\n" + script);
  //
  //    String emailValidation =
  //        "if(!/^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$/.test(row.email)) 'error'";
  //
  //    // burn in the cache
  //    validate(script, row);
  //    validate(emailValidation, row);
  //
  //    StopWatch.start("begin");
  //    int count = 1000000;
  //    for (int i = 0; i < count; i++) {
  //      String result;
  //      if (i == 500 || i == 7999 || i == 999998) {
  //        // validate(script, rowError);
  //        // validate(script, rowError);
  //        result = validate(emailValidation, rowError);
  //      } else {
  //        // validate(script, row);
  //        // validate(script, row);
  //        result = validate(emailValidation, row);
  //      }
  //      if (result != null) {
  //        System.out.println(i + ":" + result);
  //      }
  //    }
  //    StopWatch.print("done", count);
  //    System.out.println("'" + validate("row.small", row) + "'");
  //    System.out.println("'" + validate(script, row) + "'");
  //  }
}
