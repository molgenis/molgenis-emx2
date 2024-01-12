package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResultSetsItem {

  private String cohortId;
  private String cohortName;
  private String cohortType;
  private OntologyTerm cohortDesign;
  private Integer cohortSize;
  private InclusionCriteria inclusionCriteria;
  private OntologyTerm[] locations;
  private OntologyTerm[] genders;
  private OntologyTerm[] cohortDataTypes;

  public void setCohortId(String cohortId) {
    this.cohortId = cohortId;
  }

  public void setCohortName(String cohortName) {
    this.cohortName = cohortName;
  }

  public void setCohortType(String cohortType) {
    this.cohortType = cohortType;
  }

  public void setCohortDesign(OntologyTerm cohortDesign) {
    this.cohortDesign = cohortDesign;
  }

  public void setCohortSize(Integer cohortSize) {
    this.cohortSize = cohortSize;
  }

  public void setInclusionCriteria(InclusionCriteria inclusionCriteria) {
    this.inclusionCriteria = inclusionCriteria;
  }

  public void setLocations(OntologyTerm[] locations) {
    this.locations = locations;
  }

  public void setGenders(OntologyTerm[] genders) {
    this.genders = genders;
  }

  public void setCohortDataTypes(OntologyTerm[] cohortDataTypes) {
    this.cohortDataTypes = cohortDataTypes;
  }
}
