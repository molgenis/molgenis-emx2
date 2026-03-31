package org.molgenis.emx2.harvester.semantics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TableSemanticsTest {

  @Test
  void shouldMapTableColumnsWithSemantics() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName());
    TableMetadata table1 =
        TableMetadata.table("a")
            .setSemantics("a-table")
            .add(
                Column.column("column1").setSemantics("http://localhost/table1-column1-semantic"),
                Column.column("column2").setSemantics("http://localhost/table1-column2-semantic"),
                Column.column("column3")
                    .setSemantics("http://localhost/table1-column3-semantic")
                    .setComputed("foo"),
                Column.column("column4"));

    TableMetadata table2 =
        TableMetadata.table("b")
            .setSemantics("a-table")
            .add(
                Column.column("column1").setSemantics("http://localhost/table2-column1-semantic"),
                Column.column("column2"));

    schema.getMetadata().create(table1, table2);

    Map<String, Map<String, String>> expectedMapping =
        Map.of(
            "a",
                Map.of(
                    "http://localhost/table1-column1-semantic",
                    "column1",
                    "http://localhost/table1-column2-semantic",
                    "column2"),
            "b", Map.of("http://localhost/table2-column1-semantic", "column1"));

    TableSemantics tableSemantics = new TableSemantics(schema);

    for (String tableName : expectedMapping.keySet()) {
      Map<String, TableSemantics.ColumnMapping> tableMapping =
          tableSemantics.getMappingsForTableColumns(tableName);

      Set<String> semantics = tableMapping.keySet();
      assertEquals(expectedMapping.get(tableName).keySet(), semantics);

      for (String semantic : semantics) {
        String expectedColumn = expectedMapping.get(tableName).get(semantic);
        assertEquals(expectedColumn, tableMapping.get(semantic).column().getName());
      }
    }
  }
}
