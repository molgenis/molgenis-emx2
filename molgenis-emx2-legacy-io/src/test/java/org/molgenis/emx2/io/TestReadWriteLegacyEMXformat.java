package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.ConvertEmx2ToSchema;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.legacy.format.Emx1ToSchema;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.sql.SqlSchema;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static junit.framework.TestCase.assertEquals;

public class TestReadWriteLegacyEMXformat {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testImportLegacyFormat() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("bbmri-nl").getFile());

    RowStoreForCsvFilesDirectory store = new RowStoreForCsvFilesDirectory(file.toPath(), ';');

    db.transaction(
        db -> {
          Schema schema = db.createSchema("testImportLegacyFormat");

          // load metadata
          SchemaMetadata bbmri_nl = Emx1ToSchema.convert(store, "bbmri_nl_");
          // System.out.println(bbmri_nl.toString());

          schema.merge(bbmri_nl);

          assertEquals(22, schema.getTableNames().size());

          // read metadata, if available
          if (store.containsTable("molgenis")) {
            SchemaMetadata emx2Schema = ConvertEmx2ToSchema.fromRowList(store.read("molgenis"));
            schema.merge(emx2Schema);
          }
          // read data
          for (String tableName : schema.getTableNames()) {
            if (store.containsTable(tableName))
              schema.getTable(tableName).update(store.read(tableName)); // actually upsert
          }
        });
  }
}
