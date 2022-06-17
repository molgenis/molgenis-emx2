package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSetsItem {

  String id;
  OntologyTerm sex;
  OntologyTerm ethnicity;
  OntologyTerm geographicOrigin;
  Diseases[] diseases;
  Measures[] measures;

  public IndividualsResultSetsItem() {
    super();
  }
}
