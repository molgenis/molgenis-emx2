package org.molgenis.beans;

import org.molgenis.Where;

import java.util.Arrays;

public class WhereBean implements Where {

  private transient QueryBean query;
  private String[] path;
  private Operator op;
  private Object[] values;

  public WhereBean(Operator op) {
    this.op = op;
  }

  public WhereBean(QueryBean parent, String... path) {
    this.query = parent;
    this.path = path;
  }

  @Override
  public QueryBean eq(String... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public QueryBean eq(Integer... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  public String toString() {
    if (values != null)
      return String.format("%s %s %s", Arrays.toString(path), op, Arrays.toString(values));
    else return op.toString();
  }
}
