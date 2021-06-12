package org.molgenis.emx2.semantics;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.JsonLdExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestJsonld {
  private static Database db;
  private static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestJsonld.class.getSimpleName());
  }

  @Test
  public void testJsonldMetadata() {
    JsonLdExample.create(schema);

    assertEquals(
        "https://schema.org/docs/jsonldcontext.jsonld#Person",
        db.getSchema(schema.getName()).getTable("Person").getMetadata().getSemantics()[0]);

    StringWriter sw = new StringWriter();
    LinkedDataService.getJsonLdForSchema(schema, new PrintWriter(sw));
    System.out.println(sw.toString());

    sw = new StringWriter();
    LinkedDataService.getTtlForSchema(schema, new PrintWriter(sw));
    System.out.println(sw.toString());
  }
}
