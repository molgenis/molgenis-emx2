package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.RdfValidator;

/**
 * Checks presence of triples describing columns.
 *
 * @see org.molgenis.emx2.rdf.generators.Emx2RdfGenerator#describeColumn(Column)
 */
public class Emx2TableInheritanceTest extends TableInheritanceTest {
  @Test
  void testColumnsPresent_root() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Root");
    validateEmx2ColumnRdfPresence(handler, true, true, true, false, false);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_child() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Child");
    validateEmx2ColumnRdfPresence(handler, true, true, true, false, false);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_grandChildTypeA() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "GrandchildTypeA");
    validateEmx2ColumnRdfPresence(handler, true, true, false, false, false);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_grandChildTypeB() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "GrandchildTypeB");
    validateEmx2ColumnRdfPresence(handler, true, false, true, false, false);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_externalChild() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherExtTest, "ExternalChild");
    validateEmx2ColumnRdfPresence(handler, false, false, false, true, true);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_externalGrandChild() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherExtTest, "ExternalGrandchild");
    validateEmx2ColumnRdfPresence(handler, false, false, false, true, true);
    validateSubjectPresentForEachEmx2Predicate(handler);
  }

  @Test
  void testColumnsPresent_rowId() throws IOException {
    // Row endpoint describes columns of the selected table (and its subclasses) as well.
    InMemoryRDFHandler handler = parseRowRdf(tableInherTest, "Root", "id=4");
    validateEmx2ColumnRdfPresence(handler, true, true, true, false, false);
    validateSubjectPresentForEachEmx2Predicate(handler);
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

  void validateSubjectPresentForEachEmx2Predicate(InMemoryRDFHandler handler) {
    // We currently do not check whether a column is described (based on if it has non-empty
    // values), only whether according to the table it will be described IF data is present.
    Set<String> emx2Predicates = getEmx2Predicates(handler);
    Set<String> emx2Subjects = getEmx2ColumnSubjects(handler);

    emx2Predicates.removeAll(emx2Subjects);
    assertTrue(
        emx2Predicates.isEmpty(),
        "Some predicates are missing a subject describing them:" + emx2Predicates);
  }

  private Set<String> getEmx2Predicates(InMemoryRDFHandler handler) {
    return handler.resources.values().stream()
        .flatMap(predicatesMap -> predicatesMap.keySet().stream())
        .map(IRI::stringValue)
        .filter(predicate -> predicate.contains("/column/"))
        .collect(Collectors.toSet());
  }

  private Set<String> getEmx2ColumnSubjects(InMemoryRDFHandler handler) {
    return handler.resources.keySet().stream()
        .map(Resource::stringValue)
        .filter(subject -> subject.contains("/column/"))
        .collect(Collectors.toSet());
  }
}
