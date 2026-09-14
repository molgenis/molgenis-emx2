package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Query.Option.INCLUDE_FILE_CONTENTS;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TestResourceLoader.getFile;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class UpdateVersusSaveTableDataTest {

  private static final int MOLGENIS_PNG_SIZE = 37458;

  @Test
  void testUpdateTableData() {
    Table persons = createPersonsTable();

    persons.insert(Row.row("id", "p1", "name", "Joop", "age", 30));

    // verify initial data
    assertEquals(1, persons.retrieveRows().size());
    assertEquals("p1", persons.retrieveRows().getFirst().getString("id"));
    assertEquals("Joop", persons.retrieveRows().getFirst().getString("name"));
    assertEquals(30, persons.retrieveRows().getFirst().getInteger("age"));

    // update data, no name value is passed, so it should remain the same
    persons.update(Row.row("id", "p1", "age", 31));

    // verify updated data
    assertEquals(1, persons.retrieveRows().size());
    assertEquals("p1", persons.retrieveRows().getFirst().getString("id"));
    assertEquals("Joop", persons.retrieveRows().getFirst().getString("name"));
    assertEquals(31, persons.retrieveRows().getFirst().getInteger("age"));
  }

  @Test
  void testSaveTableData() {
    Table persons = createPersonsTable();

    persons.insert(Row.row("id", "p1", "name", "Joop", "age", 30));

    // verify initial data
    assertEquals(1, persons.retrieveRows().size());
    assertEquals("p1", persons.retrieveRows().getFirst().getString("id"));
    assertEquals("Joop", persons.retrieveRows().getFirst().getString("name"));
    assertEquals(30, persons.retrieveRows().getFirst().getInteger("age"));

    // save data
    persons.save(Row.row("id", "p1", "age", 31));

    // verify inserted data
    assertEquals(1, persons.retrieveRows().size());
    assertEquals("p1", persons.retrieveRows().getFirst().getString("id"));
    assertNull(persons.retrieveRows().getFirst().getString("name"));
    assertEquals(31, persons.retrieveRows().getFirst().getInteger("age"));
  }

  @Test
  void testUpdateTableDataWithRefColumn() {
    Table personTable = createTableWithRefColumn();

    // verify initial data
    assertEquals(1, personTable.retrieveRows().size());

    // make a column update
    personTable.update(Row.row("id", "p1", "age", 31));

    // verify updated data
    assertEquals(1, personTable.retrieveRows().size());
    assertEquals("red", personTable.retrieveRows().getFirst().getString("favoriteColor"));
    assertEquals(31, personTable.retrieveRows().getFirst().getInteger("age"));
  }

  @Test
  void testUpdateTableDataWithoutFileColumnKeepsFile() {
    Table personTable = createTableWithFileColumn();

    // verify initial data
    assertEquals(1, personTable.retrieveRows().size());

    // make a column update that does not mention the file column
    personTable.update(Row.row("id", "p1", "name", "Joop"));

    // verify updated data, the file should be untouched
    assertEquals(1, personTable.retrieveRows().size());
    Row updated = personTable.retrieveRows(INCLUDE_FILE_CONTENTS).getFirst();
    assertEquals("Joop", updated.getString("name"));
    assertEquals("molgenis.png", updated.getString("foto_filename"));
    assertEquals("png", updated.getString("foto_extension"));
    assertEquals(MOLGENIS_PNG_SIZE, updated.getInteger("foto_size"));
    assertEquals(MOLGENIS_PNG_SIZE, updated.getBinary("foto_contents").length);
  }

  @Test
  void testUpdateTableDataWithFileColumnReplacesFile() {
    Table personTable = createTableWithFileColumn();

    byte[] contents = "a replacement".getBytes(UTF_8);
    personTable.update(
        Row.row("id", "p1")
            .setBinary("foto", new BinaryFileWrapper("text/plain", "replacement.txt", contents)));

    // verify updated data, the file should be replaced
    assertEquals(1, personTable.retrieveRows().size());
    Row updated = personTable.retrieveRows(INCLUDE_FILE_CONTENTS).getFirst();
    assertEquals("replacement.txt", updated.getString("foto_filename"));
    assertEquals("txt", updated.getString("foto_extension"));
    assertEquals(contents.length, updated.getInteger("foto_size"));
    assertArrayEquals(contents, updated.getBinary("foto_contents"));
  }

  @Test
  void testUpdateTableDataOnInheritedTableKeepsRequiredParentColumn() {
    // setup
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema("UpdatePartialInheritedTable");
    schema.create(
        table("Person").add(column("id").setPkey()).add(column("email").setRequired(true)));
    Table employee =
        schema.create(
            table("Employee")
                .setInheritName("Person")
                .add(column("salary").setType(ColumnType.INT)));

    employee.insert(Row.row("id", "p1", "email", "known@example.com", "salary", 100));

    // update data, no email value is passed, so it should remain the same
    employee.update(Row.row("id", "p1", "salary", 200));

    assertEquals(1, employee.retrieveRows().size());
    assertEquals(200, employee.retrieveRows().getFirst().getInteger("salary"));
    assertEquals("known@example.com", employee.retrieveRows().getFirst().getString("email"));
  }

  private Table createPersonsTable() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(this.getClass().getSimpleName());
    return schema.create(
        table("Person")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("age").setType(ColumnType.INT)));
  }

  private Table createTableWithFileColumn() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(this.getClass().getSimpleName());
    Table personTable =
        schema.create(
            table("Person")
                .add(column("id").setPkey())
                .add(column("name"))
                .add(column("foto").setType(ColumnType.FILE)));

    personTable.insert(
        Row.row("id", "p1", "name", "Henk", "foto", getFile("testfiles/molgenis.png")));

    return personTable;
  }

  private Table createTableWithRefColumn() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(this.getClass().getSimpleName());
    Table colorsTable = schema.create(table("Colors").add(column("name").setPkey()));
    colorsTable.insert(Row.row("name", "red"), Row.row("name", "green"), Row.row("name", "blue"));
    Table personTable =
        schema.create(
            table("Person")
                .add(column("id").setPkey())
                .add(column("name"))
                .add(column("age").setType(ColumnType.INT))
                .add(
                    column("favoriteColor")
                        .setType(ColumnType.RADIO)
                        .setRefTable("Colors")
                        .setRequired(true)));

    personTable.insert(Row.row("id", "p1", "age", 30, "name", "Joop", "favoriteColor", "red"));

    return personTable;
  }
}
