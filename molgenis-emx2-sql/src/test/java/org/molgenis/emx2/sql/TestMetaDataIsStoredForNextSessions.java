package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.examples.synthetic.*;
import org.molgenis.emx2.Schema;

import static junit.framework.TestCase.fail;

public class TestMetaDataIsStoredForNextSessions {

  static final String SCHEMA_NAME = "TestMetaDataIsStoredForNextSessions";

  static Database database;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testProductComponentsPartsModel() {
    Schema schema = database.createSchema(SCHEMA_NAME + "1");
    ProductComponentPartsExample.create(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSimpleTypesTest() {
    Schema schema = database.createSchema(SCHEMA_NAME + "2");
    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testArrayTypesTest() {
    Schema schema = database.createSchema(SCHEMA_NAME + "3");
    ArrayTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testRefAndRefArrayTypesTest() {
    Schema schema = database.createSchema(SCHEMA_NAME + "4");
    RefAndRefArrayTestExample.createRefAndRefArrayTestExample(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
