package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconCountResponse {

  private String $schema;
  private BeaconResponseMeta meta;
  private BeaconSummaryResponseSection responseSummary;
  private Map<String, Object> info;
  private Handover[] beaconHandovers;

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
