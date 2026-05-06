package org.molgenis.emx2.rdf.writers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.rdf.generators.SemanticRdfGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RdfSailStoreWriterTest {
  private static final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();

  static final String BASE_URL = "http://localhost:8080";

  static Database database;
  static Schema petStore;
  static SailRepository repository;

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(RdfSailStoreWriterTest.class.getSimpleName() + "_petStore");
    DataModels.Profile.PET_STORE
        .getImportTask(
            database, RdfSailStoreWriterTest.class.getSimpleName() + "_petStore", "", true)
        .run();
    petStore = database.getSchema(RdfSailStoreWriterTest.class.getSimpleName() + "_petStore");
  }

  @BeforeEach
  void setUp() {
    // Example shaclSail to which data needs to be loaded.
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    repository = new SailRepository(shaclSail);
  }

  @AfterEach
  void tearDown() {
    repository.shutDown();
  }

  @Test
  void test() throws Exception {
    // Enhance the shaclSail.
    try (RdfSailStoreWriter writer = new RdfSailStoreWriter(repository)) {
      SemanticRdfGenerator generator = new SemanticRdfGenerator(writer, BASE_URL);
      generator.generate(petStore.getTable("Tag"));
    }

    // Validate if triples were added.
    Set<Statement> statements =
        Iterations.asSet(repository.getConnection().getStatements(null, null, null));

    assertAll(
        () ->
            assertTrue(
                statements.contains(
                    valueFactory.createStatement(
                        Values.iri(
                            "http://localhost:8080/RdfSailStoreWriterTest_petStore/api/rdf/Tag/name=red"),
                        Values.iri("http://www.w3.org/2000/01/rdf-schema#label"),
                        Values.literal("red")))),
        () ->
            assertTrue(
                statements.contains(
                    valueFactory.createStatement(
                        Values.iri(
                            "http://localhost:8080/RdfSailStoreWriterTest_petStore/api/rdf/Tag/name=red"),
                        Values.iri("http://www.w3.org/2002/07/owl#sameAs"),
                        Values.iri("https://dbpedia.org/page/Red")))));
  }
}
