package org.molgenis.sql;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.jooq.DSLContext;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.emx2.examples.*;
import org.molgenis.emx2.examples.synthetic.*;

import java.util.Collection;

import static junit.framework.TestCase.fail;

public class TestRoundTripMetadataDatabase {

  static final String SCHEMA_NAME = "TestRoundTripMetadataDatabase";

  static Database database;
  static Javers javers =
      JaversBuilder.javers()
          .registerIgnoredClass(DSLContext.class)
          .registerIgnoredClass(Schema.class)
          .build();

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testProductComponentsPartsModel() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "1");
    ProductComponentPartsExample.create(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testSimpleTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "2");
    SimpleTypeTestExample.createSimpleTypeTest(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testArrayTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "3");
    ArrayTypeTestExample.createSimpleTypeTest(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testRefArrayTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "4");
    RefArrayTestExample.createRefArrayTestExample(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testRefTypesTest() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "5");
    RefTestExample.createRefTestExmple(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testCompsiteRefs() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "6");
    CompositeRefExample.createCompositeRefExample(schema);
    reloadAndCompare(schema);
  }

  @Test
  public void testCompsitePrimaryKeys() throws MolgenisException {
    Schema schema = database.createSchema(SCHEMA_NAME + "7");
    CompositePrimaryKeyExample.createCompositePrimaryExample(schema);
    reloadAndCompare(schema);
  }

  public void reloadAndCompare(Schema schema) throws MolgenisException {
    // remember
    String schemaName = schema.getName();
    Collection<String> tableNames = schema.getTableNames();

    // empty the cache
    database.clearCache();

    // check reload from drive
    Schema schemaLoadedFromDisk = database.getSchema(schemaName);

    for (String tableName : tableNames) {
      Diff diff =
          javers.compare(schema.getTable(tableName), schemaLoadedFromDisk.getTable(tableName));

      if (diff.hasChanges()) {
        fail("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }
}
