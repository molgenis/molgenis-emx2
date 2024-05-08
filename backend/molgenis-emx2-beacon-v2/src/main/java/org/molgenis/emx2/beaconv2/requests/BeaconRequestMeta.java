package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.BeaconSpec;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestMeta {

  private String apiVersion;
  private String host;
  private BeaconSpec specification;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public BeaconSpec getSpecification() {
    return specification;
  }

  public void setSpecification(BeaconSpec specification) {
    this.specification = specification;
  }
}
