package org.molgenis.emx2.semantics;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
public class LinkedDataServiceTest {

  static Database database;

  static Schema patientSchema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
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
}
