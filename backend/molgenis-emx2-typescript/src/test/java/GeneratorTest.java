import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class GeneratorTest {

  private static Database db;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(GeneratorTest.class.getSimpleName());
    ProfileLoader dataCatalogueLoader = new ProfileLoader("_profiles/DataCatalogue.yaml");
    dataCatalogueLoader.load(schema, true);

    //    PetStoreLoader petStoreLoader = new PetStoreLoader();
    //    petStoreLoader.load(schema, true);
  }

  @Test
  void test() {
    Generator generator = new Generator();
    generator.generate(schema, "bla");
  }
}
