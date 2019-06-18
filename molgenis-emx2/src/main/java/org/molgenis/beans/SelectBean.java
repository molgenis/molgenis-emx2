package org.molgenis.beans;

import org.molgenis.Query;
import org.molgenis.Select;
import org.molgenis.Where;
import java.util.Arrays;

public class SelectBean implements Select {

  // root of this selectionQueryBean query;
  Query query;
  // currently selected path
  String[] path;
  // operation
  Aggregation op = Aggregation.HIDDEN;

  public SelectBean(Query query, String... column) {
    this.query = query;
    this.path = column;
  }

  public SelectBean(Query query, Aggregation op, String... path) {
    this(query, path);
    this.op = op;
  }

  @Override
  public Select include(String column) {
    query.select(mergePath(path, column));
    return this;
  }

  @Override
  public Select avg(String column) {
    query.avg(mergePath(path, column));
    return this;
  }

  @Override
  public Select expand(String column) {
    return query.expand(column);
  }

  @Override
  public Select select(String column) {
    return query.select(column);
  }

  @Override
  public Where where(String... path) {
    return query.where(path);
  }

  @Override
  public Query eq(String... values) {
    return query.where(path).eq(values);
  }

  @Override
  public Query eq(Integer... values) {
    return query.where(path).eq(values);
  }

  @Override
  public Query asc(String... path) {
    return query.asc(path);
  }

  @Override
  public Query desc(String... path) {
    return query.desc(path);
  }

  public String toString() {
    return toString("");
  }

  public String toString(String tabs) {
    StringBuilder sb = new StringBuilder();
    if (!Aggregation.HIDDEN.equals(op)) sb.append("\n" + tabs + op + Arrays.toString(path));
    else sb.append("\n" + tabs + Arrays.toString(path));
    return sb.toString();
  }

  private String[] mergePath(String path[], String column) {
    String[] newpath = Arrays.copyOf(path, path.length + 1);
    newpath[path.length] = column;
    return newpath;
  }
}
