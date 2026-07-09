package org.molgenis.emx2.fairmapper;

import java.util.List;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.io.ImportTableTask;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class HarvestingSpike {

  @Test
  void shouldHarvest() throws InterruptedException {
    SailRepository repository = RdfFileReader.readFile("stage/Dataset.ttl");

    RdfPreProcessor temporalPreProcessor = new TemporalRdfPreProcessor();
    temporalPreProcessor.process(repository);

    // This one is new, but also very obvious
    RdfPreProcessor agePreprocessor = new TypicalAgeRdfPreProcessor();
    agePreprocessor.process(repository);

    Database database = TestDatabaseFactory.getTestDatabase();
    TableQueryGenerator queryGenerator = new TableQueryGenerator();
    SchemaMetadata schema = database.getSchema("harvesting").getMetadata();
    RdfTransformer transformer =
        new SparqlSelectRdfTransformer(queryGenerator, schema, List.of("Collections"));
    TableStore tableStore = transformer.transform(repository);

    postProcess(tableStore, schema);

    ImportTableTask tasks =
        new ImportTableTask(tableStore, schema.getTableMetadata("Collections").getTable(), true);
    tasks.run();
    while (tasks.isRunning()) {
      System.out.println("waiting...");
      Thread.sleep(1000);
    }
  }

  private void postProcess(TableStore tableStore, SchemaMetadata schema) {
    addIdField(tableStore);
    addType(tableStore);
    resolveOntologies(tableStore, schema);
    removeSubjectFromStoreRows(tableStore);
  }

  /** Adds id field from acronym */
  private void addIdField(TableStore tableStore) {
    tableStore.processTable(
        "Collections",
        (iterator, source) ->
            iterator.forEachRemaining(
                row -> {
                  if (!row.containsName("acronym")) {
                    throw new MolgenisException("Expected acronym for row: " + row);
                  }

                  row.set("id", row.getString("acronym"));
                }));
  }

  /** Hardcode collections type to "Cohort study" semantic URI */
  private void addType(TableStore tableStore) {
    tableStore.processTable(
        "Collections",
        (iterator, source) ->
            iterator.forEachRemaining(
                row -> {
                  if (!row.containsName("type")) {
                    row.set("type", "http://semanticscience.org/resource/SIO_001067");
                  }
                }));
  }

  private void resolveOntologies(TableStore tableStore, SchemaMetadata schema) {
    OntologyResolver resolver = new OntologyResolver();
    Iterable<Row> rows = tableStore.readTable("Collections");
    for (Column column : schema.getTableMetadata("Collections").getColumns()) {
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

  /** Remove _subject from the rows at the end of post-processing */
  private void removeSubjectFromStoreRows(TableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      tableStore.processTable(
          tableName, (iterator, source) -> iterator.forEachRemaining(row -> row.clear("_subject")));
    }
  }
}
