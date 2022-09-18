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
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class Beaconv2_ModelEndpointsTest {

  static Database database;
  static Schema beaconSchema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    beaconSchema = database.dropCreateSchema("fairdatahub");
    FAIRDataHubLoader b2l = new FAIRDataHubLoader();
    b2l.load(beaconSchema, true);
  }

  @Test
  public void testGenomicVariants_NoParams() throws Exception {
    Request request = mock(Request.class);
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);

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

    // todo test multiple valid and some invalid query parameter combinations
    // todo support and test optional arguments

    Request request = mock(Request.class);
    when(request.queryParams("referenceName")).thenReturn("20");
    when(request.queryParams("start")).thenReturn("2447955");
    when(request.queryParams("referenceBases")).thenReturn("c");
    when(request.queryParams("alternateBases"))
        .thenReturn("G"); // 'g' in database, test case insensitivity
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1595, json.length());
  }

  @Test
  public void testGenomicVariants_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("referenceName")).thenReturn("20");
    when(request.queryParams("start")).thenReturn("2447955");
    when(request.queryParams("referenceBases")).thenReturn("c");
    when(request.queryParams("alternateBases")).thenReturn("a");
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertFalse(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertFalse(json.contains("\"resultsCount\" : 1,"));
    assertEquals(728, json.length());
  }

  @Test
  public void testGenomicVariants_RangeQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("start")).thenReturn("2447952");
    when(request.queryParams("end")).thenReturn("2447955");
    when(request.queryParams("referenceName")).thenReturn("20");
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertEquals(3073, json.length());
  }

  @Test
  public void testGenomicVariants_GeneIdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("geneId")).thenReturn("SNRPB");
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);
    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447946..2447950c>g\","));
    assertTrue(json.contains("\"id\" : \"Orphanet:391665\""));
    assertTrue(json.contains("\"clinicalRelevance\" : \"pathogenic\""));
    assertEquals(3541, json.length());
  }

  @Test
  public void testGenomicVariants_BracketQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("start")).thenReturn("2447945,2447951");
    when(request.queryParams("end")).thenReturn("2447952,2447953");
    when(request.queryParams("referenceName")).thenReturn("20");
    GenomicVariants genomicVariations =
        new GenomicVariants(request, List.of(beaconSchema.getTable("GenomicVariations")));
    String json = JsonUtil.getWriter().writeValueAsString(genomicVariations);
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertEquals(2406, json.length());
  }

  @Test
  public void testAnalyses_NoParams() throws Exception {
    Request request = mock(Request.class);
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"resultsCount\" : 4,"));
    assertEquals(2630, json.length());
  }

  @Test
  public void testAnalyses_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A05");
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertEquals(728, json.length());
  }

  @Test
  public void testAnalyses_IdQuery() throws Exception {

    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A03");
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"id\" : \"A03\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1349, json.length());
  }

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Request request = mock(Request.class);
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(
        json.contains(
            """
                        "obtentionProcedure" : {
                                      "procedureCode" : {
                                        "id" : "OBI:0002654",
                                        "label" : "needle biopsy\""""));
    assertEquals(2105, json.length());
  }

  @Test
  public void testBiosamples_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Sample0003");
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertEquals(728, json.length());
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Sample0002");
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"id\" : \"Sample0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertEquals(1516, json.length());
  }

  @Test
  public void testCohorts_NoParams() throws Exception {
    Request request = mock(Request.class);
    Cohorts cohorts = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    // 'collections' structure, different from 'resultSet'
    assertTrue(
        json.contains(
            """
            "response" : {
                "collections" : [
                  {
                    "cohortId" : "Cohort0001","""));
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
    assertEquals(3353, json.length());
  }

  @Test
  public void testCohorts_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("cohortId")).thenReturn("Cohort0003");
    Cohorts cohorts = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
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
    when(request.queryParams("cohortId")).thenReturn("Cohort0001");
    Cohorts cohorts = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"cohortId\" : \"Cohort0001\","));
    assertFalse(json.contains("\"cohortId\" : \"Cohort0002\","));
    assertEquals(2030, json.length());
  }

  @Test
  public void testIndividuals_NoParams() throws Exception {
    Request request = mock(Request.class);
    Individuals individuals =
        new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(individuals);
    assertTrue(json.contains("\"id\" : \"Ind001\","));
    assertTrue(json.contains("\"id\" : \"Ind002\","));
    assertEquals(5918, json.length());
  }

  @Test
  public void testIndividuals_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Ind002");
    Individuals individuals =
        new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(individuals);
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
    assertEquals(3408, json.length());
  }

  @Test
  public void testIndividuals_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Ind003");
    Individuals individuals =
        new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(individuals);
    assertTrue(
        json.contains(
            """
            "response" : {
                "resultSets" : [ ]
              }"""));
    assertEquals(728, json.length());
  }

  @Test
  public void testRuns_NoParams() throws Exception {
    Request request = mock(Request.class);
    Runs runs = new Runs(request, List.of(beaconSchema.getTable("Runs")));
    String json = JsonUtil.getWriter().writeValueAsString(runs);
    assertTrue(json.contains("\"resultsCount\" : 4,"));
    assertTrue(
        json.contains("\"librarySource\" : {\n" + "              \"id\" : \"GENEPIO:0001966\","));
    assertEquals(3390, json.length());
  }

  @Test
  public void testRuns_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("SRR10903405");
    Runs runs = new Runs(request, List.of(beaconSchema.getTable("Runs")));
    String json = JsonUtil.getWriter().writeValueAsString(runs);
    assertTrue(
        json.contains(
            """
                    "response" : {
                        "resultSets" : [ ]
                      }"""));
    assertEquals(728, json.length());
  }

  @Test
  public void testRuns_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("SRR10903403");
    Runs runs = new Runs(request, List.of(beaconSchema.getTable("Runs")));
    String json = JsonUtil.getWriter().writeValueAsString(runs);
    assertTrue(json.contains("\"id\" : \"SRR10903403\","));
    assertFalse(json.contains("\"id\" : \"SRR10903401\","));
    assertFalse(json.contains("\"id\" : \"SRR10903402\","));
    assertFalse(json.contains("\"id\" : \"SRR10903404\","));
    assertEquals(1525, json.length());
  }
}
