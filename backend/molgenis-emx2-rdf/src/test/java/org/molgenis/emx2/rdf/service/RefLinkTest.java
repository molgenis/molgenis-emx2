package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

class RefLinkTest extends RdfServiceTestRunner {
  private static final String SCHEMA_NAME = RefLinkTest.class.getSimpleName();

  static Schema refLinkTest;

  @BeforeAll
  static void beforeAll() {
    refLinkTest = database.dropCreateSchema(SCHEMA_NAME);

    refLinkTest.create(
        table("table1", column("id").setType(ColumnType.STRING).setPkey()),
        table(
            "table2",
            column("id1").setPkey().setType(ColumnType.REF).setRefTable("table1"),
            column("id2").setType(ColumnType.STRING).setPkey()),
        table(
            "table3",
            column("p1").setPkey().setType(ColumnType.REF).setRefTable("table1"),
            column("p2").setPkey().setType(ColumnType.REF).setRefTable("table2").setRefLink("p1"),
            column("ref").setType(ColumnType.REF).setRefTable("table2").setRefLink("p1"),
            column("ref_array")
                .setType(ColumnType.REF_ARRAY)
                .setRefTable("table2")
                .setRefLink("p1")));

    refLinkTest.getTable("table1").insert(row("id", "t1First"));
    refLinkTest.getTable("table2").insert(row("id1", "t1First", "id2", "t2First"));
    refLinkTest.getTable("table2").insert(row("id1", "t1First", "id2", "t2Second"));
    refLinkTest.getTable("table2").insert(row("id1", "t1First", "id2", "t2Third"));
    refLinkTest.getTable("table2").insert(row("id1", "t1First", "id2", "t2Fourth"));
    refLinkTest
        .getTable("table3")
        .insert(
            row(
                "p1",
                "t1First",
                "p2",
                "t2First",
                "ref",
                "t2Second",
                "ref_array",
                "t2Third,t2Fourth"));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testRefLinkWorks() {
    assertDoesNotThrow(() -> parseSchemaRdf(refLinkTest));
  }

  @Test
  void testRefLinkRef() throws IOException {
    Set<IRI> expectedRefArrayObjects =
        Set.of(Values.iri(getApi(refLinkTest) + "Table2/id1=t1First&id2=t2Second"));

    InMemoryRDFHandler handler = parseRowRdf(refLinkTest, "table3", "p1=t1First&p2=t2First");
    Set<Value> actualSubjects =
        handler
            .resources
            .get(Values.iri(getApi(refLinkTest) + "Table3/p1=t1First&p2=t2First"))
            .get(Values.iri(getApi(refLinkTest) + "Table3/column/ref"));

    assertEquals(expectedRefArrayObjects, actualSubjects);
  }

  /**
   * Tests that all subjects have full correct IRI (instead of only 1)
   *
   * @throws IOException
   */
  @Test
  void testRefLinkRefArray() throws IOException {
    Set<IRI> expectedRefArrayObjects =
        Set.of(
            Values.iri(getApi(refLinkTest) + "Table2/id1=t1First&id2=t2Third"),
            Values.iri(getApi(refLinkTest) + "Table2/id1=t1First&id2=t2Fourth"));

    InMemoryRDFHandler handler = parseRowRdf(refLinkTest, "table3", "p1=t1First&p2=t2First");
    Set<Value> actualSubjects =
        handler
            .resources
            .get(Values.iri(getApi(refLinkTest) + "Table3/p1=t1First&p2=t2First"))
            .get(Values.iri(getApi(refLinkTest) + "Table3/column/ref_array"));

    assertEquals(expectedRefArrayObjects, actualSubjects);
  }
}
