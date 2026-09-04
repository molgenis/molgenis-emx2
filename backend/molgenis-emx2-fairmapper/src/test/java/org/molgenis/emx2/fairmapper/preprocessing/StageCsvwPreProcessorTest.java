package org.molgenis.emx2.fairmapper.preprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StageCsvwPreProcessorTest {

  private static final String PREFIXES =
      """
      @prefix dcat: <http://www.w3.org/ns/dcat#> .
      @prefix csvw: <http://www.w3.org/ns/csvw#> .
      @prefix healthdcatap: <http://healthdataportal.eu/ns/health#> .
      """;

  private SailRepository repository;

  @BeforeEach
  void setUp() {
    repository = new SailRepository(new MemoryStore());
  }

  @Test
  void givenFullChain_whenProcessed_thenDatasetHasVariablesForTable() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/dataset/1", "https://example.com/csvw/1/table/1");
  }

  @Test
  void givenTableGroupWithMultipleTables_whenProcessed_thenDatasetHasVariablesForEachTable() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1>, <https://example.com/csvw/1/table/2> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasVariables(
        "https://example.com/dataset/1",
        "https://example.com/csvw/1/table/1",
        "https://example.com/csvw/1/table/2");
  }

  @Test
  void givenMultipleDistributions_whenProcessed_thenDatasetHasVariablesFromBoth() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1>, <https://example.com/distribution/2> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .
        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .

        <https://example.com/distribution/2> dcat:downloadURL <https://example.com/csvw/2> .
        <https://example.com/csvw/2> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/2/table/1> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasVariables(
        "https://example.com/dataset/1",
        "https://example.com/csvw/1/table/1",
        "https://example.com/csvw/2/table/1");
  }

  @Test
  void givenDistributionWithoutDownloadUrl_whenProcessed_thenNoVariablesAdded() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/dataset/1");
  }

  @Test
  void givenTableGroupWithoutTables_whenProcessed_thenNoVariablesAdded() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasNoVariables("https://example.com/dataset/1");
  }

  @Test
  void givenDownloadTargetNotTypedAsCsvwTableGroup_whenProcessed_thenNoVariablesAdded() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        # Missing the csvw:TableGroup / dcat:Resource typing on the download target.
        <https://example.com/csvw/1> csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasNoVariables("https://example.com/dataset/1");
  }

  @Test
  void givenNonDatasetResource_whenProcessed_thenNoVariablesAdded() {
    load(
        """
        <https://example.com/catalog/1> a dcat:Resource, dcat:Catalog ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new StageCsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/catalog/1");
  }

  @Test
  void processingShouldBeIdempotent() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Resource, dcat:Dataset ;
            dcat:distribution <https://example.com/distribution/1> .

        <https://example.com/distribution/1> dcat:downloadURL <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a dcat:Resource, csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new StageCsvwPreProcessor().process(repository);
    long countAfterFirstRun = countAllStatements();
    new StageCsvwPreProcessor().process(repository);

    assertEquals(countAfterFirstRun, countAllStatements());
    assertHasVariables("https://example.com/dataset/1", "https://example.com/csvw/1/table/1");
  }

  private void load(String turtle) {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      conn.add(new StringReader(PREFIXES + turtle), "", RDFFormat.TURTLE);
      conn.commit();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void assertHasNoVariables(String dataset) {
    assertHasVariables(dataset);
  }

  private void assertHasVariables(String dataset, String... expectedTables) {
    Set<String> expected = new HashSet<>(Set.of(expectedTables));
    Set<String> actual =
        repository
            .getConnection()
            .getStatements(
                Values.iri(dataset),
                Values.iri("http://healthdataportal.eu/ns/health#hasVariables"),
                null)
            .stream()
            .map(statement -> statement.getObject().toString())
            .collect(Collectors.toSet());

    assertEquals(expected, actual);
  }

  private long countAllStatements() {
    return repository.getConnection().getStatements(null, null, null).stream().count();
  }
}
