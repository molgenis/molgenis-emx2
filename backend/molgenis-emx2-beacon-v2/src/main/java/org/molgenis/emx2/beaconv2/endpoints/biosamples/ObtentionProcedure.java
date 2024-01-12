package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ObtentionProcedure {
  private OntologyTerm procedureCode;

  public ObtentionProcedure(OntologyTerm procedureCode) {
    this.procedureCode = procedureCode;
  }
}
