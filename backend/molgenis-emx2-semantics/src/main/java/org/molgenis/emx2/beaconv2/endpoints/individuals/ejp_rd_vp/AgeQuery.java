package org.molgenis.emx2.beaconv2.endpoints.individuals.ejp_rd_vp;

public class AgeQuery {

  private String type;
  private int value;
  private String operator;

  public AgeQuery(String type, int value, String operator) {
    this.type = type;
    this.value = value;
    this.operator = operator;
  }

  public String getType() {
    return type;
  }

  public int getValue() {
    return value;
  }

  public String getOperator() {
    return operator;
  }
}
