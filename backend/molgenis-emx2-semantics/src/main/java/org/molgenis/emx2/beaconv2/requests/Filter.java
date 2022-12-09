package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  private String id;
  private String operator; // todo: use BeaconFilterOperator but serialization is tricky
  private String value;

  public Filter() {
    super();
  }

  public Filter(String id, String operator, String value) {
    this.id = id;
    this.operator = operator;
    this.value = value;
  }

  public String getId() {
    return id;
  }

  public String getOperator() {
    return operator;
  }

  public String getValue() {
    return value;
  }
}
