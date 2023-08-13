package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestMgColumns {
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestMgColumns.class.getSimpleName());
  }

  @Test
  public void testMgDraft() {
    Table t =
        schema.create(
            table(
                "MgDraft",
                column("id").setPkey(),
                column("required").setRequired(true),
                column("notrequired")));

    try {
      t.insert(row("id", 1, "notrequired", "somevalue1"));
      fail("should fail");
    } catch (Exception e) {
      // ok
    }

    try {
      t.insert(row("id", 1, "notrequired", "somevalue2").setDraft(true));
    } catch (Exception e) {
      fail("should succeed");
    }

    // verify
    assertEquals(1, t.retrieveRows().size());
    assertEquals("somevalue2", t.retrieveRows().get(0).getString("notrequired"));

    // to make sure also test with subclass
    t = schema.create(table("MgDraftSuper", column("id").setPkey()));
    t =
        schema.create(
            table("MgDraftSub", column("required").setRequired(true), column("notrequired"))
                .setInherit("MgDraftSuper"));

    try {
      t.insert(row("id", 1, "notrequired", "somevalue1"));
      fail("should fail");
    } catch (Exception e) {
      // ok
    }

    try {
      t.insert(row("id", 1, "notrequired", "somevalue2").setDraft(true));
    } catch (Exception e) {
      fail("should succeed");
    }
  }

  @Test
  public void testUpdatedOn() {
    Table t = schema.create(table("UpdatedOn", column("id").setPkey()));

    t.insert(row("id", 1));
    Row r = t.retrieveRows().get(0);
    assertNotNull(r.getDateTime(MG_INSERTEDON));
    assertEquals(r.getDateTime(MG_INSERTEDON), r.getDateTime(MG_UPDATEDON));
    assertNotNull(r.getString(MG_INSERTEDBY));
    assertEquals(r.getString(MG_UPDATEDBY), r.getString(MG_INSERTEDBY));

    t.update(r);
    r = t.retrieveRows().get(0);
    assertTrue(r.getDateTime(MG_INSERTEDON).compareTo(r.getDateTime(MG_UPDATEDON)) < 0);

    // to make sure also test with subclass
    t = schema.create(table("UpdatedOnSub").setInherit("UpdatedOn"));

    t.insert(row("id", 2));
    r = t.retrieveRows().get(0);
    assertNotNull(r.getDateTime(MG_INSERTEDON));
    assertEquals(r.getDateTime(MG_INSERTEDON), r.getDateTime(MG_UPDATEDON));
    assertNotNull(r.getString(MG_INSERTEDBY));
    assertEquals(r.getString(MG_UPDATEDBY), r.getString(MG_INSERTEDBY));

    t.update(r);
    r = t.retrieveRows().get(0);
    assertTrue(r.getDateTime(MG_INSERTEDON).compareTo(r.getDateTime(MG_UPDATEDON)) < 0);
  }
}
