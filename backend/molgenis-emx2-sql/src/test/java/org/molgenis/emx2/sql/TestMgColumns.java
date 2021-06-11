package org.molgenis.emx2.sql;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestMgColumns {
  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestMgColumns.class.getSimpleName());
  }

  @Test
  public void testMgDraft() {
    Table t =
        schema.create(
            TableMetadata.table(
                "MgDraft",
                Column.column("id").setPkey(),
                Column.column("required").setRequired(true),
                Column.column("notrequired")));

    try {
      t.insert(Row.row("id", 1, "notrequired", "somevalue1"));
      fail("should fail");
    } catch (Exception e) {
      // ok
    }

    try {
      t.insert(Row.row("id", 1, "notrequired", "somevalue2").setDraft(true));
    } catch (Exception e) {
      fail("should succeed");
    }

    // verify
    Assert.assertEquals(1, t.retrieveRows().size());
    Assert.assertEquals("somevalue2", t.retrieveRows().get(0).getString("notrequired"));

    // to make sure also test with subclass
    t = schema.create(TableMetadata.table("MgDraftSuper", Column.column("id").setPkey()));
    t =
        schema.create(
            TableMetadata.table(
                    "MgDraftSub",
                    Column.column("required").setRequired(true),
                    Column.column("notrequired"))
                .setInherit("MgDraftSuper"));

    try {
      t.insert(Row.row("id", 1, "notrequired", "somevalue1"));
      fail("should fail");
    } catch (Exception e) {
      // ok
    }

    try {
      t.insert(Row.row("id", 1, "notrequired", "somevalue2").setDraft(true));
    } catch (Exception e) {
      fail("should succeed");
    }
  }

  @Test
  public void testUpdatedOn() {
    Table t = schema.create(TableMetadata.table("UpdatedOn", Column.column("id").setPkey()));

    t.insert(Row.row("id", 1));
    Row r = t.retrieveRows().get(0);
    Assert.assertNotNull(r.getDateTime(Constants.MG_INSERTEDON));
    Assert.assertEquals(
        r.getDateTime(Constants.MG_INSERTEDON), r.getDateTime(Constants.MG_UPDATEDON));
    Assert.assertNotNull(r.getString(Constants.MG_INSERTEDBY));
    Assert.assertEquals(r.getString(Constants.MG_UPDATEDBY), r.getString(Constants.MG_INSERTEDBY));

    t.update(r);
    r = t.retrieveRows().get(0);
    Assert.assertTrue(
        r.getDateTime(Constants.MG_INSERTEDON).compareTo(r.getDateTime(Constants.MG_UPDATEDON))
            < 0);

    // to make sure also test with subclass
    t = schema.create(TableMetadata.table("UpdatedOnSub").setInherit("UpdatedOn"));

    t.insert(Row.row("id", 2));
    r = t.retrieveRows().get(0);
    Assert.assertNotNull(r.getDateTime(Constants.MG_INSERTEDON));
    Assert.assertEquals(
        r.getDateTime(Constants.MG_INSERTEDON), r.getDateTime(Constants.MG_UPDATEDON));
    Assert.assertNotNull(r.getString(Constants.MG_INSERTEDBY));
    Assert.assertEquals(r.getString(Constants.MG_UPDATEDBY), r.getString(Constants.MG_INSERTEDBY));

    t.update(r);
    r = t.retrieveRows().get(0);
    Assert.assertTrue(
        r.getDateTime(Constants.MG_INSERTEDON).compareTo(r.getDateTime(Constants.MG_UPDATEDON))
            < 0);
  }
}
