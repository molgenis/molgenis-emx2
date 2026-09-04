package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class UpdateVersusSaveTableDataTest {

  @Test
  void testUpdateTableData() {
    Table persons = createPersonsTable();

    persons.insert(Row.row("id", "p1", "name", "Joop", "age", 30));

    // verify initial data
    assertEquals(1, persons.retrieveRows().size());
    assertEquals("p1", persons.retrieveRows().getFirst().getString("id"));
    assertEquals("Joop", persons.retrieveRows().getFirst().getString("name"));
    assertEquals(30, persons.retrieveRows().getFirst().getInteger("age"));

    // update data, name value is passed, so it should remain the same
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

    assertEquals(31, personTable.retrieveRows().getFirst().getInteger("age"));
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
