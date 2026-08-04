package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class DCATPostProcessorTest {

  private static final String SCHEMA_NAME = DCATPostProcessorTest.class.getSimpleName();
  private SchemaMetadata schema;

  @Test
  void shouldProcessHarvestedDCAT() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.DATA_CATALOGUE
        .getImportTask(database, SCHEMA_NAME, "Catalog schema to test DCAT post processing", false)
        .run();
    schema = database.getSchema(SCHEMA_NAME).getMetadata();

    InMemoryTableStore after =
        tableStoreFromDirectory("dcat/after/", "Catalogues", "Collections", "Organisations");
    InMemoryTableStore before =
        tableStoreFromDirectory("dcat/before/", "Catalogues", "Collections", "Organisations");

    DCATPostProcessor postProcessor = new DCATPostProcessor(schema);
    postProcessor.process(before);
    tableMatches(after, before, "Catalogues");
    tableMatches(after, before, "Collections");
    tableMatches(after, before, "Organisations");
  }

  private void tableMatches(InMemoryTableStore s1, InMemoryTableStore s2, String tableName) {
    List<Row> rows1 = StreamSupport.stream(s1.readTable(tableName).spliterator(), false).toList();
    List<Row> rows2 = StreamSupport.stream(s2.readTable(tableName).spliterator(), false).toList();
    CompareTools.assertEquals(rows1, rows2);
  }

  private InMemoryTableStore tableStoreFromDirectory(String directory, String... tables) {
    InMemoryTableStore tableStore = new InMemoryTableStore();

    for (String table : tables) {
      try {
        List<String> columnNames =
            schema.getTableMetadata(table).getDownloadColumnNames().stream()
                .map(Column::getName)
                .toList();

        URL resource = DCATPostProcessor.class.getResource(directory + table + ".csv");
        Iterable<Row> rows =
            CsvTableReader.read(new File(Objects.requireNonNull(resource).getFile()));

        tableStore.writeTable(table, columnNames, rows);
      } catch (IOException e) {
        fail("Unable to read CSV file: " + directory + table, e);
      }
    }

    return tableStore;
  }
}
