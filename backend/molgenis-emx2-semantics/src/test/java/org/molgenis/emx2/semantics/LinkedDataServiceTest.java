package org.molgenis.emx2.semantics;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

public class LinkedDataServiceTest {

  static Database database;
  static Schema fdpSchema;
  static Schema patientSchema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fdpSchema = database.dropCreateSchema("fdpTest");
    patientSchema = database.dropCreateSchema("patientTest");
  }

  @Test
  public void testOntologyLinks() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource("hpo_patients.xlsx").getFile()).toPath();
    MolgenisIO.importFromExcelFile(file, patientSchema, true);

    StringWriter sw = new StringWriter();
    LinkedDataService.getJsonLdForSchema(patientSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());

    sw = new StringWriter();
    LinkedDataService.getTtlForSchema(patientSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());
  }

  @Test
  public void testFairDataPointsFDP() {
    StopWatch.print("begin");

    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource("fdp.xlsx").getFile()).toPath();
    MolgenisIO.importFromExcelFile(file, fdpSchema, true);

    LinkedDataServiceTest.fdpSchema = database.getSchema("fdpTest");

    // anyway, here goes the generation
    StringWriter sw = new StringWriter();
    LinkedDataService.getJsonLdForSchema(LinkedDataServiceTest.fdpSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());

    assertEquals(2, LinkedDataServiceTest.fdpSchema.getTableNames().size());

    sw = new StringWriter();
    LinkedDataService.getTtlForSchema(LinkedDataServiceTest.fdpSchema, new PrintWriter(sw));
    System.out.println(sw.toString());
  }

  private void runImportProcedure(TableStoreForXlsxFile store, SchemaMetadata cohortSchema) {
    fdpSchema.migrate(cohortSchema);

    StopWatch.print("creation of tables complete, now starting import data");

    for (String tableName : fdpSchema.getTableNames()) {
      if (store.containsTable(tableName))
        fdpSchema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
    }
  }
}
