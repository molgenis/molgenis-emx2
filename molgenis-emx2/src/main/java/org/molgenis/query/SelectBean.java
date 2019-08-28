package org.molgenis.query;

import org.molgenis.Query;
import org.molgenis.Select;
import org.molgenis.Where;

import java.io.Serializable;
import java.util.Arrays;

public class SelectBean implements Select, Serializable {

  private transient Query query;
  private String[] path;
  private Aggregation operation = Aggregation.HIDDEN;

  public SelectBean(Query query, String... column) {
    this.query = query;
    this.path = column;
  }

  public SelectBean(Query query, Aggregation operation, String... path) {
    this(query, path);
    this.operation = operation;
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
  public Select expand(String... column) {
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

  @Override
  public String[] getPath() {
    return path;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (!Aggregation.HIDDEN.equals(operation)) sb.append(operation + Arrays.toString(path));
    else sb.append(Arrays.toString(path));
    return sb.toString();
  }

  private String[] mergePath(String[] path, String column) {
    String[] newpath = Arrays.copyOf(path, path.length + 1);
    newpath[path.length] = column;
    return newpath;
  }
}
