package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.emx1.ConvertEmx1ToSchema;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.utils.StopWatch;

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
    File file = new File(classLoader.getResource("bbmri-nl-complete").getFile());

    RowStoreForCsvFilesDirectory store = new RowStoreForCsvFilesDirectory(file.toPath(), ',');
    StopWatch.start("begin import transaction");
    db.transaction(
        db -> {
          Schema schema = db.createSchema("testImportLegacyFormat");

          StopWatch.print("created schema");

          // load metadata
          String packagePrefix = "bbmri_nl_";
          SchemaMetadata source = ConvertEmx1ToSchema.convert(store, packagePrefix);
          // System.out.println(bbmri_nl.toString());

          StopWatch.print("converted metadata");

          schema.merge(source);

          StopWatch.print("created tables");

          assertEquals(22, schema.getTableNames().size());

          // import the data known in this stores schema
          for (String tableName : schema.getTableNames()) {
            if (store.containsTable(packagePrefix + tableName)) {
              int count = 0;
              try {
                count =
                    schema
                        .getTable(tableName)
                        .update(store.read(packagePrefix + tableName)); // actually upsert
              } catch (MolgenisException e) {
                new MolgenisException(
                    "import_error",
                    "Import failed",
                    "Import of '" + tableName + "' failed: " + e.getDetail(),
                    e);
                System.out.println("skipped " + tableName + " becausd " + e);
              }
              System.out.println("wrote " + count + " rows to " + tableName);
            }
          }
          StopWatch.print("commit");
        });
    StopWatch.print("transaction complete");
  }
}
