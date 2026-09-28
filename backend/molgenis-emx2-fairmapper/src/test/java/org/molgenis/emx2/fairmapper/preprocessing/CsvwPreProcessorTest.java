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
import org.molgenis.emx2.rdf.vocabulary.HEALTHDCATAP;

class CsvwPreProcessorTest {

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
  void givenTableGroupWithSingleTable_whenProcessed_thenDatasetLinksDirectlyToTable() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/dataset/1", "https://example.com/csvw/1/table/1");
  }

  @Test
  void givenTableGroupWithMultipleTables_whenProcessed_thenDatasetLinksToEachTable() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1>, <https://example.com/csvw/1/table/2> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables(
        "https://example.com/dataset/1",
        "https://example.com/csvw/1/table/1",
        "https://example.com/csvw/1/table/2");
  }

  @Test
  void givenMultipleTableGroups_whenProcessed_thenDatasetLinksToTablesFromBoth() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1>, <https://example.com/csvw/2> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .

        <https://example.com/csvw/2> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/2/table/1> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables(
        "https://example.com/dataset/1",
        "https://example.com/csvw/1/table/1",
        "https://example.com/csvw/2/table/1");
  }

  @Test
  void givenTableGroupWithoutTables_whenProcessed_thenLinkToTableGroupIsUnchanged() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a csvw:TableGroup .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/dataset/1", "https://example.com/csvw/1");
  }

  @Test
  void givenLinkedResourceNotTypedAsCsvwTableGroup_whenProcessed_thenLinkIsUnchanged() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        # Missing the csvw:TableGroup typing.
        <https://example.com/csvw/1> csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/dataset/1", "https://example.com/csvw/1");
  }

  @Test
  void givenSubjectNotTypedAsDataset_whenProcessed_thenLinkIsUnchanged() {
    load(
        """
        <https://example.com/catalog/1> a dcat:Catalog ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables("https://example.com/catalog/1", "https://example.com/csvw/1");
  }

  @Test
  void givenDatasetAlreadyLinkedDirectlyToATable_whenProcessed_thenDirectLinkIsPreserved() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1>, <https://example.com/existing/table> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new CsvwPreProcessor().process(repository);

    assertHasVariables(
        "https://example.com/dataset/1",
        "https://example.com/csvw/1/table/1",
        "https://example.com/existing/table");
  }

  @Test
  void processingShouldBeIdempotent() {
    load(
        """
        <https://example.com/dataset/1> a dcat:Dataset ;
            healthdcatap:hasVariables <https://example.com/csvw/1> .

        <https://example.com/csvw/1> a csvw:TableGroup ;
            csvw:table <https://example.com/csvw/1/table/1> .
        """);

    new CsvwPreProcessor().process(repository);
    long countAfterFirstRun = countAllStatements();
    new CsvwPreProcessor().process(repository);

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

  private void assertHasVariables(String dataset, String... expectedTables) {
    Set<String> expected = new HashSet<>(Set.of(expectedTables));
    Set<String> actual =
        repository
            .getConnection()
            .getStatements(Values.iri(dataset), HEALTHDCATAP.HAS_VARIABLES, null)
            .stream()
            .map(statement -> statement.getObject().toString())
            .collect(Collectors.toSet());

    assertEquals(expected, actual);
  }

  private long countAllStatements() {
    return repository.getConnection().getStatements(null, null, null).stream().count();
  }
}
