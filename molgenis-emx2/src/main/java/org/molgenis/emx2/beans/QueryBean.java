package org.molgenis.emx2.beans;

import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;
import java.util.*;

public class QueryBean implements Query {
  private QueryBean parent;
  private String path;
  private List<String> selectList = new ArrayList<>();
  private List<Where> whereList = new ArrayList<>();
  private List<Sort> sortList = new ArrayList<>();
  private List<String> searchList = new ArrayList<>();

  @Override
  public List<String> getSelectList() {
    if (parent != null) {
      return parent.getSelectList();
    } else {
      return selectList;
    }
  }

  @Override
  public List<Sort> getSortList() {
    return this.sortList;
  }

  @Override
  public List<Where> getWhereLists() {
    return this.whereList;
  }

  @Override
  public List<String> getSearchList() {
    return this.searchList;
  }

  public QueryBean() {}

  QueryBean(QueryBean parent, String path) {
    this.parent = parent;
    this.path = path;
  }

  Query getRootParent() {
    if (parent != null) return parent.getRootParent();
    else return this;
  }

  @Override
  public Query select(String... columns) {
    for (String column : columns) {
      if (path != null) {
        getRootParent().select(path + "/" + column);
      } else selectList.add(column);
    }
    return this;
  }

  @Override
  public Query expand(String column) {
    return new QueryBean(this, path != null ? this.path + "/" + column : column);
  }

  @Override
  public Query collapse() {
    return parent;
  }

  @Override
  public Query where(String path, Operator operator, Serializable... values) {
    if (parent != null) {
      parent.where(path, operator, values);
    } else {
      this.whereList.add(new Where(path, operator, values));
    }
    return this;
  }

  @Override
  public Query and(String path, Operator operator, Serializable... values) {
    if (parent != null) {
      parent.and(path, operator, values);
    } else {
      this.whereList.add(new Where(path, operator, values));
    }
    return this;
  }

  @Override
  public Query or(String path, Operator operator, Serializable... values) {
    if (parent != null) {
      parent.or(path, operator, values);
    } else {
      this.whereList.add(new Where(path, operator, values));
    }
    return this;
  }

  @Override
  public Query search(String terms) {
    if (parent != null) {
      parent.search(terms);
    } else {
      this.searchList.add(terms);
    }
    return this;
  }

  @Override
  public Query asc(String column) {
    if (parent != null) {
      parent.asc(column);
    } else {
      this.sortList.add(new SortBean(Order.ASC, column));
    }
    return this;
  }

  @Override
  public Query desc(String column) {
    if (parent != null) {
      parent.desc(column);
    } else {
      this.sortList.add(new SortBean(Order.DESC, column));
    }
    return this;
  }

  @Override
  public List<Row> retrieve()  {
    if (parent != null) return parent.retrieve();
    else throw new UnsupportedOperationException();
  }

  @Override
  public <E> List<E> retrieve(String columnName, Class<E> asClass) {
    if (parent != null) return parent.retrieve(columnName, asClass);
    else throw new UnsupportedOperationException();
  }

  public String toString() {
    if (parent != null) return parent.toString();
    StringBuilder sb = new StringBuilder();

    if (!selectList.isEmpty()) {
      sb.append("\nselect {");
      for (String select : selectList) {
        sb.append("\n\t" + select);
      }
      sb.append("\n}");
    }

    if (!whereList.isEmpty()) {
      sb.append("\nwhere {");
      for (Where f : whereList) {
        sb.append("\n\t" + f.toString());
      }
      sb.append("\n}");
    }

    if (!sortList.isEmpty()) {
      sb.append("\nsort {");
      for (Sort srt : sortList) {
        sb.append("\n\t" + srt.toString());
      }
      sb.append("\n}");
    }

    return sb.toString();
  }
}
