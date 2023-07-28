package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ReceivedRequestSummary;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {

  private String beaconId = "org.molgenis.beaconv2";
  private String apiVersion = "v2.0.0";
  // private String $schema;
  private Schemas[] returnedSchemas;
  private ReceivedRequestSummary receivedRequestSummary;
  private String returnedGranularity;

  public Meta(String $schema, String entityType) {
    // this.$schema = $schema;
    this.returnedSchemas = Arrays.asList(new Schemas(entityType)).toArray(Schemas[]::new);
    this.receivedRequestSummary = new ReceivedRequestSummary(entityType);
    this.returnedGranularity = "record";
  }
}
