package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.test.ArrayTypeTestExample;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.datamodels.util.CompareTools;

public class TestMetaDataIsStoredForNextSessions {

  static final String SCHEMA_NAME = "TestMetaDataIsStoredForNextSessions";

  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testProductComponentsPartsModel() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME + "1");
    ProductComponentPartsExample.create(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSimpleTypesTest() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME + "2");
    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testArrayTypesTest() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME + "3");
    ArrayTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
