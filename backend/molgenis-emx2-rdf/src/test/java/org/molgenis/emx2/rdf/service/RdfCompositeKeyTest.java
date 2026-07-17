package org.molgenis.emx2.rdf.service;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.RdfValidator;

public class RdfCompositeKeyTest extends RdfServiceTest {
  private static final String SCHEMA_NAME = RdfCompositeKeyTest.class.getSimpleName();

  static Schema compositeKeyTest;

  private static final Resource COMP_ROOT1_FIRST =
      getIri("Root1/r1.c1a=c1a_first&r1.c1b.gc1a=gc1a_first&r1.c1b.gc1b=gc1b_first");
  private static final Resource COMP_ROOT2_FIRST =
      getIri(
          "Root2/r2a=r2a_first&r2b.c1a=c1a_second&r2b.c1b.gc1a=gc1a_first&r2b.c1b.gc1b=gc1b_first");
  private static final Resource COMP_CHILD1_FIRST =
      getIri("Child1/c1a=c1a_first&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first");
  private static final Resource COMP_CHILD1_SECOND =
      getIri("Child1/c1a=c1a_second&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first");
  private static final Resource COMP_GRANDCHILD1_FIRST =
      getIri("Grandchild1/gc1a=gc1a_first&gc1b=gc1b_first");
  private static final Resource COMP_GRANDCHILD1_SECOND =
      getIri("Grandchild1/gc1a=gc1a_second&gc1b=gc1b_second");

  private static final Triple COMP_ROOT1_KEY_REF =
      getTriple(COMP_ROOT1_FIRST, "Root1/column/r1", COMP_CHILD1_FIRST);
  private static final Triple COMP_ROOT2_KEY_REF =
      getTriple(COMP_ROOT2_FIRST, "Root2/column/r2b", COMP_CHILD1_SECOND);
  private static final Triple COMP_CHILD1_FIRST_KEY_REF =
      getTriple(COMP_CHILD1_FIRST, "Child1/column/c1b", COMP_GRANDCHILD1_FIRST);
  private static final Triple COMP_CHILD1_FIRST_REFBACK_ROOT1 =
      getTriple(COMP_CHILD1_FIRST, "Child1/column/root1refback", COMP_ROOT1_FIRST);
  private static final Triple COMP_CHILD1_SECOND_KEY_REF =
      getTriple(COMP_CHILD1_SECOND, "Child1/column/c1b", COMP_GRANDCHILD1_FIRST);
  private static final Triple COMP_CHILD1_SECOND_REFBACK_ROOT2 =
      getTriple(COMP_CHILD1_SECOND, "Child1/column/root2refback", COMP_ROOT2_FIRST);
  private static final Triple COMP_CHILD1_SECOND_NON_KEY_REF =
      getTriple(COMP_CHILD1_SECOND, "Child1/column/grandchild1ref", COMP_GRANDCHILD1_SECOND);
  private static final Triple COMP_GRANDCHILD_REFBACK_1 =
      getTriple(COMP_GRANDCHILD1_FIRST, "Grandchild1/column/child1refback", COMP_CHILD1_FIRST);
  private static final Triple COMP_GRANDCHILD_REFBACK_2 =
      getTriple(COMP_GRANDCHILD1_FIRST, "Grandchild1/column/child1refback", COMP_CHILD1_FIRST);

  static IRI getIri(String keyString) {
    return getIri(SCHEMA_NAME, keyString);
  }

  static Triple getTriple(Resource subject, String predicateLocalPart, Value object) {
    return getTriple(subject, getIri(predicateLocalPart), object);
  }

  @BeforeAll
  static void beforeAll() {
    compositeKeyTest = database.dropCreateSchema(SCHEMA_NAME);
    compositeKeyTest.create(
        table("root1", column("r1").setType(ColumnType.REF).setRefTable("child1").setPkey()),
        table(
            "root2",
            column("r2a").setPkey(),
            column("r2b").setType(ColumnType.REF).setRefTable("child1").setPkey()),
        table(
            "child1",
            column("c1a").setPkey(),
            column("c1b").setType(ColumnType.REF).setRefTable("grandchild1").setPkey(),
            column("grandchild1ref").setType(ColumnType.REF).setRefTable("grandchild1"),
            column("root1refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("root1")
                .setRefBack("r1"),
            column("root2refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("root2")
                .setRefBack("r2b")),
        table(
            "grandchild1",
            column("gc1a").setPkey(),
            column("gc1b").setPkey(),
            column("child1refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("child1")
                .setRefBack("c1b")));

    compositeKeyTest
        .getTable("grandchild1")
        .insert(
            row("gc1a", "gc1a_first", "gc1b", "gc1b_first"),
            row("gc1a", "gc1a_second", "gc1b", "gc1b_second"));

    compositeKeyTest
        .getTable("child1")
        .insert(
            row("c1a", "c1a_first", "c1b.gc1a", "gc1a_first", "c1b.gc1b", "gc1b_first"),
            row(
                "c1a",
                "c1a_second",
                "c1b.gc1a",
                "gc1a_first",
                "c1b.gc1b",
                "gc1b_first",
                "grandchild1ref.gc1a",
                "gc1a_second",
                "grandchild1ref.gc1b",
                "gc1b_second"));

    compositeKeyTest
        .getTable("root1")
        .insert(
            row("r1.c1a", "c1a_first", "r1.c1b.gc1a", "gc1a_first", "r1.c1b.gc1b", "gc1b_first"));

    compositeKeyTest
        .getTable("root2")
        .insert(
            row(
                "r2a",
                "r2a_first",
                "r2b.c1a",
                "c1a_second",
                "r2b.c1b.gc1a",
                "gc1a_first",
                "r2b.c1b.gc1b",
                "gc1b_first"));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testCompositeKeysPresenceOnFullSchema() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(compositeKeyTest);

    new RdfValidator()
        .add(COMP_ROOT1_KEY_REF, true)
        .add(COMP_ROOT2_KEY_REF, true)
        .add(COMP_CHILD1_FIRST_KEY_REF, true)
        .add(COMP_CHILD1_FIRST_REFBACK_ROOT1, true)
        .add(COMP_CHILD1_SECOND_KEY_REF, true)
        .add(COMP_CHILD1_SECOND_REFBACK_ROOT2, true)
        .add(COMP_CHILD1_SECOND_NON_KEY_REF, true)
        .add(COMP_GRANDCHILD_REFBACK_1, true)
        .add(COMP_GRANDCHILD_REFBACK_2, true)
        .validate(handler);
  }

  @Test
  void testCompositeKeysRowSelection() throws IOException {
    InMemoryRDFHandler handler =
        parseRowRdf(
            compositeKeyTest, "Child1", "c1a=c1a_second&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first");

    new RdfValidator()
        .add(COMP_ROOT1_KEY_REF, false)
        .add(COMP_ROOT2_KEY_REF, false)
        .add(COMP_CHILD1_FIRST_KEY_REF, false)
        .add(COMP_CHILD1_FIRST_REFBACK_ROOT1, false)
        .add(COMP_CHILD1_SECOND_KEY_REF, true)
        .add(COMP_CHILD1_SECOND_REFBACK_ROOT2, true)
        .add(COMP_CHILD1_SECOND_NON_KEY_REF, true)
        .add(COMP_GRANDCHILD_REFBACK_1, false)
        .add(COMP_GRANDCHILD_REFBACK_2, false)
        .validate(handler);
  }
}
