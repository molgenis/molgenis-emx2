package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconCountResponse {

  private String $schema;
  private BeaconResponseMeta meta;
  private BeaconSummaryResponseSection responseSummary;
  private Map<String, Object> info;
  private Handover[] beaconHandovers;

  public BeaconCountResponse(
      String host, BeaconRequestBody receivedRequest, boolean exists, int numTotalResults) {
    this.meta = new BeaconResponseMeta(host, "Individuals", "count", receivedRequest);
    this.$schema = "https://json-schema.org/draft/2020-12/schema";
    this.responseSummary = new BeaconSummaryResponseSection(exists, numTotalResults);
  }

  public String get$schema() {
    return $schema;
  }

  public void set$schema(String $schema) {
    this.$schema = $schema;
  }

  public BeaconResponseMeta getMeta() {
    return meta;
  }

  public void setMeta(BeaconResponseMeta meta) {
    this.meta = meta;
  }

  public BeaconSummaryResponseSection getResponseSummary() {
    return responseSummary;
  }

  public void setResponseSummary(BeaconSummaryResponseSection responseSummary) {
    this.responseSummary = responseSummary;
  }

  public Map<String, Object> getInfo() {
    return info;
  }

  public void setInfo(Map<String, Object> info) {
    this.info = info;
  }

  public Handover[] getBeaconHandovers() {
    return beaconHandovers;
  }

  public void setBeaconHandovers(Handover[] beaconHandovers) {
    this.beaconHandovers = beaconHandovers;
  }
}
