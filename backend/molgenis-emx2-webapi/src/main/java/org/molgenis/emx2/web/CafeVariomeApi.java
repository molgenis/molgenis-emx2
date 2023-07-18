package org.molgenis.emx2.web;

import static org.molgenis.emx2.json.JsonUtil.getWriter;
import static org.molgenis.emx2.web.BeaconApi.APPLICATION_JSON_MIME_TYPE;
import static org.molgenis.emx2.web.BeaconApi.getTableFromAllSchemas;
import static spark.Spark.post;

import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.cafevariome.post.CafeVariomeQueryService;
import org.molgenis.emx2.cafevariome.post.response.CVResponse;
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
    List<Table> tables = getTableFromAllSchemas("Individuals", request);
    CVResponse responseBody = CafeVariomeQueryService.query(request, tables);
    response.status(200);
    String responseStr = getWriter().writeValueAsString(responseBody);
    return postProcessResponseString(responseStr); // not whole response?
  }

  /**
   * Postprocess default JSON into a JSON escaped string with an outer array
   * @param responseStr
   * @return
   * @throws Exception
   */
  private static String postProcessResponseString(String responseStr) throws Exception {
    String replacePt1 = "{\n  \"sources\" : {";
    String replacePt2 = "      }\n    }\n  }\n}";
    if (responseStr.contains(replacePt1) && responseStr.contains(replacePt2)) {
      // replace the front and back
      responseStr = responseStr.replace(replacePt1, "{");
      responseStr = responseStr.replace(replacePt2, "}}}");
      // escape all double quotes
      responseStr = responseStr.replace("\"", "\\\"");
      // also remove all linebreaks, whitespace, etc
      responseStr = responseStr.replaceAll("\\s+", "");
      // finally, place within quotes inside an array
      responseStr = "[\"" + responseStr + "\"]";
      return responseStr;
    } else {
      throw new Exception("Unable to postprocess output");
    }
  }
}
