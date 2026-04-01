package org.molgenis.emx2.harvester;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SparqlSelectTransformerTest {

  private BindingToRowMapper mapper;

  @BeforeEach
  void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Table table = db.getSchema("pet store").getTable("Pet");
    mapper = new BindingToRowMapper(table);
  }

  @Test
  void sparqlSelect() throws IOException {

    Repository repository = new SailRepository(new MemoryStore());
    try (RepositoryConnection conn = repository.getConnection();
        InputStream stream = readTtl("pet-store.ttl")) {
      conn.add(stream, RDFFormat.TURTLE);
      String query =
          """
          PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
          PREFIX Pet-store: <http://localhost:8080/pet-store/api/rdf/>
          PREFIX Pet: <http://localhost:8080/pet-store/api/rdf/Pet/column/>
          PREFIX Pet: <http://localhost:8080/pet-store/api/rdf/Pet/column/>

          PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

          PREFIX Harvesting-Data: <http://localhost:8080/pet-store/api/rdf/Pet/column/>


          SELECT
              ?pet ?name ?categoryName ?status ?weight ?orderId
              (GROUP_CONCAT(?tagName; separator=", ") AS ?tags)
          WHERE {
              ?pet a Pet-store:Pet ;
                  Harvesting-Data:name                   ?name ;
                  Harvesting-Data:category               ?category ;
                  Harvesting-Data:status                 ?status ;
                  Harvesting-Data:weight                 ?weight .

              ?category rdfs:label ?categoryName
              OPTIONAL {
                  ?pet Harvesting-Data:orders ?order .
                  ?order rdfs:label ?orderId .
              }

              OPTIONAL {
                  ?pet Harvesting-Data:tags ?tag .
                  ?tag rdfs:label ?tagName .
              }
          }
          GROUP BY ?pet ?name ?categoryName ?status ?weight ?orderId
          ORDER BY ?pet
          """;
      TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
      parseResults(result);
    }
  }

  private void parseResults(TupleQueryResult result) {
    System.out.println("-".repeat(200));
    for (BindingSet bindings : result) {
      System.out.println(
          " BINDING | "
              + bindings.getBindingNames().stream()
                  .map(x -> x + " -> " + bindings.getBinding(x).getValue().stringValue())
                  .collect(Collectors.joining(", ")));
      Row row = mapper.map(bindings);
      System.out.println(" ROW     | " + row.toString());
      System.out.println("-".repeat(200));
    }
  }

  private InputStream readTtl(String path) {
    return SparqlSelectTransformerTest.class.getResourceAsStream(path);
  }
}
