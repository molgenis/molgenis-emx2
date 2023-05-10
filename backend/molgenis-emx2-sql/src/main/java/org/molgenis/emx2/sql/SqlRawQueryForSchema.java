package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRawQueryForSchema {
  private static final Logger logger = LoggerFactory.getLogger(SqlRawQueryForSchema.class);
  SqlSchema schema;

  public SqlRawQueryForSchema(SqlSchema schema) {
    this.schema = schema;
  }

  public List<Row> executeSql(String sql) {
    if (sql == null || sql.trim().equals("")) {
      return List.of();
    }
    List<Row> result = new ArrayList<>();
    schema.tx(
        db -> {
          // set schema search path
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          jooq.execute("SET search_path TO {0}", name(schema.getName()));
          try {
            Result<Record> fetch = jooq.fetch(sql);
            for (org.jooq.Record r : fetch) {
              result.add(new SqlRow(r));
            }
            logger.info(schema.getDatabase().getActiveUser() + " executed query " + sql);
          } catch (SQLException sqle) {
            throw new SqlMolgenisException("query failed", sqle);
          }
        });
    return result;
  }
}
