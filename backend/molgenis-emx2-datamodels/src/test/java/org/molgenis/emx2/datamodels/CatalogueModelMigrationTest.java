package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CatalogueModelMigrationTest {

  private static final String SCHEMA = "CatalogueModelMigrationTest";
  private static final String ROUND_TRIP_SCHEMA = "CatalogueModelMigrationRoundTrip";
  private static final int EXPECTED_TABLE_COUNT = 30;

  private Database database;
  private Schema schema;

  @BeforeAll
  public void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(ROUND_TRIP_SCHEMA);
    database.dropSchemaIfExists(SCHEMA);

    DataModels.Profile.DATA_CATALOGUE.getImportTask(database, SCHEMA, "test", true).run();
    schema = database.getSchema(SCHEMA);
  }

  @AfterAll
  public void tearDown() {
    database.dropSchemaIfExists(ROUND_TRIP_SCHEMA);
    database.dropSchemaIfExists(SCHEMA);
  }

  @Test
  void tableCountMatchesCurrentModel() {
    assertEquals(EXPECTED_TABLE_COUNT, schema.getTableNames().size());
  }

  @Test
  void exportImportRoundTripIsStable() throws IOException {
    Path tempDirectory = Files.createTempDirectory("catalogueModelMigration");
    try {
      MolgenisIO.toDirectory(tempDirectory, schema, false);

      Schema roundTripSchema = database.dropCreateSchema(ROUND_TRIP_SCHEMA);
      MolgenisIO.fromDirectory(tempDirectory, roundTripSchema, false);

      assertEquals(Set.copyOf(schema.getTableNames()), Set.copyOf(roundTripSchema.getTableNames()));

      for (String tableName : schema.getTableNames()) {
        int originalRowCount = schema.getTable(tableName).retrieveRows().size();
        int roundTripRowCount = roundTripSchema.getTable(tableName).retrieveRows().size();
        assertEquals(
            originalRowCount,
            roundTripRowCount,
            "Row count mismatch after round-trip for table " + tableName);
      }
    } finally {
      deleteRecursively(tempDirectory);
    }
  }

  @Test
  void adheresToDcatAndHealthRiShacl() throws IOException {
    adheresToShacl(schema, "ejp-rd-vp");
    adheresToShacl(schema, "hri-v2.0.2");
  }

  private static void deleteRecursively(Path directory) throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
