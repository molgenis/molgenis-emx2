package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Filter {
  String id;
  String type;
  String operator; // todo: use BeaconFilterOperator but serialization is tricky

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getOperator() {
    return operator;
  }
}
