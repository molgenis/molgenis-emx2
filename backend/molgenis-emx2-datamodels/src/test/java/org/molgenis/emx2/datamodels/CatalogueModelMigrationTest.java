package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CatalogueModelMigrationTest {

  private static final String SCHEMA = "CatalogueModelMigrationTest";
  private static final String ROUND_TRIP_SCHEMA = "CatalogueModelMigrationRoundTrip";
  private static final int EXPECTED_TABLE_COUNT = 31;

  private static final String ORGANISATIONS = "Organisations";
  private static final String BIOBANKS = "Biobanks";
  private static final String COLLECTIONS = "Collections";
  private static final String COLLECTION_FACTS = "Collection facts";
  private static final String ID = "id";
  private static final String HELD_BY = "held by";
  private static final String INFRASTRUCTURAL_CAPABILITIES = "infrastructural capabilities";
  private static final String IMPORTED_FROM = "imported from";
  private static final String SOURCE = "source";
  private static final String BBMRI_ERIC_DIRECTORY = "bbmri-eric-directory";

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

  @Test
  void demoDataLoadedWithExpectedCounts() {
    // Exact row counts of the migration-affected tables, guarding against silent data loss.
    // Each count is the real loaded number and must be > 0: the round-trip test cannot catch
    // an emptied table because 0 == 0 stays "stable" after export/import.
    // Organisations is the polymorphic supertype: its row count still includes the 5 Biobanks
    // subtype rows (a Biobank IS an Organisation). Base rows are 72 (71 + the new BBMRI-ERIC
    // Directory org, the R6 'imported from' upstream), so the total is 77 (72 base + 5 biobanks).
    assertEquals(77, schema.getTable(ORGANISATIONS).retrieveRows().size());
    assertEquals(5, schema.getTable(BIOBANKS).retrieveRows().size());
    assertEquals(188, schema.getTable("Organisation roles").retrieveRows().size());
    assertEquals(106, schema.getTable(COLLECTIONS).retrieveRows().size());
    assertEquals(20, schema.getTable(COLLECTION_FACTS).retrieveRows().size());
    assertEquals(139, schema.getTable("Contacts").retrieveRows().size());
    assertEquals(5, schema.getTable("Quality info").retrieveRows().size());
    assertEquals(1, schema.getTable("Services").retrieveRows().size());
    assertEquals(15, schema.getTable("Networks").retrieveRows().size());
  }

  @Test
  void migratedRowsCarryNewColumns() {
    // Collections.held by (custody, ref_array -> Organisations, R3): a stable collection
    // carries a non-empty held by that resolves to a real Organisations id.
    List<Row> collectionRows =
        schema.getTable(COLLECTIONS).where(f(ID, EQUALS, "RAINE")).retrieveRows();
    assertEquals(1, collectionRows.size());
    String[] heldBy = collectionRows.get(0).getStringArray(HELD_BY);
    assertNotNull(heldBy);
    assertTrue(heldBy.length > 0, "RAINE collection must have a held by organisation");
    assertEquals("UWA", heldBy[0]);
    assertEquals(
        1,
        schema.getTable(ORGANISATIONS).where(f(ID, EQUALS, heldBy[0])).retrieveRows().size(),
        "held by must resolve to a real Organisations id");

    // Qatar Biobank is now a Biobanks subtype row: query it from the Biobanks table and confirm it
    // carries the inherited 'part of' column (a legal entity that itself is an Organisations
    // record).
    List<Row> biobankRows =
        schema.getTable(BIOBANKS).where(f(ID, EQUALS, "bbmri-eric:ID:EXT_QBB")).retrieveRows();
    assertEquals(1, biobankRows.size());
    String partOf = biobankRows.get(0).getString("part of");
    assertEquals("directory_le_0014", partOf, "Qatar Biobank must reference its legal entity");

    // The same row stays reachable through the polymorphic Organisations supertype (a Biobank IS
    // an Organisation), so refs to Organisations still resolve to it.
    assertEquals(
        1,
        schema
            .getTable(ORGANISATIONS)
            .where(f(ID, EQUALS, "bbmri-eric:ID:EXT_QBB"))
            .retrieveRows()
            .size(),
        "Biobank must remain reachable via the Organisations supertype");
    assertEquals(
        1,
        schema.getTable(ORGANISATIONS).where(f(ID, EQUALS, partOf)).retrieveRows().size(),
        "part of must resolve to a real legal-entity Organisations id");

    // R6: 'imported from' marks the immediate upstream (the BBMRI-ERIC Directory) that migrated
    // this record, distinct from 'source' (the original owner, the national node).
    assertEquals(
        BBMRI_ERIC_DIRECTORY,
        biobankRows.get(0).getString(IMPORTED_FROM),
        "Qatar Biobank must carry 'imported from' = the BBMRI-ERIC Directory");
    assertEquals(
        "directory_nn_EXT",
        biobankRows.get(0).getString(SOURCE),
        "Qatar Biobank must keep its existing 'source' = the national node");

    // A native, pre-existing catalogue Collection must NOT be tagged as directory-migrated.
    List<Row> nativeCollectionRows =
        schema.getTable(COLLECTIONS).where(f(ID, EQUALS, "RAINE")).retrieveRows();
    assertEquals(1, nativeCollectionRows.size());
    assertNull(
        nativeCollectionRows.get(0).getString(IMPORTED_FROM),
        "a native catalogue Collection must have an empty 'imported from'");

    // A directory-migrated Collection must carry 'imported from'.
    List<Row> migratedCollectionRows =
        schema
            .getTable(COLLECTIONS)
            .where(f(ID, EQUALS, "bbmri-eric:ID:EXT_QBB:collection:covid19"))
            .retrieveRows();
    assertEquals(1, migratedCollectionRows.size());
    assertEquals(
        BBMRI_ERIC_DIRECTORY,
        migratedCollectionRows.get(0).getString(IMPORTED_FROM),
        "a directory-migrated Collection must carry 'imported from'");

    // The biobank-operation capability columns moved down from the Organisations base to Biobanks.
    assertTrue(
        schema
            .getTable(BIOBANKS)
            .getMetadata()
            .getColumnNames()
            .contains(INFRASTRUCTURAL_CAPABILITIES),
        "capability column must now live on the Biobanks subtype");
    assertTrue(
        !schema
            .getTable(ORGANISATIONS)
            .getMetadata()
            .getColumnNames()
            .contains(INFRASTRUCTURAL_CAPABILITIES),
        "capability column must no longer live on the Organisations base");

    // Collection facts (new table): a row carries a dimension (sex) plus a measure (donor count).
    List<Row> factRows =
        schema
            .getTable(COLLECTION_FACTS)
            .where(f(ID, EQUALS, "directory_factID_EXT_GBR-1-198_1"))
            .retrieveRows();
    assertEquals(1, factRows.size());
    assertEquals("Male", factRows.get(0).getString("sex"));
    Integer donors = factRows.get(0).getInteger("number of donors");
    assertNotNull(donors, "collection fact must carry a donor count measure");
    assertEquals(43, donors);

    // R3 custody invariant asserted against the real data: every Collection has >= 1 held by.
    long collectionsWithoutHeldBy =
        schema.getTable(COLLECTIONS).retrieveRows().stream()
            .filter(
                row -> {
                  String[] custody = row.getStringArray(HELD_BY);
                  return custody == null || custody.length == 0;
                })
            .count();
    assertEquals(
        0, collectionsWithoutHeldBy, "every Collection must have at least one held by (R3)");
  }

  private static void deleteRecursively(Path directory) throws IOException {
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
