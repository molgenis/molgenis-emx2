package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp.EJP_VP_IndividualsQuery;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;
import spark.Response;

@Tag("slow")
public class Beaconv2_ModelEndpointsTest {

  static Database database;
  static Schema beaconSchema;
  static List<Table> tables;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    beaconSchema = database.dropCreateSchema("fairdatahub");
    ProfileLoader b2l = new ProfileLoader("fairdatahub/FAIRDataHub.yaml");
    b2l.load(beaconSchema, true);
    tables = List.of(beaconSchema.getTable("Individuals"));
  }

  @Test
  void testFilteringTerms() throws Exception {
    FilteringTerms filteringTerms = new FilteringTerms(database);
    String json = JsonUtil.getWriter().writeValueAsString(filteringTerms);
    assertTrue(json.contains("\"entityType\" : \"filteringterms\""));
    assertTrue(json.contains("\"filteringTerms\" : ["));
    assertTrue(json.contains("\"type\" : \"alphanumeric\","));
    assertTrue(json.contains("\"id\" : \"position_assemblyId\","));
    assertTrue(json.contains("\"scope\" : \"genomicVariations\""));
    assertTrue(json.contains("\"type\" : \"ontology\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C124261\","));
    assertTrue(json.contains("\"label\" : \"Whole Transcriptome Sequencing\","));
    assertTrue(json.contains("\"scope\" : \"runs\""));
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
    assertTrue(json.contains("clinicalRelevance"));
    assertTrue(json.contains("\"id\" : \"NCIT:C168799\""));
    assertTrue(json.contains("\"label\" : \"Pathogenic\""));
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
  }

  @Test
  public void testAnalyses_NoParams() throws Exception {
    Request request = mock(Request.class);
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"resultsCount\" : 5,"));
  }

  @Test
  public void testAnalyses_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A05");
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testAnalyses_IdQuery() throws Exception {

    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("A03");
    Analyses analyses = new Analyses(request, List.of(beaconSchema.getTable("Analyses")));
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"id\" : \"A03\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Request request = mock(Request.class);
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("obtentionProcedure"));
    assertTrue(json.contains("procedureCode"));
    assertTrue(json.contains("\"id\" : \"OBI:0002654\""));
    assertTrue(json.contains("\"label\" : \"needle biopsy\""));
  }

  @Test
  public void testBiosamples_NoHits() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Sample0003");
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("id")).thenReturn("Sample0002");
    Biosamples biosamples = new Biosamples(request, List.of(beaconSchema.getTable("Biosamples")));
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"id\" : \"Sample0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
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
  }

  @Test
  public void testCohorts_IdQuery() throws Exception {
    Request request = mock(Request.class);
    when(request.queryParams("cohortId")).thenReturn("Cohort0001");
    Cohorts cohorts = new Cohorts(request, List.of(beaconSchema.getTable("Cohorts")));
    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"cohortId\" : \"Cohort0001\","));
    assertFalse(json.contains("\"cohortId\" : \"Cohort0002\","));
  }

  @Test
  public void testIndividuals_NoParams() throws Exception {
    Request request = mock(Request.class);
    Individuals individuals =
        new Individuals(request, List.of(beaconSchema.getTable("Individuals")));
    String json = JsonUtil.getWriter().writeValueAsString(individuals);
    assertTrue(json.contains("\"id\" : \"Ind001\","));
    assertTrue(json.contains("\"id\" : \"Ind002\","));
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
                                      "id" : "EDAM:topic_3308","""
                .indent(12)));
    assertTrue(
        json.contains(
            """
                                "assayCode" : {
                                  "id" : "EDAM:topic_0121",
                                  "label" : "Proteomics\""""
                .indent(16)));
    assertTrue(
        json.contains(
            """
                                "date" : "2019-07-06",
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
                                    "iso8601duration" : "P75Y9M11D\""""
                .indent(16)));
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
  }

  @Test
  public void testRuns_NoParams() throws Exception {
    Request request = mock(Request.class);
    Runs runs = new Runs(request, List.of(beaconSchema.getTable("Runs")));
    String json = JsonUtil.getWriter().writeValueAsString(runs);
    assertTrue(json.contains("\"resultsCount\" : 5,"));
    assertTrue(
        json.contains("\"librarySource\" : {\n" + "              \"id\" : \"GENEPIO:0001966\","));
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
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "NCIT_C16576",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.SEX),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_NoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "NCIT_C16576",
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "NCIT_C20197",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.SEX, EJP_VP_IndividualsQuery.SEX),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_NoHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "NCIT_C16577",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.SEX),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_1895",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OntologyFilterSyntax_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "Orphanet_1895"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_AlsoOneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_1895",
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_1955",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE, EJP_VP_IndividualsQuery.DISEASE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_1955",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_NoHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_18730",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 31,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 33,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_NoHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 30,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeGreaterThan_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 33,
                        		"operator": ">"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_ThreeHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 50,
                        		"operator": "<"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        3);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 34,
                        		"operator": "<"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThanOrEquals_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 2,
                        		"operator": "<="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_NoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 2,
                        		"operator": "<"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 3,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_OF_ONSET),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_NoHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 91,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_OF_ONSET),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 25,
                        		"operator": ">"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_OF_ONSET),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_NoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 89,
                        		"operator": ">"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_OF_ONSET),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThanOrEquals_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 89,
                        		"operator": ">="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_OF_ONSET),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosis_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 20,
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": 2,
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_AT_DIAG, EJP_VP_IndividualsQuery.AGE_AT_DIAG),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosisLessThan_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 50,
                        		"operator": "<"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_AT_DIAG),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "TTN",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_asArray_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": ["TTN"],
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "COL7A1",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_usingAND_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "TTN",
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "COL7A1",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_usingAND_NoHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "TTN",
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "CHD7",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        0);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_CombinedArrayAndString_OneHit()
      throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": ["TTN","CHD7"],
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "COL7A1",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_usingOR_ThreeHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": ["CHD7","COL7A1"],
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        3);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_usingOR_TTNinList_TwoHits() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": ["TTN","CHD7"],
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_usingOR_TTNinList_COL7doublehit_TwoHits()
      throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": ["TTN","COL7A1"],
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.CAUSAL_GENE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnPhenotype_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "HP_0012651",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.PHENOTYPE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnPhenotype_OntologyFilterSyntax() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "HP_0012651"
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.PHENOTYPE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnPhenotypeOrDisease_OntologyFilterSyntax()
      throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": ["HP_0012651","Orphanet_1895"]
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.PHENOTYPE),
        2);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAndDisease_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"description": "Query to get count of female (NCIT_C16576) individuals with diagnostic opinion (sio:SIO_001003) Edinburgh malformation syndrome (Orphanet_1895)",
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": "NCIT_C16576",
                        		"operator": "="
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "Orphanet_1895",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.SEX, EJP_VP_IndividualsQuery.DISEASE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDiseaseAndGene_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                           "query": {
                        	 "filters": [
                        	   {
                        		 "id": "%s",
                        		 "value": "Orphanet_1895",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "%s",
                        		 "value": "COL7A1",
                        		 "operator": "="
                        	   }
                        	 ]
                           }
                         }"""
            .formatted(EJP_VP_IndividualsQuery.DISEASE, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThanAndGene_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "%s",
                        		"value": 50,
                        		"operator": "<"
                        	  },
                        	  {
                        		"id": "%s",
                        		"value": "TTN",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }"""
            .formatted(EJP_VP_IndividualsQuery.AGE_THIS_YEAR, EJP_VP_IndividualsQuery.CAUSAL_GENE),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDiseaseAndGeneAndGender_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                           "query": {
                        	 "filters": [
                        	   {
                        		 "id": "%s",
                        		 "value": "Orphanet_1873",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "%s",
                        		 "value": "CHD7",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "%s",
                        		 "value": "NCIT_C20197",
                        		 "operator": "="
                        	   }
                        	 ]
                           }
                         }"""
            .formatted(
                EJP_VP_IndividualsQuery.DISEASE,
                EJP_VP_IndividualsQuery.CAUSAL_GENE,
                EJP_VP_IndividualsQuery.SEX),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDiseaseAndGeneAndGenderWithPrefixes_OneHit()
      throws Exception {
    assertNrOfHitsFor(
        """
                        {
                           "query": {
                        	 "filters": [
                        	   {
                        		 "id": "sio:%s",
                        		 "value": "ordo:Orphanet_1873",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "obo:%s",
                        		 "value": "CHD7",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "obo:%s",
                        		 "value": "obo:NCIT_C20197",
                        		 "operator": "="
                        	   }
                        	 ]
                           }
                         }"""
            .formatted(
                EJP_VP_IndividualsQuery.DISEASE,
                EJP_VP_IndividualsQuery.CAUSAL_GENE,
                EJP_VP_IndividualsQuery.SEX),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDiseaseAndGeneAndGenderWithURLs_OneHit() throws Exception {
    assertNrOfHitsFor(
        """
                        {
                           "query": {
                        	 "filters": [
                        	   {
                        		 "id": "http://semanticscience.org/resource/%s",
                        		 "value": "http://www.orpha.net/ORDO/Orphanet_1873",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "http://purl.obolibrary.org/obo/%s",
                        		 "value": "CHD7",
                        		 "operator": "="
                        	   },
                        	   {
                        		 "id": "http://purl.obolibrary.org/obo/%s",
                        		 "value": "http://purl.obolibrary.org/obo/NCIT_C20197",
                        		 "operator": "="
                        	   }
                        	 ]
                           }
                         }"""
            .formatted(
                EJP_VP_IndividualsQuery.DISEASE,
                EJP_VP_IndividualsQuery.CAUSAL_GENE,
                EJP_VP_IndividualsQuery.SEX),
        1);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnVarCaseLevelClinInt_OneHit() throws Exception {
    // Find individuals, for which there are variants with case-level clinical relevance as
    // 'Benign'. This works via the refback field 'hasGenomicVariations'.
    assertNrOfHitsFor(
        """
                        {
                          "query": {
                        	"filters": [
                        	  {
                        		"id": "HP_0045088",
                        		"value": "NCIT_C168802",
                        		"operator": "="
                        	  }
                        	]
                          }
                        }""",
        1);
  }

  /**
   * Helper function to reduce code duplication
   *
   * @param body
   * @param hits
   * @throws JsonProcessingException
   */
  private void assertNrOfHitsFor(String body, int hits) throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    Response response = mock(Response.class);
    when(request.body()).thenReturn(body);
    String jsonResponse = new EJP_VP_IndividualsQuery(request, response, tables).getPostResponse();
    if (hits > 0) {
      assertTrue(jsonResponse.contains("\"exists\" : \"true\""));
      assertTrue(jsonResponse.contains("\"numTotalResults\" : " + hits));
    } else {
      assertTrue(jsonResponse.contains("\"exists\" : \"false\""));
      assertFalse(jsonResponse.contains("\"numTotalResults\""));
    }
  }
}
