package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlDatabaseException;
import org.molgenis.sql.SqlQuery;
import org.molgenis.sql.SqlRow;

import java.util.List;

public class QueryImpl implements EmxQuery {
  private SqlQuery query;
  private SqlDatabase db;

  public QueryImpl(SqlDatabase db, String tableName) throws EmxException {
    this.db = db;
    try {
      this.query = db.query(tableName);
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public List<SqlRow> fetch() throws SqlDatabaseException {
    return query.retrieve();
  }
}
