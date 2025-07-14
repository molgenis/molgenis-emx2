package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.ONTOLOGY;
import static org.molgenis.emx2.ColumnType.ONTOLOGY_ARRAY;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestQueryOntologySearchQueryExpansion {
  static Database database;
  static Schema schema;
  static final String MY_TABLE = "MY_TABLE";
  static Table myTable;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema("TestQueryOntologySearchQueryExpansion");

    myTable =
        schema.create(
            table(MY_TABLE)
                .add(column("ID").setType(INT).setPkey())
                .add(column("Single_Ont_Value").setType(ONTOLOGY).setRefTable("CodeTable"))
                .add(column("Array_OntValue").setType(ONTOLOGY_ARRAY).setRefTable("CodeTable")));

    schema
        .getTable("CodeTable")
        .insert(
            new Row().setString("name", "name1").setString("code", "red"),
            new Row().setString("name", "name2").setString("code", "green"),
            new Row().setString("name", "name3").setString("code", "blue"));

    myTable.insert(new Row().setInt("ID", 1).setString("Single_Ont_Value", "name1"));

    myTable.insert(new Row().setInt("ID", 2).setStringArray("Array_OntValue", "name2", "name3"));
  }

  @Test
  void testOntologySearchExpansionForSingleOntologyValue() {
    Query q = schema.query(MY_TABLE, s("ID")).search("red");
    assertEquals("{\"MY_TABLE\": [{\"iD\": 1}]}", q.retrieveJSON());
  }

  @Test
  void testOntologySearchExpansionForArrayOntologyValue() {
    Query q = schema.query(MY_TABLE, s("ID")).search("green");
    assertEquals("{\"MY_TABLE\": [{\"iD\": 2}]}", q.retrieveJSON());
  }
}
