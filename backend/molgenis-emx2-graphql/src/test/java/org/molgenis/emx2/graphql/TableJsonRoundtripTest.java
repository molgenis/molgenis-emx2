package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.json.JsonUtil;

class TableJsonRoundtripTest {

  @Test
  void jsonRoundtripPreservesAllParentsOfDiamondChild() throws IOException {
    SchemaMetadata source = new SchemaMetadata();
    source.create(
        table("A").add(column("id").setType(STRING).setPkey()).add(column("aCol").setType(STRING)));
    source.create(table("B").setInheritNames("A").add(column("bCol").setType(STRING)));
    source.create(table("C").setInheritNames("A").add(column("cCol").setType(STRING)));
    source.create(table("D").setInheritNames("B", "C").add(column("dCol").setType(STRING)));

    String json = JsonUtil.schemaToJson(source);
    SchemaMetadata restored = JsonUtil.jsonToSchema(json);

    TableMetadata restoredD = restored.getTableMetadata("D");
    assertNotNull(restoredD, "Table D must survive JSON round-trip");
    List<String> inheritNames = restoredD.getInheritNames();
    assertTrue(
        inheritNames.containsAll(List.of("B", "C")),
        "After JSON round-trip, D must have both B and C as parents, got: " + inheritNames);
  }

  @Test
  void legacySingularInheritNameYieldsInheritance() throws IOException {
    String legacyJson =
        "{\"tables\":["
            + "{\"name\":\"Pet\",\"columns\":[{\"name\":\"id\",\"columnType\":\"STRING\",\"key\":1}]},"
            + "{\"name\":\"Dog\",\"inheritName\":\"Pet\",\"columns\":[{\"name\":\"breed\",\"columnType\":\"STRING\"}]}"
            + "]}";

    SchemaMetadata restored = JsonUtil.jsonToSchema(legacyJson);

    TableMetadata dog = restored.getTableMetadata("Dog");
    assertNotNull(dog, "Table Dog must be parsed from legacy JSON");
    assertEquals(
        List.of("Pet"),
        dog.getInheritNames(),
        "Legacy singular inheritName must still yield inheritance, got: " + dog.getInheritNames());
  }
}
