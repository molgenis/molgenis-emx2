package org.molgenis.emx2.sql;

import org.molgenis.emx2.*;

import java.util.List;

public class SqlQuery extends QueryBean {
  private SqlTableMetadata table;

  public SqlQuery(SqlTableMetadata table) {
    super();
    this.table = table;
  }

  @Override
  public List<Row> getRows() {
    return SqlQueryRowHelper.getRows(table, getSelect(), getFilter(), getSearchTerms());
  }

  @Override
  public String retrieveJsonGraph() {
    return SqlQueryGraphHelper.getJson(table, getSelect(), getFilter(), getSearchTerms());
  }
}
