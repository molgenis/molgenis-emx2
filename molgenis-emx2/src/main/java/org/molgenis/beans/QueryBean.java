package org.molgenis.beans;

import org.molgenis.*;

import java.util.*;

public class QueryBean implements Query {
  private Database db;
  private String table;
  private List<Select> selects = new ArrayList<>();
  private List<Where> wheres = new ArrayList<>();
  private List<Sort> sorts = new ArrayList<>();

  public QueryBean() {}

  public QueryBean(Database db, String table) {
    this.db = db;
    this.table = table;
  }

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
  public Select expand(String column) {
    SelectBean s = new SelectBean(this, Select.Aggregation.EXPAND, column);
    return s;
  }

  @Override
  public QueryBean avg(String... path) {
    this.selects.add(new SelectBean(this, Select.Aggregation.AVG, path));
    return this;
  }

  @Override
  public QueryBean sum(String... path) {
    this.selects.add(new SelectBean(this, Select.Aggregation.SUM, path));
    return this;
  }

  @Override
  public WhereBean where(String... path) {
    WhereBean f = new WhereBean(this, path);
    wheres.add(f);
    return f;
  }

  @Override
  public WhereBean and(String... path) {
    return this.where(path);
  }

  @Override
  public WhereBean or(String... path) {
    if (wheres.size() > 0) this.wheres.add(new WhereBean(Operator.OR));
    return this.where(path);
  }

  @Override
  public QueryBean asc(String... column) {
    this.sorts.add(new SortBean(Order.ASC, column));
    return this;
  }

  @Override
  public QueryBean desc(String... column) {
    this.sorts.add(new SortBean(Order.DESC, column));
    return this;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (selects.size() > 0) {
      sb.append("\nselect {");
      for (Select select : selects) {
        sb.append("\n\t" + select.toString());
      }
      sb.append("\n}");
    }

    if (wheres.size() > 0) {
      sb.append("\nwhere {");
      for (Where f : wheres) {
        sb.append("\n\t" + f.toString());
      }
      sb.append("\n}");
    }

    if (sorts.size() > 0) {
      sb.append("\nsort {");
      for (Sort srt : sorts) {
        sb.append("\n\t" + srt.toString());
      }
      sb.append("\n}");
    }

    return sb.toString();
  }

  @Override
  public List<Row> retrieve() throws MolgenisException {
    return db.retrieve(table, this);
  }
}
