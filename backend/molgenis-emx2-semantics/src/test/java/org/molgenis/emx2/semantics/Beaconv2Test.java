package org.molgenis.emx2.semantics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.responses.GenomicVariants;
import org.molgenis.emx2.datamodels.Beaconv2Loader;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class Beaconv2Test {

  static Database database;
  static Schema beaconSchema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    beaconSchema = database.dropCreateSchema("beacon_v2");
    Beaconv2Loader b2l = new Beaconv2Loader();
    b2l.load(beaconSchema, true);
  }

  @Test
  public void testRoot() {}

  @Test
  public void testGenomicVariants() throws Exception {

    Request request = mock(Request.class);
    when(request.queryParams("referenceName")).thenReturn("20");
    when(request.queryParams("start")).thenReturn("2447955");

    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    System.out.println("##### " + json);
  }
}
