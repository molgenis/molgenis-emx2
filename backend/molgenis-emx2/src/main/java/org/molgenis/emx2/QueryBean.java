package org.molgenis.emx2;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.molgenis.emx2.Operator.AND;

public class QueryBean implements Query {
  private SelectColumn select;
  private Filter filter;
  private String[] searchTerms = new String[0];

  public QueryBean() {
    this.select = new SelectColumn(null);
    this.filter = new FilterBean(AND);
  }

  public QueryBean(String field) {
    this.select = new SelectColumn(field);
    this.filter = new FilterBean(AND);
  }

  @Override
  public Query select(SelectColumn... columns) {
    this.select.subselect(columns);
    return this;
  }

  @Override
  public Query where(Filter... filters) {
    this.filter.addSubfilters(filters);
    return this;
  }

  @Override
  public List<Row> retrieveRows() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String retrieveJSON() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query search(String... terms) {
    if (this.searchTerms == null || this.searchTerms.length == 0) {
      this.searchTerms = terms;
    } else {
      this.searchTerms =
          Stream.of(this.searchTerms, terms).flatMap(Stream::of).toArray(String[]::new);
    }
    return this;
  }

  @Override
  public Query limit(int limit) {
    this.select.setLimit(limit);
    return this;
  }

  @Override
  public Query offset(int offset) {
    this.select.setOffset(offset);
    return this;
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  @Override
  public SelectColumn getSelect() {
    return select;
  }

  @Override
  public String[] getSearchTerms() {
    return searchTerms;
  }

  @Override
  public void orderBy(Map<String, Order> values) {
    this.select.setOrderBy(values);
  }

  @Override
  public Map<String, Order> getOrderBy() {
    return this.select.getOrderBy();
  }
}
