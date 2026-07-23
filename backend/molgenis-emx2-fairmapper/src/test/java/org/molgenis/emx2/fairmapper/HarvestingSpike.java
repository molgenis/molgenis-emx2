package org.molgenis.emx2.fairmapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.postprocessing.MissingReferencePrimaryKeyResolver;
import org.molgenis.emx2.fairmapper.postprocessing.OntologyResolver;
import org.molgenis.emx2.fairmapper.postprocessing.SubjectColumnRemover;
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
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.getSchema("harvesting");

    SailRepository repository = RdfFileReader.readFiles(DATA_FILES);

    preprocess(repository);

    TableStore tableStore = transform(repository, schema.getMetadata());

    postProcess(tableStore, schema.getMetadata());

    load(tableStore, schema);
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

  private static TableStore transform(SailRepository repository, SchemaMetadata schema) {
    QueryGenerator queryGenerator = new TableQueryGenerator();
    RdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            queryGenerator,
            schema,
            List.of("Contacts", "Collections", "Catalogues", "Organisations"));
    return transformer.transform(repository);
  }

  private static void preprocess(SailRepository repository) {
    RdfPreProcessor temporalPreProcessor = new TemporalRdfPreProcessor();
    temporalPreProcessor.process(repository);

    // This one is new, but also very obvious
    RdfPreProcessor agePreprocessor = new TypicalAgeRdfPreProcessor();
    agePreprocessor.process(repository);
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
    //    resolveOrganisationResource(tableStore);

    // Resolve circular 1-to-1 dependencies from collections
    MissingReferencePrimaryKeyResolver primaryKeyResolver =
        new MissingReferencePrimaryKeyResolver(schema);
    primaryKeyResolver.resolve(tableStore, TABLES);

    // Drop _subject_ fields as those aren't in the schema
    new SubjectColumnRemover().remove(tableStore);
    dropUnusedOrganisations(tableStore);
    System.out.println("test");
  }

  private void dropUnusedOrganisations(TableStore tableStore) {
    List<Row> filtered =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .filter(row -> row.containsName("resource"))
            .toList();

    if (filtered.isEmpty()) {
      tableStore.writeTable("Organisations", List.of(), List.of());
    } else {
      tableStore.writeTable(
          "Organisations", filtered.getFirst().getColumnNames().stream().toList(), filtered);
    }
  }

  private void resolveOrganisationResource(TableStore tableStore) {
    Map<String, Row> organisations =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .collect(Collectors.toMap(row -> row.getString("_subject_"), Function.identity()));

    for (String tableName : List.of("Collections", "Catalogues")) {
      tableStore.processTable(
          tableName,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (row.containsName("_subject_creator")) {
                      String creator = row.getString("_subject_creator");
                      Row organisationRow = organisations.get(creator);
                      if (organisationRow != null) {
                        organisationRow.setString("resource", row.getString("id"));
                      }
                    }

                    if (row.containsName("_subject_publisher")) {
                      String creator = row.getString("_subject_publisher");
                      Row organisationRow = organisations.get(creator);
                      if (organisationRow != null) {
                        organisationRow.setString("resource", row.getString("id"));
                      }
                    }
                  }));
    }

    List<Row> organisationRows =
        organisations.values().stream()
            .filter(row -> row.containsName("_subject_resource"))
            .toList();

    if (organisationRows.isEmpty()) {
      tableStore.writeTable("Organisations", List.of(), List.of());
    } else {
      tableStore.writeTable(
          "Organisations",
          organisationRows.getFirst().getColumnNames().stream().toList(),
          organisationRows);
    }
  }

  private void deduplicate(TableStore tableStore) {
    List<Row> rows =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .map(Row::getValueMap)
            .distinct()
            .map(Row::new)
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
}
