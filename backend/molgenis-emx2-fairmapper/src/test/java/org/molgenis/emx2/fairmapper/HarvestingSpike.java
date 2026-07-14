package org.molgenis.emx2.fairmapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.FileBasedQueryGenerator;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class HarvestingSpike {

  @Test
  void shouldHarvest() throws InterruptedException {
    SailRepository repository = RdfFileReader.readFiles("stage/Dataset.ttl", "stage/Catalog.ttl");

    RdfPreProcessor temporalPreProcessor = new TemporalRdfPreProcessor();
    temporalPreProcessor.process(repository);

    // This one is new, but also very obvious
    RdfPreProcessor agePreprocessor = new TypicalAgeRdfPreProcessor();
    agePreprocessor.process(repository);

    Database database = TestDatabaseFactory.getTestDatabase();

    QueryGenerator queryGenerator =
        new FileBasedQueryGenerator(
            Map.of(
                "Contacts",
                Path.of("src/test/resources/org/molgenis/emx2/fairmapper/queries/Contacts.rq"),
                "Organisations",
                Path.of("src/test/resources/org/molgenis/emx2/fairmapper/queries/Organisations.rq"),
                "Collections",
                Path.of("src/test/resources/org/molgenis/emx2/fairmapper/queries/Collections.rq"),
                "Catalogues",
                Path.of("src/test/resources/org/molgenis/emx2/fairmapper/queries/Catalogues.rq")));

    Schema schema = database.getSchema("harvesting");
    RdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            queryGenerator,
            schema.getMetadata(),
            List.of("Organisations", "Contacts", "Collections", "Catalogues"));
    TableStore tableStore = transformer.transform(repository);

    postProcess(tableStore, schema.getMetadata());
    ImportSchemaTask tasks =
        new ImportSchemaTask(
                tableStore, schema, false, "Organisations", "Contacts", "Collections", "Catalogues")
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
    resolveContactPoint(tableStore);
    resolvePublisher(tableStore);
    resolveCreator(tableStore);

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

  private void resolveCreator(TableStore tableStore) {
    Map<String, Row> organisations =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .collect(Collectors.toMap(r -> r.getString("_subject_"), r -> r));

    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      tableStore.processTable(
          table,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (!row.containsName("_subject_creator")) {
                      return;
                    }

                    String[] split = row.getString("_subject_creator").split(",");
                    for (String creatorIRI : split) {
                      Row creator = organisations.get(creatorIRI);
                      creator.setString("resource", row.getString("id"));

                      row.set("creator.resource", row.getString("id"));
                      row.set("creator.id", creator.getString("id"));
                    }
                  }));
    }
  }

  private void resolvePublisher(TableStore tableStore) {
    Map<String, Row> organisations =
        StreamSupport.stream(tableStore.readTable("Organisations").spliterator(), false)
            .collect(Collectors.toMap(r -> r.getString("_subject_"), r -> r));

    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      tableStore.processTable(
          table,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (!row.containsName("_subject_publisher")) {
                      return;
                    }

                    Row publisher = organisations.get(row.getString("_subject_publisher"));
                    publisher.setString("resource", row.getString("id"));

                    row.set("publisher.resource", row.getString("id"));
                    row.set("publisher.id", publisher.getString("id"));
                  }));
    }
  }

  private void resolveContactPoint(TableStore tableStore) {
    Map<String, Row> contacts =
        StreamSupport.stream(tableStore.readTable("Contacts").spliterator(), false)
            .collect(Collectors.toMap(r -> r.getString("_subject_"), r -> r));

    List<String> tables = List.of("Collections", "Catalogues");

    for (String table : tables) {
      tableStore.processTable(
          table,
          (iterator, source) ->
              iterator.forEachRemaining(
                  row -> {
                    if (row.containsName("contact point.first name")
                        && row.containsName("contact point.last name")
                        && row.containsName("_subject_contact point")) {
                      row.set("contact point.resource", row.getString("id"));
                      // We already have first name and last name because they are annotated

                      Row contactRow = contacts.get(row.getString("_subject_contact point"));
                      contactRow.set("resource", row.getString("id"));
                    }
                  }));

      tableStore.processTable(
          "Contacts",
          (iterator, source) ->
              iterator.forEachRemaining(
                  row ->
                      row.set(
                          "resource",
                          contacts.get(row.getString("_subject_")).getString("resource"))));
    }
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
      Row first = tableStore.readTable(tableName).iterator().next();
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
