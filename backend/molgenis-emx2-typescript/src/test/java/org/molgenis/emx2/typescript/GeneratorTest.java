package org.molgenis.emx2.typescript;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class GeneratorTest {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void generateTypes() {

    Schema schema = db.dropCreateSchema(GeneratorTest.class.getSimpleName() + "-PetStore");

    PetStoreLoader petStoreLoader = new PetStoreLoader();
    petStoreLoader.load(schema, true);
    Generator generator = new Generator();
    generator.generate(schema, "bla");
  }

  @Tag("slow")
  @Test
  void generateTypesForBigSchema() {

    Schema schema = db.dropCreateSchema(GeneratorTest.class.getSimpleName() + "-Catalogue");
    ProfileLoader dataCatalogueLoader = new ProfileLoader("_profiles/DataCatalogue.yaml");
    dataCatalogueLoader.load(schema, true);

    Generator generator = new Generator();
    generator.generate(schema, "bla");
  }
}
