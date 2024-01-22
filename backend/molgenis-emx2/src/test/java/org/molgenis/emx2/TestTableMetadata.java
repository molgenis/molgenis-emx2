package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TestTableMetadata {
  @Test
  void testGetColumnsFromSubclasses() {
    SchemaMetadata s =
        new SchemaMetadata()
            .create(
                table("Person", column("name")),
                table("Employee", column("details").setType(ColumnType.HEADING), column("salary"))
                    .setInheritName("Person"));

    List<Column> result = s.getTableMetadata("Person").getColumnsIncludingSubclasses();
    assertEquals(3, result.size());

    Column salary = result.get(2);
    assertEquals("salary", salary.getName());
    assertEquals("Employee", salary.getTableName());

    result = s.getTableMetadata("Person").getColumnsIncludingSubclassesExcludingHeadings();
    assertEquals(2, result.size());
  }
}
