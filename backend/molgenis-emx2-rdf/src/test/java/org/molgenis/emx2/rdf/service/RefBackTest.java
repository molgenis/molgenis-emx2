package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.Set;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

/** Uses own schema due to pet store using auto id */
class RefBackTest extends RdfServiceTest {
  private static final String SCHEMA_NAME = RefLinkTest.class.getSimpleName();

  static Schema refBackTest;

  @BeforeAll
  static void beforeAll() {
    refBackTest = database.dropCreateSchema(SCHEMA_NAME);
    refBackTest.create(
        table(
            "tableRef",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("link").setType(ColumnType.REF).setRefTable("tableRefBack")),
        table("tableRefBack", column("id").setType(ColumnType.STRING).setPkey()));
    refBackTest
        .getTable("tableRefBack")
        .getMetadata()
        .add(
            column("backlink")
                .setType(ColumnType.REFBACK)
                .setRefTable("tableRef")
                .setRefBack("link"));

    refBackTest.getTable("tableRefBack").insert(row("id", "a"));
    refBackTest.getTable("tableRef").insert(row("id", "1", "link", "a"));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void refBackInRdf() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(refBackTest);

    Set<Value> refBacks =
        handler
            .resources
            .get(Values.iri(getApi(refBackTest) + "TableRefBack/id=a"))
            .get(Values.iri(getApi(refBackTest) + "TableRefBack/column/backlink"));
    assertEquals(Set.of(Values.iri(getApi(refBackTest) + "TableRef/id=1")), refBacks);
  }
}
