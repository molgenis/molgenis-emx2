package org.molgenis.emx2.beaconv2.responses.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Ontology;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ObtentionProcedure {
  Ontology procedureCode;

  public ObtentionProcedure(String procedureCode_id, String procedureCode_label) {
    this.procedureCode = new Ontology(procedureCode_id, procedureCode_label);
  }
}
