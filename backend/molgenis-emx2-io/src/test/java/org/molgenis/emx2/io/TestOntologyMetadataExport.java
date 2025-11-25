package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestOntologyMetadataExport {
  static Database database;
  static Schema schema;
  private static final String schemaName = "TestOntologyMetadataExport";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.PET_STORE.getImportTask(database, schemaName, "", false).run();
    schema = database.getSchema(schemaName);
  }

  @Test
  void testExportOntologyLabelAndDescription() throws IOException {
    // set ontology metadata
    schema.getMetadata().getTableMetadata("Tag").setDescription("foo").setLabel("bar");
    // schema.migrate(schema);
    assertEquals(TableType.ONTOLOGIES, schema.getTable("Tag").getMetadata().getTableType());
    assertEquals("foo", schema.getTable("Tag").getMetadata().getDescription());
    assertEquals("bar", schema.getTable("Tag").getMetadata().getLabel());

    // test export import also keeps this info
    Path tmp = Files.createTempDirectory(null);
    Path excelFile = tmp.resolve("TestOntologyMetadataExport.xlsx");
    MolgenisIO.toExcelFile(excelFile, schema, false);
    schema = database.dropCreateSchema(schemaName);
    MolgenisIO.importFromExcelFile(excelFile, schema, false);

    assertEquals(TableType.ONTOLOGIES, schema.getTable("Tag").getMetadata().getTableType());
    assertEquals("foo", schema.getTable("Tag").getMetadata().getDescription());
    assertEquals("bar", schema.getTable("Tag").getMetadata().getLabel());
  }
}
