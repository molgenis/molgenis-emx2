package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.profiles.Profiles;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
public class TestProfileLoader {

  public static final String TEST_PROFILE = "testProfileSchema";
  public static final String TEST_INCLUDE = "testIncludeSchema";

  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TEST_PROFILE);
    database.dropSchemaIfExists(TEST_INCLUDE);
  }

  @Test
  void testProfileParser() {
    SchemaFromProfile schemaFromProfile = new SchemaFromProfile("TestProfile.yaml");
    Profiles profiles = schemaFromProfile.getProfiles();
    assertEquals("TestProfile", profiles.getName());
    assertEquals("Test profile with all options but small model", profiles.getDescription());
    assertEquals("JRC-CDE", profiles.getProfileTagsList().get(0));
    assertEquals("_demodata/shared-examples", profiles.getDemoDataList().get(0));
    assertEquals("_settings/datacatalogue", profiles.getSettingsList().get(0));
    assertEquals("anonymous", profiles.getSetViewPermission());
    assertEquals("user", profiles.getSetEditPermission());
    assertEquals("TestProfileOntologies", profiles.getOntologiesToFixedSchema());
    assertEquals("anonymous", profiles.getSetFixedSchemaViewPermission());
    assertEquals("user", profiles.getSetFixedSchemaEditPermission());
    assertEquals("testIncludeSchema", profiles.getFirstCreateSchemasIfMissing().get(0).getName());
  }

  @Test
  void testProfileLoader() {
    Schema testProfileSchema = database.createSchema(TEST_PROFILE);
    ProfileLoader profileLoader = new ProfileLoader("TestProfile.yaml");
    profileLoader.load(testProfileSchema, false);
    assertEquals(12, testProfileSchema.getTableNames().size());
    assertTrue(testProfileSchema.getTableNames().contains("Individuals"));
    assertFalse(testProfileSchema.getTableNames().contains("Distributions"));
    Schema testIncludeProfileSchema = database.getSchema(TEST_INCLUDE);
    assertFalse(testIncludeProfileSchema.getTableNames().contains("Individuals"));
    assertTrue(testIncludeProfileSchema.getTableNames().contains("Distribution"));
  }
}
