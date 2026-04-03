package org.molgenis.emx2.beaconv2.common.misc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/common/beaconCommonComponents.json#Handover
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Handover {

  private HandoverType handoverType;

  // An optional text including considerations on the handover link provided.
  private String note;

  // URL endpoint to where the handover process could progress (in RFC\n3986 format)
  private String url;

  public Handover() {
    this.handoverType = new HandoverType();
    this.note = "MOLGENIS EMX2 Beacon v2 handover";
    this.url = "https://molgenis.org/";
  }

  public HandoverType getHandoverType() {
    return handoverType;
  }

  public String getNote() {
    return note;
  }

  public String getUrl() {
    return url;
  }
}
