package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestSelectiveImport {

  private Schema schema;

  @BeforeEach
  void setupSchema() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestSelectiveImport.class.getSimpleName());
  }

  @Test
  void givenSelectionOfColumns_thenInsertOnlySelectedColumn() {
    Table table =
        schema.create(
            TableMetadata.table(
                "person",
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("first_name").setType(ColumnType.STRING),
                Column.column("last_name").setType(ColumnType.STRING),
                Column.column("nickname").setType(ColumnType.STRING)));

    Row row =
        Row.row(
            "id", "1234", "first_name", "John", "last_name", "doe", "nickname", "The one and only");

    int insert =
        table.insert(
            List.of(row), Set.of("id", "first_name", "last_name", Constants.MG_INSERTEDBY));
    assertEquals(1, insert);

    List<Row> rows = table.retrieveRows();
    HashMap<Object, Object> map = new HashMap<>();
    map.put("id", "1234");
    map.put("first_name", "John");
    map.put("last_name", "doe");
    map.put("nickname", null);
    map.put(Constants.MG_DRAFT, null);
    map.put(Constants.MG_INSERTEDBY, "admin");
    map.put(Constants.MG_INSERTEDON, null);
    map.put(Constants.MG_UPDATEDBY, null);
    map.put(Constants.MG_UPDATEDON, null);
    assertEquals(rows.getFirst().getValueMap(), map);
  }

  @Test
  void givenInheritedTable_whenSelectionOfColumns_thenInsertOnlySelectedColumns() {
    schema.create(
        TableMetadata.table(
            "food",
            Column.column("name").setType(ColumnType.STRING).setPkey(),
            Column.column("flavour profile").setType(ColumnType.STRING)),
        TableMetadata.table("cake", Column.column("frosting").setType(ColumnType.STRING))
            .setInheritName("food"));
    Table table = schema.getTable("cake");

    Row row = Row.row("name", "Carrot cake", "flavour_profile", "sweet", "frosting", "carrot");

    int insert = table.insert(List.of(row), Set.of("name", "frosting", Constants.MG_INSERTEDBY));
    assertEquals(1, insert);

    HashMap<Object, Object> map = new HashMap<>();
    map.put("name", "Carrot cake");
    map.put("frosting", "carrot");
    map.put("flavour profile", null);
    map.put(Constants.MG_DRAFT, null);
    map.put(Constants.MG_INSERTEDBY, "admin");
    map.put(Constants.MG_INSERTEDON, null);
    map.put(Constants.MG_UPDATEDBY, null);
    map.put(Constants.MG_UPDATEDON, null);
    map.put(Constants.MG_TABLECLASS, null);

    List<Row> rows = table.retrieveRows();
    assertEquals(rows.getFirst().getValueMap(), map);
  }
}
