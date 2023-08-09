package org.molgenis.emx2.beaconv2.endpoints.info;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;
import org.molgenis.emx2.beaconv2.endpoints.configuration.Organization;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InfoResponse {
  private String id;
  private String name;
  private String apiVersion;
  private BeaconEnvironment environment;
  private Organization organization;
  private String description;
  private String version;
  private String welcomeUrl;
  private String alternativeUrl;
  private String createDateTime;
  private String updateDateTime;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String[] datasets;

  public InfoResponse(String host) {
    this.id = host;
    this.name = "MOLGENIS EMX2 Beacon v2 at " + host;
    this.apiVersion = "v2.0.0";
    this.environment = BeaconEnvironment.test;
    this.organization = new Organization();
    this.description =
        "This MOLGENIS EMX2 Beacon v2 at "
            + host
            + " is based on the GA4GH Beacon v2.0 specification, see: https://beacon-project.io/";
    this.version = "v2.0";
    this.welcomeUrl = host;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    this.alternativeUrl = "https://umcgresearch.org/w/gcc";
    this.createDateTime = dtf.format(LocalDateTime.now());
    this.updateDateTime = dtf.format(LocalDateTime.now());
    this.datasets = new String[] {};
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public BeaconEnvironment getEnvironment() {
    return environment;
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getDescription() {
    return description;
  }

  public String getVersion() {
    return version;
  }

  public String getWelcomeUrl() {
    return welcomeUrl;
  }

  public String getAlternativeUrl() {
    return alternativeUrl;
  }

  public String getCreateDateTime() {
    return createDateTime;
  }

  public String getUpdateDateTime() {
    return updateDateTime;
  }

  public String[] getDatasets() {
    return datasets;
  }
}
