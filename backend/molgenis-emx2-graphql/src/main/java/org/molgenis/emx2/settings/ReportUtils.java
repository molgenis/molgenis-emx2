package org.molgenis.emx2.settings;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

public class ReportUtils {
  public static Map<String, String> getReportById(String reportId, Schema schema) {
    try {
      String reportsJson = schema.getMetadata().getSetting("reports");
      List<Map<String, String>> reportList = new ObjectMapper().readValue(reportsJson, List.class);
      if (reportsJson != null) {
        Optional<Map<String, String>> reportOptional =
            reportList.stream()
                .filter(r -> reportId.equals(String.valueOf(r.get("id"))))
                .findFirst();
        if (reportOptional.isPresent()) {
          return reportOptional.get();
        }
      }
    } catch (Exception e) {
      // nothing to do, error will be handled below
    }
    throw new MolgenisException("Report not found id=" + reportId);
  }

  public static Object getReportById(String reportId, Schema schema, Map<String, ?> parameters)
      throws JsonProcessingException {
    Map<String, String> report = getReportById(reportId, schema);
    return processRows(schema.retrieveSql(report.get("sql"), parameters));
  }

  public static String getReportAsJson(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    return convertToJson(schema.retrieveSql(report.get("sql"), parameters));
  }

  public static String getReportAsJson(
      String id, Schema schema, Map<String, ?> parameters, int limit, int offset) {
    Map<String, String> report = getReportById(id, schema);
    String sql = report.get("sql") + " LIMIT " + limit + " OFFSET " + offset;
    return convertToJson(schema.retrieveSql(sql, parameters));
  }

  public static List<Row> getReportAsRows(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    return schema.retrieveSql(report.get("sql"), parameters);
  }

  public static Integer getReportCount(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    String countSql = String.format("select count(*) from (%s) as count", report.get("sql"));
    return schema.retrieveSql(countSql, parameters).get(0).get("count", Integer.class);
  }

  private static Object processRows(List<Row> rows) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<Map<String, Object>> result = new ArrayList<>();
    for (Row row : rows) {
      if (row.getValueMap().size() == 1) {
        Object value = row.getValueMap().values().iterator().next();
        if (value instanceof JSONB) {
          return objectMapper.readValue(value.toString(), Object.class);
        }
      }
      result.add(row.getValueMap());
    }
    return result;
  }

  private static String convertToJson(List<Row> rows) {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(
        JSON.class,
        new JsonSerializer<>() {
          @Override
          public void serialize(JSON json, JsonGenerator gen, SerializerProvider sp)
              throws IOException {
            gen.writeRawValue(json.data());
          }
        });
    mapper.registerModule(module);
    try {
      return mapper.writeValueAsString(processRows(rows));
    } catch (Exception e) {
      throw new MolgenisException("Cannot convert sql result set to json", e);
    }
  }
}
