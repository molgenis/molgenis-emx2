package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxRow;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlDatabaseException;
import org.molgenis.sql.SqlQuery;
import org.molgenis.sql.SqlRow;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
  public List<EmxRow> fetch() throws EmxException {
    try {
      return convert(query.retrieve());
    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  @Override
  public EmxQuery eq(String joinTable, String tableName, UUID... uuids) throws EmxException {
    try {
      query.eq(joinTable, tableName, uuids);
      return this;

    } catch (SqlDatabaseException e) {
      throw new EmxException(e);
    }
  }

  private List<EmxRow> convert(List<SqlRow> retrieve) {
    return retrieve.stream().map(row -> convert(row)).collect(Collectors.toList());
  }

  private EmxRow convert(SqlRow row) {
    // TODO: convert getRef:SqlRow to getRef:EmxRow
    return new EmxRow(row.getValueMap());
  }
}
