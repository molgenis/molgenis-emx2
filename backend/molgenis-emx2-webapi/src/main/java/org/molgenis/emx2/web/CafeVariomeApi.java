package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.BeaconApi.APPLICATION_JSON_MIME_TYPE;
import static spark.Spark.post;

import org.molgenis.emx2.cafevariome.CafeVariomeService;
import spark.Request;
import spark.Response;

public class CafeVariomeApi {
  static final String CAFE_VARIOME_API_LOCATION = "/api/cafevariome";

  public static void create(MolgenisSessionManager sm) {
    post(CAFE_VARIOME_API_LOCATION, CafeVariomeApi::getQueryResponse);
  }

  private static String getQueryResponse(Request request, Response response) throws Exception {
    response.type(APPLICATION_JSON_MIME_TYPE);
    response.header("Access-Control-Allow-Origin", "*");
    return CafeVariomeService.query(request);
  }
}
