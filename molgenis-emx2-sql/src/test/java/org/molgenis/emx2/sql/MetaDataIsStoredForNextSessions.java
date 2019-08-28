package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.emx2.examples.synthetic.*;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Schema;

import static junit.framework.TestCase.fail;

public class MetaDataIsStoredForNextSessions {

  static final String SCHEMA_NAME = "MetaDataIsStoredForNextSessions";

  static Database database;

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testProductComponentsPartsModel() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "1");
    ProductComponentPartsExample.create(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSimpleTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "2");
    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testArrayTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "3");
    ArrayTypeTestExample.createSimpleTypeTest(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testRefAndRefArrayTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "4");
    RefAndRefArrayTestExample.createRefAndRefArrayTestExample(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCompsiteRefs() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "5");
    CompositeRefExample.createCompositeRefExample(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testCompsitePrimaryKeys() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "6");
    CompositePrimaryKeyExample.createCompositePrimaryKeyExample(schema.getMetadata());
    try {
      CompareTools.reloadAndCompare(database, schema);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
