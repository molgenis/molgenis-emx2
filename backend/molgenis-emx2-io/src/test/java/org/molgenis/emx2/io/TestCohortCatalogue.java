package org.molgenis.emx2.io;

import org.junit.BeforeClass;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/** representative import file for testing */
public class TestCohortCatalogue {

  static Database database;
  static Schema ontologySchema;
  static Schema cohortsSchema;
  static Schema conceptionSchema;
  static Schema rweSchema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    ontologySchema = database.dropCreateSchema("CatalogueOntologies");
    conceptionSchema = database.dropCreateSchema("Conception");
    cohortsSchema = database.dropCreateSchema("CohortNetwork");
    rweSchema = database.dropCreateSchema("RWENetwork");
  }
}
