package org.molgenis.emx2.query;

import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Select;
import org.molgenis.emx2.Where;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.*;

public class QueryBean implements Query {
  private List<Select> selects = new ArrayList<>();
  private List<Where> wheres = new ArrayList<>();
  private List<Sort> sorts = new ArrayList<>();

  @Override
  public List<Select> getSelectList() {
    return this.selects;
  }

  @Override
  public List<Sort> getSortList() {
    return this.sorts;
  }

  @Override
  public List<Where> getWhereLists() {
    return this.wheres;
  }

  @Override
  public Select select(String... path) {
    SelectBean s = new SelectBean(this, path);
    selects.add(s);
    return s;
  }

  @Override
  public Select expand(String... path) {
    return new SelectBean(this, Select.Aggregation.EXPAND, path);
  }

  @Override
  public Query avg(String... path) {
    this.selects.add(new SelectBean(this, Select.Aggregation.AVG, path));
    return this;
  }

  @Override
  public WhereBean where(String... path) {
    WhereBean f = new WhereBean(this, path);
    wheres.add(f);
    return f;
  }

  @Override
  public Query search(String terms) {
    this.wheres.add(new WhereBean(this, Operator.SEARCH, terms));
    return this;
  }

  @Override
  public WhereBean and(String... path) {
    return this.where(path);
  }

  @Override
  public Query or() {
    this.wheres.add(new WhereBean(this, Operator.OR));
    return this;
  }

  @Override
  public WhereBean or(String... path) {
    if (!wheres.isEmpty()) this.wheres.add(new WhereBean(this, Operator.OR));
    return this.where(path);
  }

  @Override
  public Query asc(String... column) {
    this.sorts.add(new SortBean(Order.ASC, column));
    return this;
  }

  @Override
  public Query desc(String... column) {
    this.sorts.add(new SortBean(Order.DESC, column));
    return this;
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> asClass) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (!selects.isEmpty()) {
      sb.append("\nselect {");
      for (Select select : selects) {
        sb.append("\n\t" + select.toString());
      }
      sb.append("\n}");
    }

    if (!wheres.isEmpty()) {
      sb.append("\nwhere {");
      for (Where f : wheres) {
        sb.append("\n\t" + f.toString());
      }
      sb.append("\n}");
    }

    if (!sorts.isEmpty()) {
      sb.append("\nsort {");
      for (Sort srt : sorts) {
        sb.append("\n\t" + srt.toString());
      }
      sb.append("\n}");
    }

    return sb.toString();
  }
}
