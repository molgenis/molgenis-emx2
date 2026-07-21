package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.List;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.RdfValidator;

class TableInheritanceTest extends RdfServiceTestRunner {
  private static final String SCHEMA_NAME = TableInheritanceTest.class.getSimpleName();
  private static final String EXTERNAL_SCHEMA_NAME = SCHEMA_NAME + "_EXTERNAL";

  static Schema tableInherTest;
  static Schema tableInherExtTest;

  private static final Triple INHER_ID1 =
      getTriple(SCHEMA_NAME, "Root/id=1", "Root/column/rootColumn", "id1 data");
  private static final Triple INHER_ID2 =
      getTriple(SCHEMA_NAME, "Root/id=2", "Child/column/childColumn", "id2 data");
  private static final Triple INHER_ID3 =
      getTriple(SCHEMA_NAME, "Root/id=3", "GrandchildTypeA/column/grandchildColumn", "id3 data");
  private static final Triple INHER_ID4 =
      getTriple(SCHEMA_NAME, "Root/id=4", "GrandchildTypeB/column/grandchildColumn", "id4 data");
  private static final Triple INHER_ID4_GRANDPARENT_FIELD =
      getTriple(SCHEMA_NAME, "Root/id=4", "Root/column/rootColumn", "id4 data for rootColumn");
  private static final Triple INHER_ID4_PARENT_FIELD =
      getTriple(SCHEMA_NAME, "Root/id=4", "Child/column/childColumn", "id4 data for childColumn");
  private static final Triple INHER_ID5 =
      getTriple(
          SCHEMA_NAME,
          "Root/id=5",
          EXTERNAL_SCHEMA_NAME,
          "ExternalChild/column/externalChildColumn",
          "id5 data");
  private static final Triple INHER_ID6 =
      getTriple(
          SCHEMA_NAME,
          "Root/id=6",
          EXTERNAL_SCHEMA_NAME,
          "ExternalGrandchild/column/externalGrandchildColumn",
          "id6 data");
  private static final Triple INHER_UNRELATED =
      getTriple(
          EXTERNAL_SCHEMA_NAME,
          "ExternalUnrelated/id=a",
          "ExternalUnrelated/column/externalUnrelatedColumn",
          "unrelated data");

  static Triple getTriple(
      String subjectSchemaName,
      String subjectLocalPart,
      String predicateSchemaName,
      String predicateLocalPart,
      String object) {
    return getTriple(
        getIri(subjectSchemaName, subjectLocalPart),
        getIri(predicateSchemaName, predicateLocalPart),
        Values.literal(object));
  }

  static Triple getTriple(
      String schemaName, String subjectLocalPart, String predicateLocalPart, String object) {
    return getTriple(schemaName, subjectLocalPart, schemaName, predicateLocalPart, object);
  }

  @BeforeAll
  static void beforeAll() {
    database.dropSchemaIfExists(EXTERNAL_SCHEMA_NAME); // must be deleted first

    // Use example from the catalogue schema since this has all the different issues.
    tableInherTest = database.dropCreateSchema(SCHEMA_NAME);
    tableInherTest.create(
        table(
            "Root",
            column("id", ColumnType.STRING).setKey(1),
            column("rootColumn", ColumnType.STRING)));
    tableInherTest.create(table("Child", column("childColumn")).setInheritName("Root"));
    // Same column name but not in shared parent, so test how this is handled.
    tableInherTest.create(
        table("GrandchildTypeA", column("grandchildColumn")).setInheritName("Child"));
    tableInherTest.create(
        table("GrandchildTypeB", column("grandchildColumn")).setInheritName("Child"));
    tableInherTest.getTable("Root").insert(row("id", "1", "rootColumn", "id1 data"));
    tableInherTest.getTable("Child").insert(row("id", "2", "childColumn", "id2 data"));
    tableInherTest
        .getTable("GrandchildTypeA")
        .insert(row("id", "3", "grandchildColumn", "id3 data"));
    tableInherTest
        .getTable("GrandchildTypeB")
        .insert(
            row(
                "id",
                "4",
                "rootColumn",
                "id4 data for rootColumn",
                "childColumn",
                "id4 data for childColumn",
                "grandchildColumn",
                "id4 data"));

    // Test for table that extends table from different schema
    tableInherExtTest = database.createSchema(EXTERNAL_SCHEMA_NAME);
    tableInherExtTest.create(
        table("ExternalChild", column("externalChildColumn"))
            .setImportSchema(tableInherTest.getName())
            .setInheritName("Root"));
    tableInherExtTest.create(
        table("ExternalGrandchild", column("externalGrandchildColumn"))
            .setInheritName("ExternalChild"));
    tableInherExtTest.create(
        table(
            "ExternalUnrelated",
            column("id", ColumnType.STRING).setKey(1),
            column("externalUnrelatedColumn")));
    tableInherExtTest
        .getTable("ExternalChild")
        .insert(row("id", "5", "externalChildColumn", "id5 data"));
    tableInherExtTest
        .getTable("ExternalGrandchild")
        .insert(row("id", "6", "externalGrandchildColumn", "id6 data"));
    tableInherExtTest
        .getTable("ExternalUnrelated")
        .insert(row("id", "a", "externalUnrelatedColumn", "unrelated data"));
  }

  @Test
  void testTableInheritanceAlwaysSamePredicate() throws IOException {
    InMemoryRDFHandler handler = parseRootRdf(List.of(tableInherTest, tableInherExtTest));
    // All should use the same predicate for rootColumn:
    // Root (is root of all inheritance)
    // Child extends Root
    // GrandChildTypeA extends Child
    // GrandChildTypeB extends Child
    // ExternalChild extends Root
    // ExternalGrandchild extends ExternalChild
    assertAll(
        () ->
            assertTrue(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "Root/column/rootColumn")),
                "There should be a predicate for the rootColumn in the Root table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "Child/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the Child table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "GrandchildTypeA/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeA table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "GrandchildTypeB/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeB table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "ExternalChild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalChild table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(SCHEMA_NAME) + "ExternalGrandchild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalGrandchild table"));
  }

  @Test
  void testTableInheritanceRetrieveData() throws IOException {
    InMemoryRDFHandler handler = parseRootRdf(List.of(tableInherTest));
    new RdfValidator()
        .add(INHER_ID1, true)
        .add(INHER_ID2, true)
        .add(INHER_ID3, true)
        .add(INHER_ID4, true)
        .add(INHER_ID4_PARENT_FIELD, true)
        .add(INHER_ID4_GRANDPARENT_FIELD, true)
        .add(INHER_ID5, false) // different schema
        .add(INHER_ID6, false) // different schema
        .add(INHER_UNRELATED, false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableRoot() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Root");
    new RdfValidator()
        .add(INHER_ID1, true)
        .add(INHER_ID2, true)
        .add(INHER_ID3, true)
        .add(INHER_ID4, true)
        .add(INHER_ID4_PARENT_FIELD, true)
        .add(INHER_ID4_GRANDPARENT_FIELD, true)
        .add(INHER_ID5, false) // different schema
        .add(INHER_ID6, false) // different schema
        .add(INHER_UNRELATED, false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableChild() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Child");
    new RdfValidator()
        .add(INHER_ID1, false) // parent of selected table
        .add(INHER_ID2, true)
        .add(INHER_ID3, true) // child
        .add(INHER_ID4, true) // child
        .add(INHER_ID4_PARENT_FIELD, true) // child
        .add(INHER_ID4_GRANDPARENT_FIELD, true) // child
        .add(INHER_ID5, false) // different schema
        .add(INHER_ID6, false) // different schema
        .add(INHER_UNRELATED, false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeA() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "GrandchildTypeA");
    new RdfValidator()
        .add(INHER_ID1, false) // grandparent of selected table
        .add(INHER_ID2, false) // parent of selected table
        .add(INHER_ID3, true)
        .add(INHER_ID4, false) // sibling of selected table
        .add(INHER_ID4_PARENT_FIELD, false) // sibling of selected
        .add(INHER_ID4_GRANDPARENT_FIELD, false) // sibling of selected table
        .add(INHER_ID5, false) // different schema
        .add(INHER_ID6, false) // different schema
        .add(INHER_UNRELATED, false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeB() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "GrandchildTypeB");
    new RdfValidator()
        .add(INHER_ID1, false) // grandparent of selected table
        .add(INHER_ID2, false) // parent of selected table
        .add(INHER_ID3, false) // sibling of selected table
        .add(INHER_ID4, true)
        .add(INHER_ID4_PARENT_FIELD, true)
        .add(INHER_ID4_GRANDPARENT_FIELD, true)
        .add(INHER_ID5, false) // different schema
        .add(INHER_ID6, false) // different schema
        .add(INHER_UNRELATED, false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithRowId() throws IOException {
    InMemoryRDFHandler handler = parseRowRdf(tableInherTest, "Root", "id=4");
    new RdfValidator()
        .add(INHER_ID1, false) // not selected
        .add(INHER_ID2, false) // not selected
        .add(INHER_ID3, false) // not selected
        .add(INHER_ID4, true)
        .add(INHER_ID4_PARENT_FIELD, true)
        .add(INHER_ID4_GRANDPARENT_FIELD, true)
        .add(INHER_ID5, false) // not selected
        .add(INHER_ID6, false) // not selected
        .add(INHER_UNRELATED, false) // not selected
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaRetrieveData() throws IOException {
    InMemoryRDFHandler handler = parseRootRdf(List.of(tableInherExtTest));
    new RdfValidator()
        .add(INHER_ID1, false) // different schema
        .add(INHER_ID2, false) // different schema
        .add(INHER_ID3, false) // different schema
        .add(INHER_ID4, false) // different schema
        .add(INHER_ID4_PARENT_FIELD, false) // different schema
        .add(INHER_ID4_GRANDPARENT_FIELD, false) // different schema
        .add(INHER_ID5, true)
        .add(INHER_ID6, true)
        .add(INHER_UNRELATED, true)
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithTableExternalChild() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    InMemoryRDFHandler handler = parseTableRdf(tableInherExtTest, "ExternalChild");
    new RdfValidator()
        .add(INHER_ID1, false) // different schema
        .add(INHER_ID2, false) // different schema
        .add(INHER_ID3, false) // different schema
        .add(INHER_ID4, false) // different schema
        .add(INHER_ID4_PARENT_FIELD, false) // different schema
        .add(INHER_ID4_GRANDPARENT_FIELD, false) // different schema
        .add(INHER_ID5, true)
        .add(INHER_ID6, true)
        .add(INHER_UNRELATED, false) // not part of inheritance
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithRowId() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    InMemoryRDFHandler handler = parseRowRdf(tableInherExtTest, "ExternalChild", "id=5");
    new RdfValidator()
        .add(INHER_ID1, false) // not selected
        .add(INHER_ID2, false) // not selected
        .add(INHER_ID3, false) // not selected
        .add(INHER_ID4, false) // not selected
        .add(INHER_ID4_PARENT_FIELD, false) // not selected
        .add(INHER_ID4_GRANDPARENT_FIELD, false) // not selected
        .add(INHER_ID5, true)
        .add(INHER_ID6, false) // not selected
        .add(INHER_UNRELATED, false) // not selected
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchema() throws IOException {
    InMemoryRDFHandler handler = parseRootRdf(List.of(tableInherTest, tableInherExtTest));
    new RdfValidator()
        .add(INHER_ID1, true)
        .add(INHER_ID2, true)
        .add(INHER_ID3, true)
        .add(INHER_ID4, true)
        .add(INHER_ID4_PARENT_FIELD, true)
        .add(INHER_ID4_GRANDPARENT_FIELD, true)
        .add(INHER_ID5, true)
        .add(INHER_ID6, true)
        .add(INHER_UNRELATED, true)
        .validate(handler);
  }
}
