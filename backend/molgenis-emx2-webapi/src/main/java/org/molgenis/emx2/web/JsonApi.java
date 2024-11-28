package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.ZipApi.getReportParameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.*;
import org.jooq.JSONB;
import org.molgenis.emx2.MolgenisException;
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
    ObjectMapper mapper = new ObjectMapper();
    List<Map<String, String>> reportList = mapper.readValue(reportsJson, List.class);
    Map<String, Object> jsonResponse = new HashMap<>();
    for (String reportId : reports.split(",")) {
      Map<String, String> reportObject = getReportById(reportId, reportList);
      String sql = reportObject.get("sql");
      List<Row> rows = schema.retrieveSql(sql, parameters);
      List<Object> result = new ArrayList<>();
      for (Row row : rows) {
        // single json object will not be nested in key/value
        if (rows.get(0).getValueMap().size() == 1
            && rows.get(0).getValueMap().values().iterator().next() instanceof JSONB) {
          result.add(
              mapper.readTree(rows.get(0).getValueMap().values().iterator().next().toString()));
        } else {
          result.add(row.getValueMap());
        }
        jsonResponse.put(reportId, result);
      }
    }
    if (jsonResponse.size() == 1) {
      ctx.json(jsonResponse.values().iterator().next());
    } else {
      ctx.json(jsonResponse);
    }
  }

  static Map<String, String> getReportById(String reportId, List<Map<String, String>> reportList) {
    Optional<Map<String, String>> reportOptional =
        reportList.stream().filter(r -> reportId.equals(r.get("id"))).findFirst();
    if (!reportOptional.isPresent()) {
      throw new MolgenisException("Report not found id=" + reportId);
    }
    return reportOptional.get();
  }
}
