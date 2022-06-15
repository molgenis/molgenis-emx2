package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ObtentionProcedure {
  OntologyTerm procedureCode;

  public ObtentionProcedure(String procedureCode_id, String procedureCode_label) {
    this.procedureCode = new OntologyTerm(procedureCode_id, procedureCode_label);
  }
}
