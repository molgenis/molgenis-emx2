package org.molgenis.emx2.cafevariome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.json.JsonUtil.getWriter;

import java.util.List;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.cafevariome.get.CafeVariomeIndexService;
import org.molgenis.emx2.cafevariome.get.IndexResponse;
import org.molgenis.emx2.cafevariome.post.CafeVariomeQueryService;
import org.molgenis.emx2.cafevariome.post.response.QueryResponse;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class TestCafeVariomeAPI {

  private static final String schemaName = "cafevariome";
  private static List<Table> tables;

  @BeforeAll
  public static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema cafeVariomeSchema = database.dropCreateSchema(schemaName);
    ProfileLoader b2l = new ProfileLoader("_profiles/CafeVariome.yaml");
    b2l.load(cafeVariomeSchema, true);
    tables = List.of(cafeVariomeSchema.getTable("Individuals"));
  }

  @Test
  void testIndexService() throws Exception {
    IndexResponse queryResponse = CafeVariomeIndexService.index(tables);
    String responseStr = getWriter().writeValueAsString(queryResponse);
    assertTrue(responseStr.contains("attributes_values"));
    assertTrue(responseStr.contains("assigned female at birth"));
    assertTrue(responseStr.contains("age_age_iso8601duration"));
    assertTrue(responseStr.contains("P32Y6M1D"));
    assertTrue(responseStr.contains("attributes_display_names"));
    assertTrue(responseStr.contains("values_display_names"));
  }

  @Test
  void testOneGeneQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.geneFullHeaderQuery);
    assertEquals(2, response.getSources().get(schemaName).getCount());
    assertTrue(getWriter().writeValueAsString(response).contains("Ind001"));
    assertTrue(getWriter().writeValueAsString(response).contains("Ind005"));
  }

  @Test
  void testMultiGeneQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.multiGeneQuery);
    assertEquals(3, response.getSources().get(schemaName).getCount());
    assertTrue(getWriter().writeValueAsString(response).contains("Ind001"));
    assertTrue(getWriter().writeValueAsString(response).contains("Ind002"));
    assertTrue(getWriter().writeValueAsString(response).contains("Ind005"));
  }

  @Test
  void testCombinationQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.combinationQuery);
    assertEquals(1, response.getSources().get(schemaName).getCount());
    assertTrue(getWriter().writeValueAsString(response).contains("Ind005"));
  }

  @Test
  void testHpoQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.hpoQuery);
    assertEquals(1, response.getSources().get(schemaName).getCount());
    assertTrue(getWriter().writeValueAsString(response).contains("MinInd004"));
  }

  @Test
  void testOrdoQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.ordoQuery);
    assertEquals(2, response.getSources().get(schemaName).getCount());
    assertTrue(getWriter().writeValueAsString(response).contains("Ind001"));
  }

  @Test
  void testFullQuery() throws Exception {
    QueryResponse response = getResponseFor(CafeVariomeQueries.fullQuery);
    assertEquals(0, response.getSources().get(schemaName).getCount());
  }

  /** Helper function to get response for a request body */
  protected QueryResponse getResponseFor(String body) throws Exception {
    Request request = mock(Request.class);
    when(request.body()).thenReturn(body);
    return CafeVariomeQueryService.query(request, tables);
  }
}
