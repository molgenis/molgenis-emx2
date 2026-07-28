package org.molgenis.emx2.fairmapper.preprocessing;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;

class TemporalRdfPreProcessorTest {

  private static final IRI DATASET_SUBJECT = iri("https://example.com/dataset/1");
  private static final IRI CATALOG_SUBJECT = iri("https://example.com/catalog/1");
  private static final IRI COLLECTION_SUBJECT = iri("https://example.com/collection/1");
  private static final IRI MISSING_END_DATE_SUBJECT = iri("https://example.com/dataset/2");
  private static final IRI NO_TEMPORAL_SUBJECT = iri("https://example.com/dataset/3");
  private static final IRI MISSING_START_DATE_SUBJECT = iri("https://example.com/dataset/4");

  private static SailRepository repository;

  @BeforeAll
  static void setUp() {
    repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addTemporalData(conn, DATASET_SUBJECT, DCAT.DATASET, 2020, 2023);
      addTemporalData(conn, CATALOG_SUBJECT, DCAT.CATALOG, 2018, 2022);
      addTemporalData(conn, COLLECTION_SUBJECT, DCAT.DATASET_SERIES, 2019, 2024);
      addTemporalData(conn, MISSING_END_DATE_SUBJECT, DCAT.DATASET, 2021, null);
      addTemporalData(conn, MISSING_START_DATE_SUBJECT, DCAT.DATASET, null, 2022);
      conn.add(statement(NO_TEMPORAL_SUBJECT, RDF.TYPE, DCAT.DATASET, null));
      conn.commit();
    }

    new TemporalRdfPreProcessor().process(repository);
  }

  @Test
  void datasetShouldHaveStartAndEndYearEnriched() {
    assertHasStartYear(DATASET_SUBJECT, 2020);
    assertHasEndYear(DATASET_SUBJECT, 2023);
  }

  @Test
  void catalogShouldHaveStartAndEndYearEnriched() {
    assertHasStartYear(CATALOG_SUBJECT, 2018);
    assertHasEndYear(CATALOG_SUBJECT, 2022);
  }

  @Test
  void collectionShouldNotHaveStartOrEndYearEnriched() {
    assertTrue(getYear(COLLECTION_SUBJECT, DCAT.START_DATE).isEmpty());
    assertTrue(getYear(COLLECTION_SUBJECT, DCAT.END_DATE).isEmpty());
  }

  @Test
  void datasetWithoutTemporalShouldNotBeEnriched() {
    assertTrue(getYear(NO_TEMPORAL_SUBJECT, DCAT.START_DATE).isEmpty());
    assertTrue(getYear(NO_TEMPORAL_SUBJECT, DCAT.END_DATE).isEmpty());
  }

  @Test
  void whenStartDateMissing_thenOnlyEndYearEnriched() {
    assertHasEndYear(MISSING_START_DATE_SUBJECT, 2022);
    assertTrue(getYear(MISSING_START_DATE_SUBJECT, DCAT.START_DATE).isEmpty());
  }

  @Test
  void whenEndDateMissing_thenOnlyStartYearEnriched() {
    assertHasStartYear(MISSING_END_DATE_SUBJECT, 2021);
    assertTrue(getYear(MISSING_END_DATE_SUBJECT, DCAT.END_DATE).isEmpty());
  }

  @Test
  void enrichmentShouldBeIdempotent() {
    long countBefore = countAllStatements();
    new TemporalRdfPreProcessor().process(repository);
    assertEquals(countBefore, countAllStatements());
  }

  private void assertHasStartYear(IRI subject, int expectedYear) {
    int startYear =
        getYear(subject, DCAT.START_DATE)
            .orElseThrow(
                () ->
                    new AssertionError(
                        "Expected dcat:startDate on <%s> but found none".formatted(subject)));
    assertEquals(expectedYear, startYear);
  }

  private void assertHasEndYear(IRI subject, int expectedYear) {
    int endYear =
        getYear(subject, DCAT.END_DATE)
            .orElseThrow(
                () ->
                    new AssertionError(
                        "Expected dcat:endDate on <%s> but found none".formatted(subject)));
    assertEquals(expectedYear, endYear);
  }

  private static void addTemporalData(
      SailRepositoryConnection conn, IRI subject, IRI type, Integer startYear, Integer endYear) {
    BNode temporal = bnode();
    conn.add(statement(subject, RDF.TYPE, type, null));
    conn.add(statement(subject, DCTERMS.TEMPORAL, temporal, null));
    if (startYear != null) {
      conn.add(statement(temporal, DCAT.START_DATE, literal(startYear + "-01-01", XSD.DATE), null));
    }
    if (endYear != null) {
      conn.add(statement(temporal, DCAT.END_DATE, literal(endYear + "-12-31", XSD.DATE), null));
    }
  }

  private static long countAllStatements() {
    String query = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }";
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
      return Long.parseLong(result.next().getValue("count").stringValue());
    }
  }

  private Optional<Integer> getYear(IRI subject, IRI predicate) {
    String query = "SELECT ?year WHERE { <%s> <%s> ?year }".formatted(subject, predicate);
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {

      if (!result.hasNext()) {
        return Optional.empty();
      }

      Optional<Integer> year =
          Optional.of(result.next().getValue("year").stringValue()).map(Integer::parseInt);

      if (result.hasNext()) {
        fail("Multiple years found for subject: " + subject);
      }

      return year;
    }
  }
}
