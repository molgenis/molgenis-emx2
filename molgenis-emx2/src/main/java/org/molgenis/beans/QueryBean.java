package org.molgenis.beans;

import org.molgenis.Query;
import org.molgenis.Select;
import org.molgenis.Sort;
import org.molgenis.Where;

import java.util.*;

public class QueryBean implements Query {
  private List<SelectBean> selects = new ArrayList<>();
  private List<WhereBean> wheres = new ArrayList<>();
  private List<SortBean> sorts = new ArrayList<>();

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
    this.wheres.add(new WhereBean(Where.Operator.OR));
    return this.where(path);
  }

  @Override
  public QueryBean asc(String... column) {
    this.sorts.add(new SortBean(Sort.Order.ASC, column));
    return this;
  }

  @Override
  public QueryBean desc(String... column) {
    this.sorts.add(new SortBean(Sort.Order.DESC, column));
    return this;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (selects.size() > 0) {
      sb.append("\nselect {");
      for (SelectBean select : selects) {
        sb.append(select.toString("\t"));
      }
      sb.append("\n}");
    }

    if (wheres.size() > 0) {
      sb.append("\nwhere {");
      for (WhereBean f : wheres) {
        sb.append("\n\t" + f.toString());
      }
      sb.append("\n}");
    }

    if (sorts.size() > 0) {
      sb.append("\nsort {");
      for (SortBean srt : sorts) {
        sb.append("\n\t" + srt.toString());
      }
      sb.append("\n}");
    }

    return sb.toString();
  }
}
