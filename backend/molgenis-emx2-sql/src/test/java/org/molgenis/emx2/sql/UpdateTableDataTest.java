package org.molgenis.emx2.sql;

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
    assert persons.retrieveRows().size() == 1;
    assert persons.retrieveRows().getFirst().getString("id").equals("p1");
    assert persons.retrieveRows().getFirst().getString("name").equals("Joop");
    assert persons.retrieveRows().getFirst().getInteger("age") == 30;

    // update data, name value is passed, so it should remain the same
    persons.update(Row.row("id", "p1", "age", 31));

    // verify updated data
    assert persons.retrieveRows().size() == 1;
    assert persons.retrieveRows().getFirst().getString("id").equals("p1");
    assert persons.retrieveRows().getFirst().getString("name").equals("Joop");
    assert persons.retrieveRows().getFirst().getInteger("age") == 31;
  }

  @Test
  void testSaveTableData() {
    Table persons = createPersonsTable();

    persons.insert(Row.row("id", "p1", "name", "Joop", "age", 30));

    // verify initial data
    assert persons.retrieveRows().size() == 1;
    assert persons.retrieveRows().getFirst().getString("id").equals("p1");
    assert persons.retrieveRows().getFirst().getString("name").equals("Joop");
    assert persons.retrieveRows().getFirst().getInteger("age") == 30;

    // save data
    persons.save(Row.row("id", "p1", "age", 31));

    // verify inserted data
    assert persons.retrieveRows().size() == 1;
    assert persons.retrieveRows().getFirst().getString("id").equals("p1");
    assert persons.retrieveRows().getFirst().getString("name") == null;
    assert persons.retrieveRows().getFirst().getInteger("age") == 31;
  }

  private Table createPersonsTable() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
    return schema.create(
        table("Person")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("age").setType(ColumnType.INT)));
  }
}
