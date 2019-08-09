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
import org.molgenis.emx2.examples.ProductComponentPartsExample;
import org.molgenis.utils.StopWatch;

import static junit.framework.TestCase.fail;

public class TestRoundTripMetadataDatabase {

  static Database database;

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testProductComponentsPartsModel() throws MolgenisException {
    final String SCHEMA_NAME = "TestRoundTripMetadataDatabase";
    Schema schema = database.createSchema(SCHEMA_NAME);

    // create schema
    ProductComponentPartsExample.create(schema);

    // load schema from drive
    database.clearCache();

    Schema schemaLoadedFromDisk = database.getSchema(SCHEMA_NAME);
    // cache
    schema.getTable("Product");

    // compare
    Javers javers =
        JaversBuilder.javers()
            .registerIgnoredClass(DSLContext.class)
            .registerIgnoredClass(Schema.class)
            .build();

    Diff diff =
        javers.compare(schema.getTable("Product"), schemaLoadedFromDisk.getTable("Product"));
    if (diff.hasChanges()) {
      fail("Roundtrip test failed: changes, " + diff.toString());
    }

    diff = javers.compare(schema.getTable("Component"), schemaLoadedFromDisk.getTable("Component"));
    if (diff.hasChanges()) {
      fail("Roundtrip test failed: changes, " + diff.toString());
    }

    diff = javers.compare(schema.getTable("Part"), schemaLoadedFromDisk.getTable("Part"));
    if (diff.hasChanges()) {
      fail("Roundtrip test failed: changes, " + diff.toString());
    }

    StopWatch.print("comparison returned 'equal'");
  }
}
