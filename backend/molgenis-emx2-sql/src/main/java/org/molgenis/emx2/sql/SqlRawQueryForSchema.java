package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRawQueryForSchema {
  private static final Logger logger = LoggerFactory.getLogger(SqlRawQueryForSchema.class);
  SqlSchema schema;

  public SqlRawQueryForSchema(SqlSchema schema) {
    this.schema = schema;
  }

  public List<Row> executeSql(String sql, Map<String, ?> parameters) {
    if (sql == null || sql.trim().equals("")) {
      return List.of();
    }
    List<Row> result = new ArrayList<>();

    // convert parameters to sql bindings
    List<Object> bindings = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\$\\{([^\\}]+)\\}"); // match ${xyz}
    Matcher matcher = pattern.matcher(sql);
    StringBuilder builder = new StringBuilder();
    int i = 0;
    while (matcher.find()) {
      bindings.add(convertParameters(parameters, matcher.group(1)));
      builder.append(sql.substring(i, matcher.start()));
      builder.append("{" + (bindings.size() - 1) + "}");
      i = matcher.end();
    }
    builder.append(sql.substring(i, sql.length()));
    final String finalSql = builder.toString();

    schema.tx(
        db -> {
          // set schema search path
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          jooq.execute("SET search_path TO {0}", name(schema.getName()));
          try {
            logger.info(schema.getDatabase().getActiveUser() + " tries to execute" + finalSql);
            Result<Record> fetch = jooq.fetch(finalSql, bindings.toArray());
            for (org.jooq.Record r : fetch) {
              result.add(new SqlRow(r));
            }
            logger.info(schema.getDatabase().getActiveUser() + " completed" + finalSql);
          } catch (SQLException sqle) {
            throw new SqlMolgenisException("query failed", sqle);
          }
        });
    return result;
  }

  private Object convertParameters(Map<String, ?> parameters, String typedParameter) {
    if (typedParameter.contains(":")) {
      ColumnType type = ColumnType.valueOf(typedParameter.split(":")[1].toUpperCase());
      String key = typedParameter.split(":")[0];
      if (!parameters.containsKey(key)) {
        throw new MolgenisException("Query expects parameter '" + key + "'");
      }
      return TypeUtils.getTypedValue(parameters.get(key), type);
    } else {
      if (!parameters.containsKey(typedParameter)) {
        throw new MolgenisException("Query expects parameter '" + typedParameter + "'");
      }
      return TypeUtils.getTypedValue(parameters.get(typedParameter), ColumnType.STRING);
    }
  }
}
