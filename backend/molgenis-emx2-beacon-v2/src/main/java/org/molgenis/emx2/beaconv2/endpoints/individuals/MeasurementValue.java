package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.semantics.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MeasurementValue {
  private Integer value;
  private OntologyTerm units;

  public MeasurementValue(Integer value, OntologyTerm units) {
    this.value = value;
    this.units = units;
  }
}
