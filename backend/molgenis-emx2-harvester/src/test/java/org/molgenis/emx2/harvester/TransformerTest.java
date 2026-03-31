package org.molgenis.emx2.harvester;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TransformerTest {

  @Test
  void sparqlConstruct() throws IOException {
    Repository repository = new SailRepository(new MemoryStore());

    try (RepositoryConnection conn = repository.getConnection();
        InputStream stream = readTtl("pet-store.ttl")) {
      conn.add(stream, RDFFormat.TURTLE);
      String query =
          """
            PREFIX Pet-store: <http://localhost:8080/pet-store/api/rdf/>
            PREFIX Pet: <http://localhost:8080/pet-store/api/rdf/Pet/column/>
            PREFIX Harvesting-Data: <http://localhost:8080/pet-store/api/rdf/Pet/column/>

            CONSTRUCT {
                ?pet Pet:name         ?name ;
                    Pet:category      ?category ;
                    Pet:status        ?status ;
                    Pet:weight        ?weight ;
                    Pet:mg_insertedOn ?insertedOn ;
                    Pet:tags          ?tags ;
                    Pet:orders        ?orders ;
                    Pet:mg_updatedOn  ?updatedOn .
            } WHERE {
                ?pet a Pet-store:Pet ;
                    Harvesting-Data:name                   ?name ;
                    Harvesting-Data:category               ?category ;
                    Harvesting-Data:status                 ?status ;
                    Harvesting-Data:weight                 ?weight ;
                    Harvesting-Data:mg_insertedOn          ?insertedOn ;
                    Harvesting-Data:mg_updatedOn           ?updatedOn .
                    OPTIONAL { ?pet Harvesting-Data:orders ?orders } .
                    OPTIONAL { ?pet Harvesting-Data:tags   ?tags } .
            }
          """;
      for (Statement statement : conn.prepareGraphQuery(query).evaluate()) {
        System.out.println(
            String.join(
                " -> ",
                statement.getSubject().stringValue(),
                statement.getPredicate().stringValue(),
                statement.getObject().stringValue()));
      }
    }
  }

  @Test
  void sparqlSelect() throws IOException {
    Repository repository = new SailRepository(new MemoryStore());

    try (RepositoryConnection conn = repository.getConnection();
        InputStream stream = readTtl("pet-store.ttl")) {
      conn.add(stream, RDFFormat.TURTLE);
      String query =
          """
          PREFIX Pet-store: <http://localhost:8080/pet-store/api/rdf/>
          PREFIX Pet: <http://localhost:8080/pet-store/api/rdf/Pet/column/>
          PREFIX Harvesting-Data: <http://localhost:8080/pet-store/api/rdf/Pet/column/>
          PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

          SELECT
              ?pet ?name ?category ?status ?weight ?intWeight ?insertedOn ?tags ?orders ?updatedOn
          WHERE {
              ?pet a Pet-store:Pet ;
                  Harvesting-Data:name                   ?name ;
                  Harvesting-Data:category               ?category ;
                  Harvesting-Data:status                 ?status ;
                  Harvesting-Data:weight                 ?weight ;
                  Harvesting-Data:mg_insertedOn          ?insertedOn ;
                  Harvesting-Data:mg_updatedOn           ?updatedOn .
                  OPTIONAL { ?pet Harvesting-Data:orders ?orders } .
                  OPTIONAL { ?pet Harvesting-Data:tags   ?tags } .

                  BIND(STRDT(STR(ROUND(?weight)), xsd:integer) AS ?intWeight)
          } ORDER BY ?pet
          """;
      for (BindingSet bindings : conn.prepareTupleQuery(query).evaluate()) {
        System.out.println(
            bindings.getBindingNames().stream()
                .map(x -> x + " -> " + bindings.getBinding(x).getValue().stringValue())
                .collect(Collectors.joining(", ")));
      }
    }
  }

  private InputStream readTtl(String path) {
    return TransformerTest.class.getResourceAsStream(path);
  }
}
