package org.molgenis.emx2.cafevariome.post.request.query;

public class EAVTriple {
  private String attribute;
  private String operator;
  private String value;

  public EAVTriple(String attribute, String operator, String value) {
    this.attribute = attribute;
    this.operator = operator;
    this.value = value;
  }

  public String getAttribute() {
    return attribute;
  }

  public String getOperator() {
    return operator;
  }

  public String getValue() {
    return value;
  }
}
