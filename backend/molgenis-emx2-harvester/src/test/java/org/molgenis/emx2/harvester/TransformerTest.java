package org.molgenis.emx2.harvester;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

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

  @Test
  void jsonLd() throws IOException {
    Repository repository = new SailRepository(new MemoryStore());

    try (RepositoryConnection conn = repository.getConnection();
        InputStream stream = readTtl("pet-store.ttl");
        OutputStream out = new ByteArrayOutputStream()) {

      RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, out);
      writer.startRDF();

      Rio.parse(stream, RDFFormat.TURTLE)
          .forEach(
              statement -> {
                try {
                  writer.handleStatement(statement);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              });

      // End the JSON-LD document
      writer.endRDF();

      String jsonld = out.toString();

      System.out.println(jsonld);
    }

    //    try {
    //      StringWriter writer = new StringWriter();
    //      RDFWriter rdfWriter = Rio.createWriter(RDFFormat.JSONLD, writer);
    //      WriterConfig config = rdfWriter.getWriterConfig();
    //      config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
    //      config.set(JSONLDSettings.COMPACT_ARRAYS, true);
    //      rdfWriter.startRDF();
    //      for (org.eclipse.rdf4j.model.Statement statement : model) {
    //        rdfWriter.handleStatement(statement);
    //      }
    //      rdfWriter.endRDF();
    //      return writer.toString();
    //    }
  }

  @Test
  void jsonLdFrames() throws IOException {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.getSchema("pet-store");
    Model rdfModel = Rio.parse(readTtl("pet-store.ttl"), RDFFormat.TURTLE);

    JsonLdFrameGenerator frameGenerator = new JsonLdFrameGenerator();
    JsonNode frame = frameGenerator.generate(schema.getMetadata());

    JsonLdFramer framer = new JsonLdFramer();
    JsonNode framedJson = framer.frame(rdfModel, frame);
    System.out.println("test");
  }

  List<JsonNode> extractGraphItems(JsonNode framedJson) {
    String jsonLdType = "@type";
    String jsonLdGraph = "@graph";

    List<JsonNode> items = new ArrayList<>();
    if (framedJson.has(jsonLdGraph) && framedJson.get(jsonLdGraph).isArray()) {
      for (JsonNode item : framedJson.get(jsonLdGraph)) {
        items.add(item);
      }
    } else if (framedJson.has(jsonLdType)) {
      items.add(framedJson);
    }
    return items;
  }

  private InputStream readTtl(String path) {
    return TransformerTest.class.getResourceAsStream(path);
  }
}
