package org.molgenis.emx2.fairmapper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.postprocessing.MissingReferencePrimaryKeyResolver;
import org.molgenis.emx2.fairmapper.postprocessing.OntologyResolver;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class HarvestingSpike {

  private static final String[] DATA_FILES = {"stage/Dataset.ttl", "stage/Catalog.ttl"};

  private static final String[] TABLES = {"Organisations", "Contacts", "Collections", "Catalogues"};

  @Test
  void shouldHarvest() throws InterruptedException {
    SailRepository repository = RdfFileReader.readFiles(DATA_FILES);

    RdfPreProcessor temporalPreProcessor = new TemporalRdfPreProcessor();
    temporalPreProcessor.process(repository);

    // This one is new, but also very obvious
    RdfPreProcessor agePreprocessor = new TypicalAgeRdfPreProcessor();
    agePreprocessor.process(repository);

    Database database = TestDatabaseFactory.getTestDatabase();

    QueryGenerator queryGenerator = new TableQueryGenerator();

    Schema schema = database.getSchema("harvesting");
    RdfTransformer transformer =
        new SparqlSelectRdfTransformer(queryGenerator, schema.getMetadata(), List.of(TABLES));
    TableStore tableStore = transformer.transform(repository);

    postProcess(tableStore, schema.getMetadata());
    ImportSchemaTask tasks =
        new ImportSchemaTask(tableStore, schema, false, TABLES)
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    tasks.run();
    while (tasks.isRunning()) {
      System.out.println("waiting...");
      Thread.sleep(1000);
    }
  }

  private void postProcess(TableStore tableStore, SchemaMetadata schema) {
    // Base id field off of acronym
    addIdField(tableStore);

    // Set type of collections to a hardcoded value
    addType(tableStore);

    // Resolve semantic uri of ontologies to their designated names
    resolveOntologies(tableStore, schema);

    // Resolve organisation id's from their subject iri's
    resolveOrganisationsId(tableStore);

    // Resolve circular 1-to-1 dependencies from collections
    MissingReferencePrimaryKeyResolver primaryKeyResolver =
        new MissingReferencePrimaryKeyResolver(schema);
    primaryKeyResolver.resolve(
        tableStore, TABLES);

    // The query fetches too many organisations, filter out the ones that don't hold a reference to
    // a resource, as those are unneeded
    filterUnreferencedOrganisations(tableStore);

    // Drop _subject_ fields as those aren't in the schema
    removeSubjectFromStoreRows(tableStore);
  }

  private void filterUnreferencedOrganisations(TableStore tableStore) {
    List<Row> rows =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .filter(row -> row.containsName("resource"))
            .toList();

    if (rows.isEmpty()) {
      tableStore.writeTable("Organisations", List.of(), List.of());
    } else {
      tableStore.writeTable(
          "Organisations", rows.getFirst().getColumnNames().stream().toList(), rows);
    }
  }

  private void resolveOrganisationsId(TableStore tableStore) {
    tableStore.processTable(
        "Organisations",
        (iterator, source) ->
            iterator.forEachRemaining(
                row -> row.setString("id", row.getString("organisation name"))));
  }

  /** Adds id field from acronym */
  private void addIdField(TableStore tableStore) {
    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      tableStore.processTable(
          table,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (row.getString("acronym") != null) {
                      row.set("id", row.getString("acronym"));
                    } else if (row.getString("name") != null) {
                      row.set("id", row.getString("name"));
                    } else {
                      throw new MolgenisException("Expected acronym for row: " + row);
                    }
                  }));
    }
  }

  /** Hardcode collections type to "Cohort study" semantic URI */
  private void addType(TableStore tableStore) {
    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      tableStore.processTable(
          table,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (!row.containsName("type")) {
                      row.set("type", "http://semanticscience.org/resource/SIO_001067");
                    }
                  }));
    }
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

  /** Remove _subject from the rows at the end of post-processing */
  private void removeSubjectFromStoreRows(TableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      Iterator<Row> rows = tableStore.readTable(tableName).iterator();
      if (!rows.hasNext()) {
        continue;
      }

      Row first = rows.next();
      Set<String> subjectColumns =
          first.getColumnNames().stream()
              .filter(row -> row.startsWith("_subject_"))
              .collect(Collectors.toSet());

      tableStore.processTable(
          tableName,
          (iterator, source) ->
              iterator.forEachRemaining(row -> subjectColumns.forEach(row::clear)));
    }
  }
}
