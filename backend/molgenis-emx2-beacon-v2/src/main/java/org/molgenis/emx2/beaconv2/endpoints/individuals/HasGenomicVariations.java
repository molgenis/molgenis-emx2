package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.ClinicalInterpretations;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HasGenomicVariations {

  private ClinicalInterpretations[] clinicalInterpretations;
}
