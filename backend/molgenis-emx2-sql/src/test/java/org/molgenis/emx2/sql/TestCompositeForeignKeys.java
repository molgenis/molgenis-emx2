package org.molgenis.emx2.sql;

import org.junit.Before;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

import static org.junit.Assert.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class TestCompositeForeignKeys {
  private Schema schema;

  @Before
  public void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestCompositeForeignKeys.class.getSimpleName());

    // create target table

  }

  @Test
  public void testCompositeRefArray() {

    schema.create(
        table(
            "Person",
            column("firstName").pkey(),
            column("lastName").pkey(),
            column("cousins", REF_ARRAY).refTable("Person"))); // .with("lastName", "lastName")));

    Table p = schema.getTable("Person");

    p.insert(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));

    p.insert(
        new Row()
            .setString("firstName", "Donald")
            .setString("lastName", "Duck")
            .setString("cousins-firstName", "Kwik")
            .setString("cousins-lastName", "Duck"));

    try {
      p.delete(new Row().setString("firstName", "Kwik").setString("lastName", "Duck"));
      fail("should have failed on foreign key error");
    } catch (Exception e) {
      System.out.println("errored correctly: " + e);
    }
  }

  @Test
  public void testCompositeMref() {
    schema.create(
        table(
            "Person2",
            column("firstName").pkey(),
            column("lastName").pkey(),
            column("father", REF).refTable("Person2"),
            column("mother", REF).refTable("Person2"),
            column("children", MREF).refTable("Person2"))); // .with("lastName", "lastName")));
  }
}
