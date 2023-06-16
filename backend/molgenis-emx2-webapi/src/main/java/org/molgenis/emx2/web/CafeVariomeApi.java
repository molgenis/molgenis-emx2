package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static org.molgenis.emx2.web.BeaconApi.APPLICATION_JSON_MIME_TYPE;
import static org.molgenis.emx2.web.BeaconApi.getTableFromAllSchemas;
import static spark.Spark.post;

import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.cafevariome.CafeVariomeService;
import org.molgenis.emx2.cafevariome.response.Response;
import spark.Request;

public class CafeVariomeApi {
  static final String CAFE_VARIOME_API_LOCATION = "/api/cafevariome";

  public static void create(MolgenisSessionManager sm) {
    post(CAFE_VARIOME_API_LOCATION, CafeVariomeApi::getQueryResponse);
  }

  private static String getQueryResponse(Request request, spark.Response response)
      throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    response.header("Access-Control-Allow-Origin", "*");
    List<Table> tables = getTableFromAllSchemas("Individuals", request);
    Response responseBody = CafeVariomeService.query(request, tables);
    response.status(200);
    return getWriter().writeValueAsString(responseBody); // not whole response?
  }
}
