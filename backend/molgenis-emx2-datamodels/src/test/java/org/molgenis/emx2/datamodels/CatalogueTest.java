package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Constants.MG_ID;
import static org.molgenis.emx2.rdf.CustomAssertions.adheresToShacl;
import static org.molgenis.emx2.rdf.jsonld.JsonLdUtils.convertToTurtle;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator;

public class CatalogueTest extends TestLoaders {

  @Test
  void test06DataCatalogueLoader() throws Exception {
    assertEquals(24, dataCatalogue.getTableNames().size());

    // check compliance - when compliant, add: DCAT_AP_SHACL_FILES and HEALTH_RI_V2_SHACL_FILES
    adheresToShacl(dataCatalogue, "ejp-rd-vp");
    adheresToShacl(dataCatalogue, "hri-v2.0.2");
  }

  @Test
  public void test07DataCatalogueCohortStagingLoader() {
    assertEquals(20, cohortStaging.getTableNames().size());
  }

  @Disabled
  @Test
  public void test08DataCatalogueNetworkStagingLoader() {
    assertEquals(15, networkStaging.getTableNames().size());
  }

  @Test
  public void test09CatalogueFilteredDcatExport() throws Exception {
    GraphqlExecutor graphqlExecutor = new GraphqlExecutor(dataCatalogue);
    Map<String, Object> context =
        JsonLdSchemaGenerator.generateJsonLdSchemaAsMap(
            dataCatalogue.getMetadata(), "http://localhost/catalogue");

    String fullQuery = graphqlExecutor.getSelectAllQuery();
    Map fullData = graphqlExecutor.queryAsMap(fullQuery, Map.of());
    String fullTtl = convertToTurtle(context, fullData);

    String resourcesIdentifier = dataCatalogue.getTable("Resources").getMetadata().getIdentifier();
    String filteredQuery =
        String.format(
            "query($limit:Int){%s(limit:$limit){...%sAllFields}}",
            resourcesIdentifier, resourcesIdentifier);
    // todo: make useful select and filter
    Map filteredData = graphqlExecutor.queryAsMap(filteredQuery, Map.of("limit", 1));
    String filteredTtl = convertToTurtle(context, filteredData);

    assertTrue(fullTtl.contains("@prefix"));
    assertTrue(fullTtl.contains("dcat:"));
    assertTrue(filteredTtl.contains("@prefix"));
    assertTrue(fullTtl.length() > filteredTtl.length());
    assertTrue(filteredTtl.length() > filteredTtl.indexOf("@prefix") + 200);
  }

  @Test
  public void test10CatalogueCompositeKeyMgId() throws Exception {
    GraphqlExecutor graphqlExecutor = new GraphqlExecutor(dataCatalogue);
    Map<String, Object> context =
        JsonLdSchemaGenerator.generateJsonLdSchemaAsMap(
            dataCatalogue.getMetadata(), "http://localhost/catalogue");

    Map collectionEventsResult =
        graphqlExecutor.queryAsMap(
            "{CollectionEvents(filter:{resource:{id:{equals:\"testCohort1\"}}}){...CollectionEventsAllFields}}",
            Map.of());
    List<Map<String, Object>> collectionEvents =
        (List<Map<String, Object>>) collectionEventsResult.get("CollectionEvents");
    assertNotNull(collectionEvents);
    assertFalse(collectionEvents.isEmpty());
    for (Map<String, Object> event : collectionEvents) {
      String mgId = (String) event.get(MG_ID);
      assertNotNull(mgId);
      assertTrue(
          mgId.contains("resource=testCohort1"), "Expected resource=testCohort1 in: " + mgId);
      assertTrue(mgId.contains("name="), "Expected name= in: " + mgId);
    }

    Map subpopulationsResult =
        graphqlExecutor.queryAsMap(
            "{Subpopulations(filter:{resource:{id:{equals:\"testCohort1\"}}}){...SubpopulationsAllFields}}",
            Map.of());
    List<Map<String, Object>> subpopulations =
        (List<Map<String, Object>>) subpopulationsResult.get("Subpopulations");
    assertNotNull(subpopulations);
    assertFalse(subpopulations.isEmpty());
    for (Map<String, Object> subpopulation : subpopulations) {
      String mgId = (String) subpopulation.get(MG_ID);
      assertNotNull(mgId);
      assertTrue(
          mgId.contains("resource=testCohort1"), "Expected resource=testCohort1 in: " + mgId);
      assertTrue(mgId.contains("name="), "Expected name= in: " + mgId);
    }

    Map allData = graphqlExecutor.queryAsMap(graphqlExecutor.getSelectAllQuery(), Map.of());
    String ttl = convertToTurtle(context, allData);
    assertTrue(ttl.contains("resource=testCohort1"), "TTL should contain composite key identifiers");
  }
}
