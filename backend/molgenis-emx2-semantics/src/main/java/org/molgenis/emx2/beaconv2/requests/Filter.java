package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  String id;
  String operator; // todo: use BeaconFilterOperator but serialization is tricky
  String value;

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
