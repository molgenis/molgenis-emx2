package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsMeta extends Meta {

  private String returnedGranularity = "record";
  private ReceivedRequestSummary receivedRequestSummary = new ReceivedRequestSummary("datasets");

  public DatasetsMeta(String $schema, String entityType) {
    super($schema, entityType);
  }
}
