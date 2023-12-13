package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResultSetsItem {

  private String createDateTime; // iso 8601 format, e.g. "2017-01-17T20:33:40Z"
  private OntologyTerm[]
      dataUseConditions; // e.g. [{"id": "DUO:0000007", "label": "disease specific research",
  // "version": "17-07-2016"}]
  private String
      description; // e.g. "This dataset provides examples of the actual data in this Beacon
  // instance."
  private String externalUrl; // e.g. "example.org/wiki/Main_Page"
  private String id; // e.g. "ds01010101"
  private Object
      info; // Placeholder to allow the Beacon to return any additional information that is
  // necessary or could be of interest in relation to the query or the entry returned.
  private String name; // e.g. "Dataset with synthetic data"
  private String updateDateTime; // iso 8601 format, e.g "2017-01-17T20:33:40Z"
  private String version; // e.g. v1.1

  public String getCreateDateTime() {
    return createDateTime;
  }

  public void setCreateDateTime(String createDateTime) {
    this.createDateTime = createDateTime;
  }

  public OntologyTerm[] getDataUseConditions() {
    return dataUseConditions;
  }

  public void setDataUseConditions(OntologyTerm[] dataUseConditions) {
    this.dataUseConditions = dataUseConditions;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExternalUrl() {
    return externalUrl;
  }

  public void setExternalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Object getInfo() {
    return info;
  }

  public void setInfo(Object info) {
    this.info = info;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUpdateDateTime() {
    return updateDateTime;
  }

  public void setUpdateDateTime(String updateDateTime) {
    this.updateDateTime = updateDateTime;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
