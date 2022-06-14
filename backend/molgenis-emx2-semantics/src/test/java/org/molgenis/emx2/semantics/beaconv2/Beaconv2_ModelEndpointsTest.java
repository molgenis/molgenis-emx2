package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.responses.Analyses;
import org.molgenis.emx2.beaconv2.responses.Biosamples;
import org.molgenis.emx2.beaconv2.responses.GenomicVariants;
import org.molgenis.emx2.datamodels.Beaconv2Loader;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class Beaconv2_ModelEndpointsTest {

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
  public void testGenomicVariants_NoParams() throws Exception {
    Request request = mock(Request.class);
    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);

    // check correct empty resultset structure (must be exactly this!)
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]\n" + "  }"));
    assertEquals(728, json.length());
  }

  @Test
  public void testGenomicVariants_SequenceQuery() throws Exception {

    // todo test case insensitive
    // todo test multiple valid and some invalid query parameter combinations
    // todo support and test optional arguments

    Request request = mock(Request.class);
    when(request.queryParams("referenceName")).thenReturn("20");
    when(request.queryParams("start")).thenReturn("2447955");
    when(request.queryParams("referenceBases")).thenReturn("c");
    when(request.queryParams("alternateBases")).thenReturn("g");
    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1394, json.length());
  }

  @Test
  public void testGenomicVariants_RangeQuery() throws Exception {
    // start=2447952 & end=2447955 & referenceName=20

    Request request = mock(Request.class);
    when(request.queryParams("start")).thenReturn("2447952");
    when(request.queryParams("end")).thenReturn("2447955");
    when(request.queryParams("referenceName")).thenReturn("20");
    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertEquals(1862, json.length());
  }

  @Test
  public void testGenomicVariants_GeneIdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("geneId")).thenReturn("SNRPB");
    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447946..2447950c>g\","));
    assertEquals(2330, json.length());
  }

  @Test
  public void testGenomicVariants_BracketQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("start")).thenReturn("2447945,2447951");
    when(request.queryParams("end")).thenReturn("2447952,2447953");
    when(request.queryParams("referenceName")).thenReturn("20");
    GenomicVariants gv =
        new GenomicVariants(request, Arrays.asList(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertEquals(1394, json.length());
  }

  @Test
  public void testAnalyses_NoParams() throws Exception {
    Request request = mock(Request.class);
    Analyses a = new Analyses(request, Arrays.asList(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(a);
    assertTrue(json.contains("\"resultsCount\" : 4,"));
    assertEquals(2604, json.length());
  }

  @Test
  public void testAnalyses_IdQuery() throws Exception {

    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A03");
    Analyses a = new Analyses(request, Arrays.asList(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(a);
    assertTrue(json.contains("\"id\" : \"A03\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1341, json.length());
  }

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Request request = mock(Request.class);
    Biosamples b = new Biosamples(request, Arrays.asList(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(b);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(
        json.contains(
            "\"obtentionProcedure\" : {\n"
                + "              \"procedureCode\" : {\n"
                + "                \"id\" : \"OBI0002654\",\n"
                + "                \"label\" : \"needle biopsy\""));
    assertEquals(2531, json.length());
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("sample-example-0002");
    Biosamples b = new Biosamples(request, Arrays.asList(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(b);
    assertTrue(json.contains("\"id\" : \"sample-example-0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1670, json.length());
  }
}
