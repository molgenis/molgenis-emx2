package org.molgenis.emx2.fairmapper;

import java.util.List;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.postprocessing.MissingReferencePrimaryKeyResolver;
import org.molgenis.emx2.fairmapper.postprocessing.OntologyResolver;
import org.molgenis.emx2.fairmapper.postprocessing.SubjectColumnRemover;
import org.molgenis.emx2.fairmapper.preprocessing.MolgenisPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class HarvestingSpikeAnnotations {

  private static final String[] DATA_FILES = {"stage/Dataset.ttl", "stage/Catalog.ttl"};

  private static final String[] TABLES = {"Organisations", "Contacts", "Collections", "Catalogues"};

  @Test
  void shouldHarvest() throws InterruptedException {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.getSchema("harvesting-semantics");

    SailRepository repository = RdfFileReader.readFiles(DATA_FILES);

    preprocess(repository);

    TableStore tableStore = transform(repository, schema.getMetadata());

    postProcess(tableStore, schema.getMetadata());

    load(tableStore, schema);
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

  private static void load(TableStore tableStore, Schema schema) throws InterruptedException {
    ImportSchemaTask tasks =
        new ImportSchemaTask(tableStore, schema, false, TABLES)
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    tasks.run();
    while (tasks.isRunning()) {
      System.out.println("waiting...");
      Thread.sleep(1000);
    }
  }

  private static void preprocess(SailRepository repository) {
    new TemporalRdfPreProcessor().process(repository);
    new TypicalAgeRdfPreProcessor().process(repository);
    new MolgenisPreProcessor().process(repository);
  }

  private void postProcess(TableStore tableStore, SchemaMetadata schema) {
    // Add https://molgenis.org/resources#id based on acronym or name
    // Add https://molgenis.org/resources#type -> http://semanticscience.org/resource/SIO_001067

    // Add https://molgenis.org/organisations#id -> name
    // Add https://molgenis.org/organisations#resource
    // Add https://molgenis.org/contacts#resource

    // Resolve semantic uri of ontologies to their designated names
    resolveOntologies(tableStore, schema);

    // Resolve circular 1-to-1 dependencies from collections
    MissingReferencePrimaryKeyResolver primaryKeyResolver =
        new MissingReferencePrimaryKeyResolver(schema);
    primaryKeyResolver.resolve(tableStore, TABLES);

    // Drop _subject_ fields as those aren't in the schema
    new SubjectColumnRemover().remove(tableStore);
  }

  private void resolveOntologies(TableStore tableStore, SchemaMetadata schema) {
    OntologyResolver resolver = new OntologyResolver();

    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      Iterable<Row> rows = tableStore.readTable(table);
      for (Column column : schema.getTableMetadata(table).getColumns()) {
        if (!column.isOntology()) {
          continue;
        }

        for (Row row : rows) {
          if (row.containsName(column.getName())) {
            resolver.resolve(column, row);
          }
        }
      }
    }
  }
}
