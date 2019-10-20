package org.molgenis.emx2;

import java.io.Serializable;

public class Where {
  String path;
  Operator operator;
  Serializable[] values;

  public Where(String path, Operator operator, Serializable... values) {
    this.path = path;
    this.operator = operator;
    this.values = values;
  }

  public String getPath() {
    return path;
  }

  //  public void setPath(String path) {
  //    this.path = path;
  //  }

  public Operator getOperator() {
    return operator;
  }

  //  public void setOperator(Operator operator) {
  //    this.operator = operator;
  //  }

  public Serializable[] getValues() {
    return values;
  }

  //  public void setValues(Serializable... values) {
  //    this.values = values;
  //  }
}
