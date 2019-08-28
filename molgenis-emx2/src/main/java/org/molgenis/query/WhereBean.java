package org.molgenis.query;

import org.molgenis.Query;
import org.molgenis.Where;

import java.io.Serializable;
import java.util.Arrays;

public class WhereBean implements Where, Serializable {

  private transient QueryBean query;
  private String[] path;
  private Operator op;
  private Serializable[] values;

  public WhereBean(QueryBean parent, Operator op) {
    this.query = parent;
    this.op = op;
  }

  public WhereBean(QueryBean parent, String... path) {
    this.query = parent;
    this.path = path;
    this.op = Operator.EQ;
  }

  public WhereBean(QueryBean parent, Operator op, String... terms) {
    this.query = parent;
    this.op = op;
    this.values = terms;
  }

  @Override
  public QueryBean eq(Serializable... values) {
    this.values = values;
    this.op = Operator.EQ;
    return this.query;
  }

  @Override
  public Query contains(Serializable... values) {
    this.values = values;
    this.op = Operator.ANY;
    return this.query;
  }

  @Override
  public Query search(String terms) {
    this.op = Operator.SEARCH;
    this.values = new String[] {terms};
    return this.query;
  }

  @Override
  public Operator getOperator() {
    return this.op;
  }

  @Override
  public String[] getPath() {
    return this.path;
  }

  @Override
  public Serializable[] getValues() {
    return this.values;
  }

  public String toString() {
    if (values != null)
      return String.format("%s %s %s", Arrays.toString(path), op, Arrays.toString(values));
    else return op.toString();
  }
}
