package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.RdfValidator;

class Emx2TableInheritanceTest extends TableInheritanceTest {
  private static final String EMX2_IRI_PATH = "/column/";
  private static final String TABLECLASS_PATH = EMX2_IRI_PATH + MG_TABLECLASS;

  // Tests should not modify the data!
  private static InMemoryRDFHandler rootRdf;
  private static InMemoryRDFHandler childRdf;
  private static InMemoryRDFHandler grandChildTypeARdf;
  private static InMemoryRDFHandler grandChildTypeBRdf;
  private static InMemoryRDFHandler externalChildRdf;
  private static InMemoryRDFHandler externalGrandChildRdf;
  private static InMemoryRDFHandler rowIdRdf;

  @BeforeAll
  static void generateRdf() throws IOException {
    rootRdf = parseTableRdf(tableInherTest, "Root");
    childRdf = parseTableRdf(tableInherTest, "Child");
    grandChildTypeARdf = parseTableRdf(tableInherTest, "GrandchildTypeA");
    grandChildTypeBRdf = parseTableRdf(tableInherTest, "GrandchildTypeB");
    externalChildRdf = parseTableRdf(tableInherExtTest, "ExternalChild");
    externalGrandChildRdf = parseTableRdf(tableInherExtTest, "ExternalGrandchild");
    rowIdRdf = parseRowRdf(tableInherTest, "Root", "id=4");
  }

  @Test
  void testPresenceEmx2Columns_root() {
    validateEmx2ColumnRdfPresence(rootRdf, true, true, true, false, false);
  }

  @Test
  void testPresenceEmx2Columns_child() {
    validateEmx2ColumnRdfPresence(childRdf, true, true, true, false, false);
  }

  @Test
  void testPresenceEmx2Columns_grandChildTypeA() {
    validateEmx2ColumnRdfPresence(grandChildTypeARdf, true, true, false, false, false);
  }

  @Test
  void testPresenceEmx2Columns_grandChildTypeB() {
    validateEmx2ColumnRdfPresence(grandChildTypeBRdf, true, false, true, false, false);
  }

  @Test
  void testPresenceEmx2Columns_externalChild() {
    validateEmx2ColumnRdfPresence(externalChildRdf, false, false, false, true, true);
  }

  @Test
  void testPresenceEmx2Columns_externalGrandChild() {
    validateEmx2ColumnRdfPresence(externalGrandChildRdf, false, false, false, true, true);
  }

  @Test
  void testPresenceEmx2Columns_rowId() {
    validateEmx2ColumnRdfPresence(rowIdRdf, true, true, true, false, false);
  }

  void validateEmx2ColumnRdfPresence(
      InMemoryRDFHandler handler,
      boolean child,
      boolean grandChildTypeA,
      boolean grandChildTypeB,
      boolean externalChild,
      boolean externalGrandChild) {
    new RdfValidator()
        // Root should always be present (used for primary key)
        .add(getIri(SCHEMA_NAME, "Root/column/rootColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, true)
        .add(getIri(SCHEMA_NAME, "Child/column/childColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, child)
        .add(
            getIri(SCHEMA_NAME, "GrandchildTypeA/column/grandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            grandChildTypeA)
        .add(
            getIri(SCHEMA_NAME, "GrandchildTypeB/column/grandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            grandChildTypeB)
        .add(
            getIri(EXTERNAL_SCHEMA_NAME, "ExternalChild/column/externalChildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            externalChild)
        .add(
            getIri(EXTERNAL_SCHEMA_NAME, "ExternalGrandchild/column/externalGrandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            externalGrandChild)
        .validate(handler);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_root() {
    validateSubjectPresentForEachEmx2Predicate(rootRdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_child() {
    validateSubjectPresentForEachEmx2Predicate(childRdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_grandChildTypeA() {
    validateSubjectPresentForEachEmx2Predicate(grandChildTypeARdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_grandChildTypeB() {
    validateSubjectPresentForEachEmx2Predicate(grandChildTypeBRdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_externalChild() {
    validateSubjectPresentForEachEmx2Predicate(externalChildRdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_externalGrandChild() {
    validateSubjectPresentForEachEmx2Predicate(externalGrandChildRdf);
  }

  @Test
  void testEmx2SubjectPresentForEmx2Predicates_rowId() {
    validateSubjectPresentForEachEmx2Predicate(rowIdRdf);
  }

  void validateSubjectPresentForEachEmx2Predicate(InMemoryRDFHandler handler) {
    // We currently do not check whether a column is described (based on if it has non-empty
    // values), only whether according to the table it will be described IF data is present.
    Set<String> emx2Predicates = getEmx2Predicates(handler);
    Set<String> emx2Subjects = getEmx2ColumnSubjects(handler);

    emx2Predicates.removeAll(emx2Subjects);
    assertTrue(
        emx2Predicates.isEmpty(),
        "Some predicates are missing a subject describing them: " + emx2Predicates);
  }

  @Test
  void testTableClassColumnPresentInSchema() {
    // Precondition: Ensures the mg_tableclass tests are not trivially passing.
    assertNotNull(tableInherTest.getTable("Root").getMetadata().getLocalColumn(MG_TABLECLASS));
    assertNotNull(tableInherTest.getTable("Child").getMetadata().getLocalColumn(MG_TABLECLASS));
    assertNotNull(
        tableInherExtTest.getTable("ExternalChild").getMetadata().getLocalColumn(MG_TABLECLASS));
  }

  @Test
  void testTableClassNotPresent_root() {
    validateNoTableClass(rootRdf);
  }

  @Test
  void testTableClassNotPresent_child() {
    validateNoTableClass(childRdf);
  }

  @Test
  void testTableClassNotPresent_grandChildTypeA() {
    validateNoTableClass(grandChildTypeARdf);
  }

  @Test
  void testTableClassNotPresent_grandChildTypeB() {
    validateNoTableClass(grandChildTypeBRdf);
  }

  @Test
  void testTableClassNotPresent_externalChild() {
    validateNoTableClass(externalChildRdf);
  }

  @Test
  void testTableClassNotPresent_externalGrandChild() {
    validateNoTableClass(externalGrandChildRdf);
  }

  @Test
  void testTableClassNotPresent_rowId() {
    validateNoTableClass(rowIdRdf);
  }

  void validateNoTableClass(InMemoryRDFHandler handler) {
    getEmx2ColumnSubjects(handler).forEach(this::failOnTableClass);
    getEmx2Predicates(handler).forEach(this::failOnTableClass);
  }

  void failOnTableClass(String iri) {
    if (iri.contains(TABLECLASS_PATH)) {
      fail("Table class was found: " + iri);
    }
  }

  private Set<String> getEmx2Predicates(InMemoryRDFHandler handler) {
    return handler.resources.values().stream()
        .flatMap(predicatesMap -> predicatesMap.keySet().stream())
        .map(IRI::stringValue)
        .filter(predicate -> predicate.contains(EMX2_IRI_PATH))
        .collect(Collectors.toSet());
  }

  private Set<String> getEmx2ColumnSubjects(InMemoryRDFHandler handler) {
    return handler.resources.keySet().stream()
        .map(Resource::stringValue)
        .filter(subject -> subject.contains(EMX2_IRI_PATH))
        .collect(Collectors.toSet());
  }
}
