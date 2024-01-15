package org.molgenis.emx2.cafevariome;

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

  static Database database;
  static Schema cafeVariomeSchema;
  static List<Table> tables;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    cafeVariomeSchema = database.dropCreateSchema("cafevariome");
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
    String responseStr = getResponseFor(CafeVariomeQueries.geneFullHeaderQuery);
    assertTrue(responseStr.contains("\"count\" : 2,"));
  }

  @Test
  void testMultiGeneQuery() throws Exception {
    String responseStr = getResponseFor(CafeVariomeQueries.multiGeneNoHeaderQuery);
    assertTrue(responseStr.contains("\"count\" : 3,"));
  }

  /**
   * Helper function to get response for a request body
   *
   * @param body
   * @return
   * @throws Exception
   */
  protected String getResponseFor(String body) throws Exception {
    Request request = mock(Request.class);
    when(request.body()).thenReturn(CafeVariomeQueries.multiGeneNoHeaderQuery);
    QueryResponse queryResponse = CafeVariomeQueryService.query(request, tables);
    return getWriter().writeValueAsString(queryResponse);
  }
}
