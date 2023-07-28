package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Organization {
  private String id;
  private String name;
  private String description;
  private String address;
  private String welcomeUrl;
  private String contactUrl;
  private String logoUrl;

  public Organization() {
    this.id = "https://ror.org/03cv38k47";
    this.name = "Genomics Coordination Center";
    this.description =
        "Genomics Coordination Center at Dept. of Genetics at the University Medical Center Groningen";
    this.address =
        "Genomics Coordination Center, Department of Genetics, University Medical Center Groningen, Antonius Deusinglaan 1, 9713 AV Groningen, The Netherlands";
    this.welcomeUrl = "https://www.molgenis.org/";
    this.contactUrl = "mailto:info@molgenis.org";
    this.logoUrl = "https://www.molgenis.org/assets/img/logo_blue.png";
  }
}
