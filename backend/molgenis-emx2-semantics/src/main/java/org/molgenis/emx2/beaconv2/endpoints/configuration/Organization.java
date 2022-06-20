package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Organization {
  private String id = "gcc";
  private String name = "Genomics Coordination Center";
  private String description = "Genomics Coordination Center at the University Medical Center Groningen";
  private String address = "Genomics Coordination Center, Department of Genetics, University Medical Center Groningen, Groningen, The Netherlands";
  private String welcomeUrl = "https://www.molgenis.org/";
  private String contactUrl = "mailto:info@molgenis.org";
  private String logoUrl = "https://www.molgenis.org/assets/img/logo_blue.png";
}
