package org.molgenis.emx2.fairmapper.preprocessing;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.DefaultNamespace;

class CsvwPreProcessorTest {

  private static final IRI DATASET = iri("https://example.com/dataset/1");
  private static final IRI DISTRIBUTION = iri("https://example.com/distribution/1");
  private static final IRI TABLE_GROUP = iri("https://example.com/csvw/1");
  private static final IRI TABLE = iri("https://example.com/csvw/1/table/1");
  private static final IRI TABLE_2 = iri("https://example.com/csvw/1/table/2");
  private static final IRI DISTRIBUTION_2 = iri("https://example.com/distribution/2");
  private static final IRI TABLE_GROUP_2 = iri("https://example.com/csvw/2");
  private static final IRI TABLE_3 = iri("https://example.com/csvw/2/table/1");

  private SailRepository repository;

  @BeforeEach
  void setUp() {
    repository = new SailRepository(new MemoryStore());
  }

  @Test
  void givenFullChain_whenProcessed_thenDatasetHasVariablesForTable() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET, TABLE);
  }

  @Test
  void givenTableGroupWithMultipleTables_whenProcessed_thenDatasetHasVariablesForEachTable() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE_2);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET, TABLE, TABLE_2);
  }

  @Test
  void givenMultipleDistributions_whenProcessed_thenDatasetHasVariablesFromBoth() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);

      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);

      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION_2);
      addTriplet(conn, DISTRIBUTION_2, DCAT.DOWNLOAD_URL, TABLE_GROUP_2);
      addType(conn, TABLE_GROUP_2, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP_2, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP_2, csvw("table"), TABLE_3);

      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET, TABLE, TABLE_3);
  }

  @Test
  void givenDistributionWithoutDownloadUrl_whenProcessed_thenNoVariablesAdded() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET);
  }

  @Test
  void givenTableGroupWithoutTables_whenProcessed_thenNoVariablesAdded() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET);
  }

  @Test
  void givenDownloadTargetNotTypedAsCsvwTableGroup_whenProcessed_thenNoVariablesAdded() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      // Missing the csvw:TableGroup / dcat:Resource typing on the download target.
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET);
  }

  /**
   * The CONSTRUCT query requires the dataset to be typed as both {@code dcat:Resource} and {@code
   * dcat:Dataset} explicitly - it does not do RDFS-style subclass reasoning to infer {@code
   * dcat:Resource} from {@code dcat:Dataset} alone. Real-world data that only tags a dataset with
   * {@code dcat:Dataset} (which is arguably the more common case, since Dataset is a subclass of
   * Resource) will silently get no {@code hasVariables} triples. This test documents that current
   * behaviour rather than asserting what "should" happen, per instructions not to touch the
   * pre-processor itself.
   */
  @Test
  void givenDatasetTypedOnlyAsDcatDataset_whenProcessed_thenNoVariablesAdded() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(DATASET);
  }

  @Test
  void givenNonDatasetResource_whenProcessed_thenNoVariablesAdded() {
    IRI catalog = iri("https://example.com/catalog/1");
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, catalog, DCAT.RESOURCE);
      addType(conn, catalog, dcat("Catalog"));
      addTriplet(conn, catalog, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);

    assertHasVariables(catalog);
  }

  @Test
  void processingShouldBeIdempotent() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      addType(conn, DATASET, DCAT.RESOURCE);
      addType(conn, DATASET, DCAT.DATASET);
      addTriplet(conn, DATASET, dcat("distribution"), DISTRIBUTION);
      addTriplet(conn, DISTRIBUTION, DCAT.DOWNLOAD_URL, TABLE_GROUP);
      addType(conn, TABLE_GROUP, DCAT.RESOURCE);
      addType(conn, TABLE_GROUP, csvw("TableGroup"));
      addTriplet(conn, TABLE_GROUP, csvw("table"), TABLE);
      conn.commit();
    }

    new CsvwPreProcessor().process(repository);
    long countAfterFirstRun = countAllStatements();
    new CsvwPreProcessor().process(repository);

    assertEquals(countAfterFirstRun, countAllStatements());
    assertHasVariables(DATASET, TABLE);
  }

  private void assertHasVariables(IRI dataset, IRI... expectedTables) {
    Set<String> expected = new HashSet<>();
    for (IRI table : expectedTables) {
      expected.add(table.stringValue());
    }

    String query =
        "SELECT ?table WHERE { <%s> <%s> ?table }".formatted(dataset, healthdcatap("hasVariables"));
    Set<String> actual = new HashSet<>();
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
      result.forEach(bindingSet -> actual.add(bindingSet.getValue("table").stringValue()));
    }

    assertEquals(expected, actual);
  }

  private static void addType(SailRepositoryConnection conn, IRI subject, IRI type) {
    addTriplet(conn, subject, RDF.TYPE, type);
  }

  private static void addTriplet(
      SailRepositoryConnection conn, IRI subject, IRI predicate, IRI object) {
    conn.add(statement(subject, predicate, object, null));
  }

  private long countAllStatements() {
    String query = "SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }";
    try (SailRepositoryConnection conn = repository.getConnection();
        TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
      return Long.parseLong(result.next().getValue("count").stringValue());
    }
  }

  private static IRI dcat(String localName) {
    return iri(DefaultNamespace.DCAT.getNamespace().getName() + localName);
  }

  private static IRI csvw(String localName) {
    return iri(DefaultNamespace.CSVW.getNamespace().getName() + localName);
  }

  private static IRI healthdcatap(String localName) {
    return iri(DefaultNamespace.HEALTHDCAT_AP.getNamespace().getName() + localName);
  }
}
