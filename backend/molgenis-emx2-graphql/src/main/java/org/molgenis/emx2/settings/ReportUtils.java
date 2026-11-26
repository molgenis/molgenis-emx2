package org.molgenis.emx2.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.JSONB;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.json.JsonUtil;

public final class ReportUtils {
  private ReportUtils() {
    // no instances
  }

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
    return processRows(schema.retrieveSql(getSql(report), parameters));
  }

  public static String getReportAsJson(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    return convertToJson(schema.retrieveSql(getSql(report), parameters));
  }

  public static String getReportAsJson(
      String id, Schema schema, Map<String, ?> parameters, int limit, int offset) {
    Map<String, String> report = getReportById(id, schema);
    String sql = getSql(report) + " LIMIT " + limit + " OFFSET " + offset;
    return convertToJson(schema.retrieveSql(sql, parameters));
  }

  public static List<Row> getReportAsRows(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    return schema.retrieveSql(getSql(report), parameters);
  }

  public static Integer getReportCount(String id, Schema schema, Map<String, ?> parameters) {
    Map<String, String> report = getReportById(id, schema);
    String countSql = String.format("select count(*) from (%s) as count", getSql(report));
    return schema.retrieveSql(countSql, parameters).get(0).get("count", Integer.class);
  }

  /**
   * Returns the report sql without trailing semicolons, so it can be extended with LIMIT/OFFSET or
   * wrapped in a count query.
   */
  static String getSql(Map<String, String> report) {
    String sql = report.get("sql");
    int end = sql.length();
    while (end > 0 && (sql.charAt(end - 1) == ';' || Character.isWhitespace(sql.charAt(end - 1)))) {
      end--;
    }
    return sql.substring(0, end);
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
    try {
      return JsonUtil.getJooqMapper().writeValueAsString(processRows(rows));
    } catch (Exception e) {
      throw new MolgenisException("Cannot convert sql result set to json", e);
    }
  }
}
