package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Parser;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Bundle dcat-fdp deleted - FDP JSLT transforms no longer available")
class FdpShaclValidationTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Path BUNDLE_PATH = Path.of("../../fair-mappings/dcat-fdp");
  private static final Path SHACL_PATH = Path.of("../../data/_shacl/fair_data_point/v1.2");

  private static Model fdpShapes;
  private static Model catalogShapes;

  @BeforeAll
  static void loadShacl() throws Exception {
    try (InputStream fdpIn = Files.newInputStream(SHACL_PATH.resolve("FAIRDataPointShape.ttl"));
        InputStream catIn = Files.newInputStream(SHACL_PATH.resolve("CatalogShape.ttl"))) {
      fdpShapes = Rio.parse(fdpIn, "", RDFFormat.TURTLE);
      catalogShapes = Rio.parse(catIn, "", RDFFormat.TURTLE);
    }
  }

  @Test
  void testFdpRootPassesShacl() throws Exception {
    JsonNode input = loadJson("test/publish/fdp-root/input.json");
    String jslt = Files.readString(BUNDLE_PATH.resolve("src/transforms/publish/to-fdp-root.jslt"));

    JsonNode output = Parser.compileString(jslt).apply(input);
    String jsonLd = mapper.writeValueAsString(output);

    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);

    assertShaclValid(rdf, fdpShapes, "FDP root");
  }

  @Test
  void testCatalogPassesShacl() throws Exception {
    JsonNode input = loadJson("test/publish/catalog/input.json");
    String jslt =
        Files.readString(BUNDLE_PATH.resolve("src/transforms/publish/to-dcat-catalog.jslt"));

    JsonNode output = Parser.compileString(jslt).apply(input);
    String jsonLd = mapper.writeValueAsString(output);

    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);

    assertShaclValid(rdf, catalogShapes, "Catalog");
  }

  private JsonNode loadJson(String relativePath) throws Exception {
    return mapper.readTree(BUNDLE_PATH.resolve(relativePath).toFile());
  }

  private void assertShaclValid(Model data, Model shapes, String name) {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    SailRepository repo = new SailRepository(shaclSail);
    repo.init();

    try (SailRepositoryConnection conn = repo.getConnection()) {
      conn.begin();
      shapes.forEach(st -> conn.add(st, RDF4J.SHACL_SHAPE_GRAPH));
      conn.commit();

      conn.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
      data.forEach(conn::add);

      try {
        conn.commit();
      } catch (Exception e) {
        if (e.getCause() instanceof org.eclipse.rdf4j.common.exception.ValidationException ve) {
          Model report = ve.validationReportAsModel();
          fail(name + " failed SHACL validation:\n" + report);
        }
        throw e;
      }
    } finally {
      repo.shutDown();
    }
  }
}
