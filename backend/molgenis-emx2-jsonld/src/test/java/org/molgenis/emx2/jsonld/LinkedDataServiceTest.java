package org.molgenis.emx2.jsonld;

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
import org.molgenis.emx2.io.emx2.Emx2;
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
    MolgenisIO.fromExcelFile(file, patientSchema);

    StringWriter sw = new StringWriter();
    JsonLdService.jsonld(patientSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());

    sw = new StringWriter();
    JsonLdService.ttl(patientSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());
  }

  @Test
  public void testFairDataPointsFDP() {
    StopWatch.print("begin");

    // todo make this code shorter, redicolos
    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource("fdp.xlsx").getFile()).toPath();
    TableStoreForXlsxFile store = new TableStoreForXlsxFile(file);
    SchemaMetadata fdpSchema = Emx2.fromRowList(store.readTable("molgenis"));
    System.out.println(fdpSchema);
    StopWatch.print("schema loaded, now creating tables");
    database.tx(
        db -> {
          runImportProcedure(store, fdpSchema);
          StopWatch.print("import of data complete");
        });

    // fix, reload should not be need
    database.clearCache();
    LinkedDataServiceTest.fdpSchema = database.getSchema("fdpTest");

    // anyway, here goes the generation
    StringWriter sw = new StringWriter();
    JsonLdService.jsonld(LinkedDataServiceTest.fdpSchema, new PrintWriter(sw));
    System.out.println("result\r" + sw.getBuffer().toString());

    assertEquals(2, LinkedDataServiceTest.fdpSchema.getTableNames().size());

    sw = new StringWriter();
    JsonLdService.ttl(LinkedDataServiceTest.fdpSchema, new PrintWriter(sw));
    System.out.println(sw.toString());
  }

  private void runImportProcedure(TableStoreForXlsxFile store, SchemaMetadata cohortSchema) {
    fdpSchema.merge(cohortSchema);

    StopWatch.print("creation of tables complete, now starting import data");

    for (String tableName : fdpSchema.getTableNames()) {
      if (store.containsTable(tableName))
        fdpSchema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
    }
  }
}
