package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Profile;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestProfileMigrationLoader {
  public static final String TEST_PROFILE_MIGRATION = "testProfileMigration";

  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TEST_PROFILE_MIGRATION);
  }

  @Test
  void loadYamlIntoObject() {
    SchemaFromProfile petStoreProfile = new SchemaFromProfile("TestProfileMigration.yaml");
    assertEquals("Test Profile Migration", petStoreProfile.getProfiles().getName());
    assertEquals(
        Profile.MIGRATION_TEST, petStoreProfile.getProfiles().getProfileMigration().getProfile());
    assertEquals(3, petStoreProfile.getProfiles().getProfileMigration().getStep());
  }
}
