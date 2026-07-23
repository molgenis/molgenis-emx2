package org.molgenis.emx2.fairmapper;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class DCATHarvesterSpike {

  private static final String[] DATA_FILES = {"stage/Dataset.ttl", "stage/Catalog.ttl"};
  private static final String[] TABLES = {"Organisations", "Contacts", "Collections", "Catalogues"};

  @Test
  void shouldHarvest() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.getSchema("harvesting");
    SailRepository repository = readFiles(DATA_FILES);

    preprocess(repository);

    TableStore tableStore = transform(repository, schema.getMetadata());

    new DCATPostProcessor(schema.getMetadata()).process(tableStore);
  }

  private static void preprocess(SailRepository repository) {
    RdfPreProcessor temporalPreProcessor = new TemporalRdfPreProcessor();
    temporalPreProcessor.process(repository);

    // This one is new, but also very obvious
    RdfPreProcessor agePreprocessor = new TypicalAgeRdfPreProcessor();
    agePreprocessor.process(repository);
  }

  private static TableStore transform(SailRepository repository, SchemaMetadata schema) {
    QueryGenerator queryGenerator = new TableQueryGenerator();
    RdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            queryGenerator,
            schema,
            List.of("Contacts", "Collections", "Catalogues", "Organisations"));
    return transformer.transform(repository);
  }

  private static SailRepository readFiles(String... filenames) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection connection = repository.getConnection()) {
      for (String filename : filenames) {
        URL url = DCATHarvesterSpike.class.getResource(filename);
        connection.add(url, RDFFormat.TURTLE);
      }
      connection.commit();
    } catch (IOException e) {
      fail("Unable to set up SailRepository for petstore.ttl", e);
    }

    return repository;
  }
}
