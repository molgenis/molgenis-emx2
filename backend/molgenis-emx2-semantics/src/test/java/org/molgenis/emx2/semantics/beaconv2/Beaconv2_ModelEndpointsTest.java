package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.endpoints.*;
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
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);

    // check correct empty resultset structure (must be exactly this!)
    assertTrue(
        json.contains(
            """
            "response" : {
                "resultSets" : [ ]
              }"""));
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
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1394, json.length());
  }

  @Test
  public void testGenomicVariants_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("referenceName")).thenReturn("20");
    when(request.queryParams("start")).thenReturn("2447955");
    when(request.queryParams("referenceBases")).thenReturn("c");
    when(request.queryParams("alternateBases")).thenReturn("a");
    GenomicVariants gv =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertEquals(728, json.length());
  }

  @Test
  public void testGenomicVariants_RangeQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("start")).thenReturn("2447952");
    when(request.queryParams("end")).thenReturn("2447955");
    when(request.queryParams("referenceName")).thenReturn("20");
    GenomicVariants gv =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
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
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
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
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(gv);
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertEquals(1394, json.length());
  }

  @Test
  public void testAnalyses_NoParams() throws Exception {
    Request request = mock(Request.class);
    Analyses a = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(a);
    assertTrue(json.contains("\"resultsCount\" : 4,"));
    assertEquals(2604, json.length());
  }

  @Test
  public void testAnalyses_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A05");
    Analyses a = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(a);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertEquals(728, json.length());
  }

  @Test
  public void testAnalyses_IdQuery() throws Exception {

    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A03");
    Analyses a = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(a);
    assertTrue(json.contains("\"id\" : \"A03\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1341, json.length());
  }

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Request request = mock(Request.class);
    Biosamples b = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(b);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(
        json.contains(
            """
                        "obtentionProcedure" : {
                                      "procedureCode" : {
                                        "id" : "OBI:0002654",
                                        "label" : "needle biopsy\""""));
    assertEquals(2121, json.length());
  }

  @Test
  public void testBiosamples_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("sample-example-0003");
    Biosamples b = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(b);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertEquals(728, json.length());
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("sample-example-0002");
    Biosamples b = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(b);
    assertTrue(json.contains("\"id\" : \"sample-example-0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1523, json.length());
  }

  @Test
  public void testCohorts_NoParams() throws Exception {
    Request request = mock(Request.class);
    Cohorts c = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(c);
    // 'collections' structure, different from 'resultSet'
    assertTrue(
        json.contains(
            """
            "response" : {
                "collections" : [
                  {
                    "cohortId" : "cohort0001","""));
    assertTrue(
        json.contains(
            """
            "locations" : [
                      {
                        "id" : "ISO3166:FR",
                        "label" : "France"
                      },
                      {
                        "id" : "ISO3166:ES",
                        "label" : "Spain\""""));
    assertEquals(3627, json.length());
  }

  @Test
  public void testCohorts_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("cohortId")).thenReturn("cohort0003");
    Cohorts c = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(c);
    assertTrue(
        json.contains(
            """
            "response" : {
                "collections" : [ ]
              }"""));
    assertEquals(729, json.length());
  }

  @Test
  public void testCohorts_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("cohortId")).thenReturn("cohort0001");
    Cohorts c = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(c);
    assertTrue(json.contains("\"cohortId\" : \"cohort0001\","));
    assertFalse(json.contains("\"cohortId\" : \"cohort0002\","));
    assertEquals(2163, json.length());
  }

  @Test
  public void testIndividuals_NoParams() throws Exception {
    Request request = mock(Request.class);
    Individuals i = new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(i);
    assertTrue(json.contains("\"id\" : \"Ind001\","));
    assertTrue(json.contains("\"id\" : \"Ind002\","));
    assertEquals(5916, json.length());
  }

  @Test
  public void testIndividuals_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Ind002");
    Individuals c = new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(c);
    assertFalse(json.contains("\"id\" : \"Ind001\","));
    assertTrue(json.contains("\"id\" : \"Ind002\","));
    // check if nested references and other key structures are present
    assertTrue(
        json.contains(
            """
            "measures" : [
                          {
                            "assayCode" : {
                              "id" : "EDAM:topic_3308","""));
    assertTrue(
        json.contains(
            """
            },
                         {
                           "assayCode" : {
                             "id" : "EDAM:topic_0121","""
                .indent(1)));
    assertTrue(
        json.contains(
            """
            "measurementVariable" : "TTN peptipe",
                            "measurementValue" : {
                              "value" : 6853,
                              "units" : {
                                "id" : "NCIT:C67433",
                                "label" : "Nanomole per Milligram of Protein\""""));
    assertTrue(
        json.contains(
            """
            "observationMoment" : {
                              "age" : {
                                "iso8601duration" : "P75Y9M11D\""""));
    assertEquals(3406, json.length());
  }

  @Test
  public void testIndividuals_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Ind003");
    Individuals c = new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(c);
    assertTrue(
        json.contains(
            """
            "response" : {
                "resultSets" : [ ]
              }"""));
    assertEquals(728, json.length());
  }
}
