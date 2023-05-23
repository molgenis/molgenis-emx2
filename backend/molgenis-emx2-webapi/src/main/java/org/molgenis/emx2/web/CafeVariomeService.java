package org.molgenis.emx2.web;

import static spark.Spark.get;

import org.molgenis.emx2.cafevariome.CafeVariomeApi;
import spark.Request;
import spark.Response;

public class CafeVariomeService {
  static final String CAFE_VARIOME_API_LOCATION = "/api/cafevariome";

  public static void create(MolgenisSessionManager sm) {
    get("/:schema" + CAFE_VARIOME_API_LOCATION, CafeVariomeService::getQueryResponse);
  }

  private static String getQueryResponse(Request request, Response response) {
    return CafeVariomeApi.query(request.body());
  }
}
