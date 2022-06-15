package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Organization {
  String id = "gcc";
  String name = "Genomics Coordination Center";
  String description = "Genomics Coordination Center at the University Medical Center Groningen";
  String address =
      "Genomics Coordination Center, Department of Genetics, University Medical Center Groningen, Groningen, The Netherlands";
  String welcomeUrl = "https://www.molgenis.org/";
  String contactUrl = "mailto:info@molgenis.org";
  String logoUrl = "https://www.molgenis.org/assets/img/logo_blue.png";
}
