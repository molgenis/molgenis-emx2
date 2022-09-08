package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ClinicalInterpretations {

  private OntologyTerm category;
  private String clinicalRelevance;
  private String conditionId;
  private OntologyTerm effect;

  public static ClinicalInterpretations[] get(Object clininterpr) {
    if (clininterpr == null) {
      return null;
    }
    List<Map<String, Object>> clininterprCast = (List<Map<String, Object>>) clininterpr;
    ClinicalInterpretations[] result = new ClinicalInterpretations[clininterprCast.size()];
    for (int i = 0; i < clininterprCast.size(); i++) {
      Map map = clininterprCast.get(i);
      ClinicalInterpretations clinicalInterpretations = new ClinicalInterpretations();
      if (map.get("category") != null) {
        clinicalInterpretations.category = mapToOntologyTerm((Map) map.get("category"));
      }
      if (map.get("clinicalRelevance") != null) {
        clinicalInterpretations.clinicalRelevance = (String) map.get("clinicalRelevance");
      }
      if (map.get("conditionId") != null) {
        clinicalInterpretations.conditionId = (String) map.get("conditionId");
      }
      if (map.get("effect") != null) {
        clinicalInterpretations.effect = mapToOntologyTerm((Map) map.get("effect"));
      }
      result[i] = clinicalInterpretations;
    }
    return result;
  }
}
