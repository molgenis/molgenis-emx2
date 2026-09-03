package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestMgColumns {
  private static final String EDITOR_USER = "mgcolumns_editor";
  private static Database database;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    schema = database.dropCreateSchema(TestMgColumns.class.getSimpleName());
    if (!database.hasUser(EDITOR_USER)) database.addUser(EDITOR_USER);
    schema.addMember(EDITOR_USER, Privileges.EDITOR.toString());
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
                .setInheritName("MgDraftSuper"));

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

    // update without mg values, so they are set to the active user and the current time
    r.clear(MG_UPDATEDBY);
    r.clear(MG_UPDATEDON);
    t.update(r);
    r = t.retrieveRows().get(0);
    assertTrue(r.getDateTime(MG_INSERTEDON).compareTo(r.getDateTime(MG_UPDATEDON)) < 0);

    // to make sure also test with subclass
    t = schema.create(table("UpdatedOnSub").setInheritName("UpdatedOn"));

    t.insert(row("id", 2));
    r = t.retrieveRows().get(0);
    assertNotNull(r.getDateTime(MG_INSERTEDON));
    assertEquals(r.getDateTime(MG_INSERTEDON), r.getDateTime(MG_UPDATEDON));
    assertNotNull(r.getString(MG_INSERTEDBY));
    assertEquals(r.getString(MG_UPDATEDBY), r.getString(MG_INSERTEDBY));

    r.clear(MG_UPDATEDBY);
    r.clear(MG_UPDATEDON);
    t.update(r);
    r = t.retrieveRows().get(0);
    assertTrue(r.getDateTime(MG_INSERTEDON).compareTo(r.getDateTime(MG_UPDATEDON)) < 0);
  }

  @Test
  public void testSaveKeepsInsertedMetadata() {
    Table t = schema.create(table("SavedOn", column("id").setPkey(), column("value")));

    t.insert(row("id", 1, "value", "somevalue1"));
    Row inserted = t.retrieveRows().getFirst();

    t.save(row("id", 1, "value", "somevalue2"));

    Row saved = t.retrieveRows().getFirst();
    assertEquals("somevalue2", saved.getString("value"));
    assertEquals(inserted.getDateTime(MG_INSERTEDON), saved.getDateTime(MG_INSERTEDON));
    assertEquals(inserted.getString(MG_INSERTEDBY), saved.getString(MG_INSERTEDBY));
    assertTrue(saved.getDateTime(MG_INSERTEDON).isBefore(saved.getDateTime(MG_UPDATEDON)));
  }

  @Test
  public void testProvidedMgValuesAreUsedOnInsertUpdateAndSave() {
    Table t = schema.create(table("ProvidedMg", column("id").setPkey(), column("value")));

    LocalDateTime insertedOn = LocalDateTime.of(2001, 1, 1, 10, 0, 0);
    LocalDateTime updatedOn = LocalDateTime.of(2002, 2, 2, 10, 0, 0);
    t.insert(
        row(
            "id",
            1,
            "value",
            "somevalue1",
            MG_INSERTEDBY,
            "importer1",
            MG_INSERTEDON,
            insertedOn,
            MG_UPDATEDBY,
            "importer2",
            MG_UPDATEDON,
            updatedOn));

    Row inserted = t.retrieveRows().getFirst();
    assertEquals("importer1", inserted.getString(MG_INSERTEDBY));
    assertEquals(insertedOn, inserted.getDateTime(MG_INSERTEDON));
    assertEquals("importer2", inserted.getString(MG_UPDATEDBY));
    assertEquals(updatedOn, inserted.getDateTime(MG_UPDATEDON));

    // update with provided values
    LocalDateTime updatedOn2 = LocalDateTime.of(2003, 3, 3, 10, 0, 0);
    t.update(
        row("id", 1, "value", "somevalue2", MG_UPDATEDBY, "importer3", MG_UPDATEDON, updatedOn2));

    Row updated = t.retrieveRows().getFirst();
    assertEquals("somevalue2", updated.getString("value"));
    assertEquals("importer1", updated.getString(MG_INSERTEDBY));
    assertEquals(insertedOn, updated.getDateTime(MG_INSERTEDON));
    assertEquals("importer3", updated.getString(MG_UPDATEDBY));
    assertEquals(updatedOn2, updated.getDateTime(MG_UPDATEDON));

    // update can also override the inserted metadata
    LocalDateTime insertedOn2 = LocalDateTime.of(2005, 5, 5, 10, 0, 0);
    t.update(
        row(
            "id",
            1,
            "value",
            "somevalue2b",
            MG_INSERTEDBY,
            "importer3b",
            MG_INSERTEDON,
            insertedOn2));

    updated = t.retrieveRows().getFirst();
    assertEquals("importer3b", updated.getString(MG_INSERTEDBY));
    assertEquals(insertedOn2, updated.getDateTime(MG_INSERTEDON));

    // save on an existing row (insert on conflict) with provided values
    LocalDateTime updatedOn3 = LocalDateTime.of(2004, 4, 4, 10, 0, 0);
    LocalDateTime insertedOn3 = LocalDateTime.of(2006, 6, 6, 10, 0, 0);
    t.save(
        row(
            "id",
            1,
            "value",
            "somevalue3",
            MG_INSERTEDBY,
            "importer4b",
            MG_INSERTEDON,
            insertedOn3,
            MG_UPDATEDBY,
            "importer4",
            MG_UPDATEDON,
            updatedOn3));

    Row saved = t.retrieveRows().getFirst();
    assertEquals("somevalue3", saved.getString("value"));
    assertEquals("importer4", saved.getString(MG_UPDATEDBY));
    assertEquals(updatedOn3, saved.getDateTime(MG_UPDATEDON));
    assertEquals("importer4b", saved.getString(MG_INSERTEDBY));
    assertEquals(insertedOn3, saved.getDateTime(MG_INSERTEDON));

    // without provided values the active user and current time are applied again
    t.update(row("id", 1, "value", "somevalue4"));

    Row fallback = t.retrieveRows().getFirst();
    assertEquals(schema.getDatabase().getActiveUser(), fallback.getString(MG_UPDATEDBY));
    assertTrue(fallback.getDateTime(MG_UPDATEDON).isAfter(updatedOn3));
    assertEquals("importer4b", fallback.getString(MG_INSERTEDBY));
    assertEquals(insertedOn3, fallback.getDateTime(MG_INSERTEDON));
  }

  @Test
  public void testProvidedMgValuesAreIgnoredWithoutManagePermission() {
    Table t = schema.create(table("ProvidedMgAsEditor", column("id").setPkey(), column("value")));
    LocalDateTime provided = LocalDateTime.of(2001, 1, 1, 10, 0, 0);

    try {
      database.setActiveUser(EDITOR_USER);
      Table editorTable = database.getSchema(schema.getName()).getTable("ProvidedMgAsEditor");

      editorTable.insert(
          row(
              "id",
              1,
              "value",
              "somevalue1",
              MG_INSERTEDBY,
              "importer1",
              MG_INSERTEDON,
              provided,
              MG_UPDATEDBY,
              "importer2",
              MG_UPDATEDON,
              provided));

      Row inserted = editorTable.retrieveRows().getFirst();
      assertEquals(EDITOR_USER, inserted.getString(MG_INSERTEDBY));
      assertEquals(EDITOR_USER, inserted.getString(MG_UPDATEDBY));
      assertTrue(inserted.getDateTime(MG_INSERTEDON).isAfter(provided));
      assertTrue(inserted.getDateTime(MG_UPDATEDON).isAfter(provided));

      // same for update and for save on an existing row
      editorTable.update(
          row(
              "id",
              1,
              "value",
              "somevalue2",
              MG_INSERTEDBY,
              "importer1",
              MG_UPDATEDBY,
              "importer2"));
      Row updated = editorTable.retrieveRows().getFirst();
      assertEquals("somevalue2", updated.getString("value"));
      assertEquals(EDITOR_USER, updated.getString(MG_INSERTEDBY));
      assertEquals(EDITOR_USER, updated.getString(MG_UPDATEDBY));

      editorTable.save(
          row("id", 1, "value", "somevalue3", MG_INSERTEDBY, "importer1", MG_INSERTEDON, provided));
      Row saved = editorTable.retrieveRows().getFirst();
      assertEquals("somevalue3", saved.getString("value"));
      assertEquals(EDITOR_USER, saved.getString(MG_INSERTEDBY));
      assertTrue(saved.getDateTime(MG_INSERTEDON).isAfter(provided));
    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  public void testSaveKeepsInsertedMetadataInSubclass() {
    schema.create(table("SavedOnSuper", column("id").setPkey(), column("value")));
    Table t = schema.create(table("SavedOnSub").setInheritName("SavedOnSuper"));

    t.insert(row("id", 1, "value", "somevalue1"));
    Row inserted = t.retrieveRows().getFirst();

    t.save(row("id", 1, "value", "somevalue2"));

    Row saved = t.retrieveRows().getFirst();
    assertEquals("somevalue2", saved.getString("value"));
    assertEquals(inserted.getDateTime(MG_INSERTEDON), saved.getDateTime(MG_INSERTEDON));
    assertEquals(inserted.getString(MG_INSERTEDBY), saved.getString(MG_INSERTEDBY));
    assertTrue(saved.getDateTime(MG_INSERTEDON).isBefore(saved.getDateTime(MG_UPDATEDON)));
  }
}
