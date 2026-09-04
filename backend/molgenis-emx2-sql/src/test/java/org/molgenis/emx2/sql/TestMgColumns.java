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

class TestMgColumns {
  private static final String EDITOR_USER = "mgcolumns_editor";

  private static final LocalDateTime IN_2001 = LocalDateTime.of(2001, 1, 1, 10, 0, 0);
  private static final LocalDateTime IN_2002 = LocalDateTime.of(2002, 2, 2, 10, 0, 0);
  private static final LocalDateTime IN_2003 = LocalDateTime.of(2003, 3, 3, 10, 0, 0);
  private static final LocalDateTime IN_2004 = LocalDateTime.of(2004, 4, 4, 10, 0, 0);
  private static final LocalDateTime IN_2005 = LocalDateTime.of(2005, 5, 5, 10, 0, 0);
  private static final LocalDateTime IN_2006 = LocalDateTime.of(2006, 6, 6, 10, 0, 0);

  private static Database database;
  private static Schema schema;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    schema = database.dropCreateSchema(TestMgColumns.class.getSimpleName());
    if (!database.hasUser(EDITOR_USER)) database.addUser(EDITOR_USER);
    schema.addMember(EDITOR_USER, Privileges.EDITOR.toString());
  }

  /** Reads back the single row these tests keep in a table. */
  private static Row onlyRow(Table table) {
    return table.retrieveRows().getFirst();
  }

  private static void assertInsertedMetadata(Row row, String by, LocalDateTime on) {
    assertEquals(by, row.getString(MG_INSERTEDBY));
    assertEquals(on, row.getDateTime(MG_INSERTEDON));
  }

  private static void assertUpdatedMetadata(Row row, String by, LocalDateTime on) {
    assertEquals(by, row.getString(MG_UPDATEDBY));
    assertEquals(on, row.getDateTime(MG_UPDATEDON));
  }

  /** Insert and update metadata should be identical and filled in right after an insert. */
  private static void assertMetadataOfFreshInsert(Row row) {
    assertNotNull(row.getDateTime(MG_INSERTEDON));
    assertNotNull(row.getString(MG_INSERTEDBY));
    assertEquals(row.getDateTime(MG_INSERTEDON), row.getDateTime(MG_UPDATEDON));
    assertEquals(row.getString(MG_INSERTEDBY), row.getString(MG_UPDATEDBY));
  }

  @Test
  void testMgDraft() {
    Table plain =
        schema.create(
            table(
                "MgDraft",
                column("id").setPkey(),
                column("required").setRequired(true),
                column("notrequired")));

    assertRequiredIsEnforcedUnlessDraft(plain);

    assertEquals(1, plain.retrieveRows().size());
    assertEquals("somevalue2", onlyRow(plain).getString("notrequired"));

    // to make sure also test with subclass
    schema.create(table("MgDraftSuper", column("id").setPkey()));
    Table subclass =
        schema.create(
            table("MgDraftSub", column("required").setRequired(true), column("notrequired"))
                .setInheritName("MgDraftSuper"));

    assertRequiredIsEnforcedUnlessDraft(subclass);
  }

  private static void assertRequiredIsEnforcedUnlessDraft(Table table) {
    assertThrows(
        Exception.class,
        () -> table.insert(row("id", 1, "notrequired", "somevalue1")),
        "insert without a required value should fail");

    assertDoesNotThrow(
        () -> table.insert(row("id", 1, "notrequired", "somevalue2").setDraft(true)),
        "insert without a required value should succeed as draft");
  }

  @Test
  void testUpdatedOn() {
    Table t = schema.create(table("UpdatedOn", column("id").setPkey()));
    t.insert(row("id", 1));
    assertUpdatedOnMovesForwardOnUpdate(t);

    // to make sure also test with subclass
    Table subclass = schema.create(table("UpdatedOnSub").setInheritName("UpdatedOn"));
    subclass.insert(row("id", 2));
    assertUpdatedOnMovesForwardOnUpdate(subclass);
  }

  private static void assertUpdatedOnMovesForwardOnUpdate(Table table) {
    Row row = onlyRow(table);
    assertMetadataOfFreshInsert(row);

    // update without mg values, so they are set to the active user and the current time
    row.clear(MG_UPDATEDBY);
    row.clear(MG_UPDATEDON);
    table.update(row);

    Row updated = onlyRow(table);
    assertTrue(updated.getDateTime(MG_INSERTEDON).isBefore(updated.getDateTime(MG_UPDATEDON)));
  }

  @Test
  void testSaveKeepsInsertedMetadata() {
    Table t = schema.create(table("SavedOn", column("id").setPkey(), column("value")));
    assertSaveKeepsInsertedMetadata(t);
  }

  @Test
  void testSaveKeepsInsertedMetadataInSubclass() {
    schema.create(table("SavedOnSuper", column("id").setPkey(), column("value")));
    Table subclass = schema.create(table("SavedOnSub").setInheritName("SavedOnSuper"));
    assertSaveKeepsInsertedMetadata(subclass);
  }

  private static void assertSaveKeepsInsertedMetadata(Table table) {
    table.insert(row("id", 1, "value", "somevalue1"));
    Row inserted = onlyRow(table);

    table.save(row("id", 1, "value", "somevalue2"));

    Row saved = onlyRow(table);
    assertEquals("somevalue2", saved.getString("value"));
    assertInsertedMetadata(
        saved, inserted.getString(MG_INSERTEDBY), inserted.getDateTime(MG_INSERTEDON));
    assertTrue(saved.getDateTime(MG_INSERTEDON).isBefore(saved.getDateTime(MG_UPDATEDON)));
  }

  @Test
  void testProvidedMgValuesAreUsedOnInsertUpdateAndSave() {
    Table t = schema.create(table("ProvidedMg", column("id").setPkey(), column("value")));

    t.insert(
        row("id", 1, "value", "somevalue1")
            .set(MG_INSERTEDBY, "importer1")
            .set(MG_INSERTEDON, IN_2001)
            .set(MG_UPDATEDBY, "importer2")
            .set(MG_UPDATEDON, IN_2002));

    Row inserted = onlyRow(t);
    assertInsertedMetadata(inserted, "importer1", IN_2001);
    assertUpdatedMetadata(inserted, "importer2", IN_2002);

    // update with provided values
    t.update(
        row("id", 1, "value", "somevalue2")
            .set(MG_UPDATEDBY, "importer3")
            .set(MG_UPDATEDON, IN_2003));

    Row updated = onlyRow(t);
    assertEquals("somevalue2", updated.getString("value"));
    assertInsertedMetadata(updated, "importer1", IN_2001);
    assertUpdatedMetadata(updated, "importer3", IN_2003);

    // update can also override the inserted metadata
    t.update(
        row("id", 1, "value", "somevalue2b")
            .set(MG_INSERTEDBY, "importer3b")
            .set(MG_INSERTEDON, IN_2005));

    assertInsertedMetadata(onlyRow(t), "importer3b", IN_2005);

    // save on an existing row (insert on conflict) with provided values
    t.save(
        row("id", 1, "value", "somevalue3")
            .set(MG_INSERTEDBY, "importer4b")
            .set(MG_INSERTEDON, IN_2006)
            .set(MG_UPDATEDBY, "importer4")
            .set(MG_UPDATEDON, IN_2004));

    Row saved = onlyRow(t);
    assertEquals("somevalue3", saved.getString("value"));
    assertInsertedMetadata(saved, "importer4b", IN_2006);
    assertUpdatedMetadata(saved, "importer4", IN_2004);

    // without provided values the active user and current time are applied again
    t.update(row("id", 1, "value", "somevalue4"));

    Row fallback = onlyRow(t);
    assertEquals(schema.getDatabase().getActiveUser(), fallback.getString(MG_UPDATEDBY));
    assertTrue(fallback.getDateTime(MG_UPDATEDON).isAfter(IN_2004));
    assertInsertedMetadata(fallback, "importer4b", IN_2006);
  }

  @Test
  void testProvidedMgValuesAreIgnoredWithoutManagePermission() {
    schema.create(table("ProvidedMgAsEditor", column("id").setPkey(), column("value")));

    try {
      database.setActiveUser(EDITOR_USER);
      Table editorTable = database.getSchema(schema.getName()).getTable("ProvidedMgAsEditor");

      editorTable.insert(
          row("id", 1, "value", "somevalue1")
              .set(MG_INSERTEDBY, "importer1")
              .set(MG_INSERTEDON, IN_2001)
              .set(MG_UPDATEDBY, "importer2")
              .set(MG_UPDATEDON, IN_2001));

      Row inserted = onlyRow(editorTable);
      assertEquals(EDITOR_USER, inserted.getString(MG_INSERTEDBY));
      assertEquals(EDITOR_USER, inserted.getString(MG_UPDATEDBY));
      assertTrue(inserted.getDateTime(MG_INSERTEDON).isAfter(IN_2001));
      assertTrue(inserted.getDateTime(MG_UPDATEDON).isAfter(IN_2001));

      // same for update and for save on an existing row
      editorTable.update(
          row("id", 1, "value", "somevalue2")
              .set(MG_INSERTEDBY, "importer1")
              .set(MG_UPDATEDBY, "importer2"));

      Row updated = onlyRow(editorTable);
      assertEquals("somevalue2", updated.getString("value"));
      assertEquals(EDITOR_USER, updated.getString(MG_INSERTEDBY));
      assertEquals(EDITOR_USER, updated.getString(MG_UPDATEDBY));

      editorTable.save(
          row("id", 1, "value", "somevalue3")
              .set(MG_INSERTEDBY, "importer1")
              .set(MG_INSERTEDON, IN_2001));

      Row saved = onlyRow(editorTable);
      assertEquals("somevalue3", saved.getString("value"));
      assertEquals(EDITOR_USER, saved.getString(MG_INSERTEDBY));
      assertTrue(saved.getDateTime(MG_INSERTEDON).isAfter(IN_2001));
    } finally {
      database.becomeAdmin();
    }
  }
}
