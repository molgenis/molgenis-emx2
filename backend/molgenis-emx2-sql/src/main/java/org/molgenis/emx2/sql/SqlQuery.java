package org.molgenis.emx2.sql;

import org.molgenis.emx2.QueryBean;
import org.molgenis.emx2.Row;

import java.util.List;

public class SqlQuery extends QueryBean {
  private SqlTableMetadata table;

  public SqlQuery(SqlTableMetadata table) {
    super();
    this.table = table;
  }

  @Override
  public List<Row> getRows() {
    return SqlQueryRowsExecutor.getRows(table, getSelect(), getFilter(), getSearchTerms());
  }

  @Override
  public String retrieveJSON() {
    return SqlQueryGraphExecutor.getJson(table, getSelect(), getFilter(), getSearchTerms());
  }
}
