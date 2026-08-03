package org.molgenis.emx2.fairmapper.preprocessing;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.*;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.DefaultNamespace;

class TypicalAgeRdfPreProcessorTest {

  private static final IRI DATASET_SUBJECT = iri("https://example.com/dataset/1");
  private static final IRI CATALOG_SUBJECT = iri("https://example.com/catalog/1");

  private static SailRepository repository;

  @BeforeEach
  void setUp() {
    repository = new SailRepository(new MemoryStore());
  }

  @Test
  void givenDataset_whenOnlyMinAge_thenAddMinimumAge() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, DATASET_SUBJECT, DCAT.DATASET);
      addMinAge(conn, DATASET_SUBJECT, 1);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    assertMinTypicalAgeEquals(DATASET_SUBJECT, 1);
    assertMaxTypicalAgeEquals(DATASET_SUBJECT, null);
  }

  @Test
  void givenDataset_whenOnlyMaxAge_thenAddMaximumAge() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, DATASET_SUBJECT, DCAT.DATASET);
      addMaxAge(conn, DATASET_SUBJECT, 1);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    assertMinTypicalAgeEquals(DATASET_SUBJECT, null);
    assertMaxTypicalAgeEquals(DATASET_SUBJECT, 1);
  }

  @Test
  void givenDataset_whenBothMinAndMax_thenAddAgeBothTriplets() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, DATASET_SUBJECT, DCAT.DATASET);
      addMinAge(conn, DATASET_SUBJECT, 1);
      addMaxAge(conn, DATASET_SUBJECT, 10);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    assertMinTypicalAgeEquals(DATASET_SUBJECT, 1);
    assertMaxTypicalAgeEquals(DATASET_SUBJECT, 10);
  }

  @Test
  void givenCatalog_thenDontAddNewTriplets() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, CATALOG_SUBJECT, DCAT.CATALOG);
      addMinAge(conn, CATALOG_SUBJECT, 1);
      addMaxAge(conn, CATALOG_SUBJECT, 10);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    assertMinTypicalAgeEquals(CATALOG_SUBJECT, null);
    assertMaxTypicalAgeEquals(CATALOG_SUBJECT, null);
  }

  @Test
  void givenDataset_whenNoMinOrMax_thenDontAdd() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, DATASET_SUBJECT, DCAT.DATASET);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    assertMinTypicalAgeEquals(DATASET_SUBJECT, null);
    assertMaxTypicalAgeEquals(DATASET_SUBJECT, null);
  }

  @Test
  void processingShouldBeIdempotent() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addSubjectType(conn, DATASET_SUBJECT, DCAT.DATASET);
      addMinAge(conn, DATASET_SUBJECT, 1);
      addMaxAge(conn, DATASET_SUBJECT, 10);
      conn.commit();
    }

    new TypicalAgeRdfPreProcessor().process(repository);
    long countAfterFirstRun = countAllStatements();
    new TypicalAgeRdfPreProcessor().process(repository);
    assertEquals(countAfterFirstRun, countAllStatements());
  }

  private static void addSubjectType(SailRepositoryConnection conn, IRI subject, IRI type) {
    addTriplet(conn, subject, RDF.TYPE, type);
  }

  private static void addMaxAge(SailRepositoryConnection conn, IRI subject, int maxAge) {
    addTriplet(conn, subject, iriFromHealthDCATAP("maxTypicalAge"), literal(maxAge));
  }

  private static void addMinAge(SailRepositoryConnection conn, IRI subject, int minAge) {
    addTriplet(conn, subject, iriFromHealthDCATAP("minTypicalAge"), literal(minAge));
  }

  private static void addTriplet(
      SailRepositoryConnection conn, IRI subject, IRI predicate, Value value) {
    conn.add(statement(subject, predicate, value, null));
  }

  /**
   * @param expectedAge null if no age is expected
   */
  private void assertMinTypicalAgeEquals(IRI subject, Integer expectedAge) {
    assertEquals(getAge(subject, iriFromHealthDCATAP("minimumTypicalAge")), expectedAge);
  }

  /**
   * @param expectedAge null if no age is expected
   */
  private void assertMaxTypicalAgeEquals(IRI subject, Integer expectedAge) {
    assertEquals(getAge(subject, iriFromHealthDCATAP("maximumTypicalAge")), expectedAge);
  }

  private Integer getAge(IRI subject, IRI predicate) {
    String query = "SELECT ?age WHERE { <%s> <%s> ?age }".formatted(subject, predicate);
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {

      if (!result.hasNext()) {
        return null;
      }

      Integer year =
          Optional.of(result.next().getValue("age").stringValue())
              .map(Integer::parseInt)
              .orElse(null);

      if (result.hasNext()) {
        fail("Multiple years found for subject: " + subject);
      }

      return year;
    }
  }

  private static long countAllStatements() {
    String query = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }";
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
      return Long.parseLong(result.next().getValue("count").stringValue());
    }
  }

  private static IRI iriFromHealthDCATAP(String localName) {
    return iri(DefaultNamespace.HEALTHDCAT_AP.getNamespace().getName() + localName);
  }
}
