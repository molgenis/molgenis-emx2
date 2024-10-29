package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.getReportParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

public class JsonApi {

  private JsonApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String reportPath = "/{schema}/api/reports/json";
    app.get(reportPath, JsonApi::getJsonReport);
  }

  public static void getJsonReport(Context ctx) throws JsonProcessingException {
    Schema schema = getSchema(ctx);
    String reports = ctx.queryParam("id");
    Map<String, Object> parameters = getReportParameters(ctx);

    String reportsJson = schema.getMetadata().getSetting("reports");

    List<Map<String, Object>> reportList = new ObjectMapper().readValue(reportsJson, List.class);

    Map<String, Object> jsonResponse = new HashMap<>();

    for (String reportId : reports.split(",")) {
      Map<String, Object> reportObject = reportList.get(Integer.parseInt(reportId.trim()));
      String sql = (String) reportObject.get("sql");
      String name = (String) reportObject.get("name");
      List<Row> rows = schema.retrieveSql(sql, parameters);

      List<Map<String, Object>> result = new ArrayList<>();
      for (Row row : rows) {
        result.add(row.getValueMap());
      }
      jsonResponse.put(name, result);
    }

    ctx.json(jsonResponse);
  }
}
