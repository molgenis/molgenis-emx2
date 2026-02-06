package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.FairMapperException;

class SparqlEngineTest {

  @Test
  void testBasicConstruct() {
    ModelBuilder builder = new ModelBuilder();
    builder.setNamespace("ex", "http://example.org/");
    builder.subject("ex:person1").add(RDF.TYPE, FOAF.PERSON).add(FOAF.NAME, "John Doe");
    Model input = builder.build();

    String sparql =
        """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        CONSTRUCT {
          ?s foaf:name ?name .
        }
        WHERE {
          ?s rdf:type foaf:Person .
          ?s foaf:name ?name .
        }
        """;

    Model result = SparqlEngine.construct(input, sparql);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertTrue(result.size() >= 1);
  }

  @Test
  void testEmptyResult() {
    ModelBuilder builder = new ModelBuilder();
    builder.setNamespace("ex", "http://example.org/");
    builder.subject("ex:person1").add(FOAF.NAME, "John Doe");
    Model input = builder.build();

    String sparql =
        """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        CONSTRUCT {
          ?s foaf:age ?age .
        }
        WHERE {
          ?s foaf:age ?age .
        }
        """;

    Model result = SparqlEngine.construct(input, sparql);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testMaxTriplesLimit() {
    ModelBuilder builder = new ModelBuilder();
    for (int i = 0; i < 100_001; i++) {
      builder
          .subject("http://example.org/person" + i)
          .add(RDF.TYPE, FOAF.PERSON)
          .add(FOAF.NAME, "Person " + i);
    }
    Model input = builder.build();

    String sparql =
        """
        CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }
        """;

    assertThrows(FairMapperException.class, () -> SparqlEngine.construct(input, sparql));
  }

  @Test
  void testMultipleTriples() {
    ModelBuilder builder = new ModelBuilder();
    builder.setNamespace("ex", "http://example.org/");
    builder
        .subject("ex:person1")
        .add(RDF.TYPE, FOAF.PERSON)
        .add(FOAF.NAME, "John Doe")
        .add(FOAF.MBOX, "john@example.org");
    Model input = builder.build();

    String sparql =
        """
        PREFIX foaf: <http://xmlns.com/foaf/0.1/>
        CONSTRUCT {
          ?s ?p ?o .
        }
        WHERE {
          ?s ?p ?o .
        }
        """;

    Model result = SparqlEngine.construct(input, sparql);

    assertNotNull(result);
    assertEquals(3, result.size());
  }
}
