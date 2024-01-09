package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsMeta extends Meta {

  private String returnedGranularity;
  private ReceivedRequestSummary receivedRequestSummary;

  public DatasetsMeta(String $schema, String entityType) {
    super($schema, entityType);
    this.returnedGranularity = "record";
    this.receivedRequestSummary = new ReceivedRequestSummary("datasets");
  }

  @Override
  public String getReturnedGranularity() {
    return returnedGranularity;
  }

  @Override
  public ReceivedRequestSummary getReceivedRequestSummary() {
    return receivedRequestSummary;
  }
}
