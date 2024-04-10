package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestBody {
  private String $schema;
  private BeaconRequestMeta meta = new BeaconRequestMeta();
  private BeaconQuery query = new BeaconQuery();

  public BeaconRequestBody() {}

  public BeaconRequestBody(Request request) {
    query = new BeaconQuery(request);
  }

  public String get$schema() {
    return $schema;
  }

  public BeaconRequestMeta getMeta() {
    return meta;
  }

  public BeaconQuery getQuery() {
    return query;
  }
}
