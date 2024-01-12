package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ReceivedRequestSummary;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {

  private String beaconId;
  private String apiVersion;
  private String $schema;
  private Schemas[] returnedSchemas;
  private ReceivedRequestSummary receivedRequestSummary;
  private String returnedGranularity;

  public Meta(String $schema, String entityType) {
    this.$schema = $schema;
    this.beaconId = "org.molgenis.beaconv2";
    this.apiVersion = "v2.0.0";
    this.returnedSchemas = Arrays.asList(new Schemas(entityType)).toArray(Schemas[]::new);
    this.receivedRequestSummary = new ReceivedRequestSummary(entityType);
    this.returnedGranularity = "record";
  }

  public String getBeaconId() {
    return beaconId;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public String get$schema() {
    return $schema;
  }

  public Schemas[] getReturnedSchemas() {
    return returnedSchemas;
  }

  public ReceivedRequestSummary getReceivedRequestSummary() {
    return receivedRequestSummary;
  }

  public String getReturnedGranularity() {
    return returnedGranularity;
  }
}
