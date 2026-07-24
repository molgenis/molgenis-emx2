package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemporalEnrichmentTest {

  private static final String SELECT_QUERY =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dcterms: <http://purl.org/dc/terms/>
      SELECT
          ?Resources ?start_year ?end_year
      WHERE {
          ?Resources dcterms:title ?name .
          OPTIONAL { ?Resources dcat:startDate ?start_year . }
          OPTIONAL { ?Resources dcat:endDate ?end_year . }
      }
      """;

  private SailRepository repository;
  private SailRepositoryConnection conn;

  @BeforeEach
  void setUp() throws IOException {
    repository = new SailRepository(new MemoryStore());
    conn = repository.getConnection();
    try (InputStream inputStream = readTtl("data/Dataset.ttl")) {
      conn.add(inputStream, RDFFormat.TURTLE);
    }
  }

  @AfterEach
  void tearDown() {
    conn.close();
  }

  @Test
  void shouldAddStartAndEndYear() {
    try (TupleQueryResult result = queryStartEndYear()) {
      BindingSet binding = result.stream().findFirst().orElseThrow(AssertionError::new);
      assertNull(binding.getValue("start_year"));
      assertNull(binding.getValue("end_year"));
    }

    new TemporalEnrichment(repository).enrich();

    try (TupleQueryResult result = queryStartEndYear()) {
      BindingSet binding = result.stream().findFirst().orElseThrow(AssertionError::new);
      assertEquals("1968", binding.getValue("start_year").stringValue());
      assertEquals("2025", binding.getValue("end_year").stringValue());
    }
  }

  private TupleQueryResult queryStartEndYear() {
    return conn.prepareTupleQuery(QueryLanguage.SPARQL, SELECT_QUERY).evaluate();
  }

  private InputStream readTtl(String path) {
    return TemporalEnrichmentTest.class.getResourceAsStream(path);
  }
}
