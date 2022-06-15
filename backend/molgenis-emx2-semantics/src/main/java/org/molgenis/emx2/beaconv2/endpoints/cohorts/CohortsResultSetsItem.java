package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResultSetsItem {

  String cohortId;
  String cohortName;
  String cohortType;
  OntologyTerm[] cohortDesign;
  Integer cohortSize;
  InclusionCriteria inclusionCriteria;
  OntologyTerm[] locations;
  OntologyTerm[] genders;
  OntologyTerm[] cohortDataTypes;

  public CohortsResultSetsItem() {
    super();
  }
}
