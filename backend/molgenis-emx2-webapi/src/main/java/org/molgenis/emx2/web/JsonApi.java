package org.molgenis.emx2.web;

import static org.molgenis.emx2.settings.ReportUtils.getReportById;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.getReportParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.*;
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
    Map<String, ?> parameters = getReportParameters(ctx);
    Map<String, Object> jsonResponse = new HashMap<>();
    for (String reportId : reports.split(",")) {
      jsonResponse.put(reportId, getReportById(reportId, schema, parameters));
    }
    if (jsonResponse.size() == 1) {
      ctx.json(jsonResponse.values().iterator().next());
    } else {
      ctx.json(jsonResponse);
    }
  }
}
