package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.utils.TypeUtils;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CaseLevelData {

  private ClinicalInterpretations[] clinicalInterpretations;
  private String individualId;

  public ClinicalInterpretations[] getClinicalInterpretations() {
    return clinicalInterpretations;
  }

  public void setClinicalInterpretations(ClinicalInterpretations[] clinicalInterpretations) {
    this.clinicalInterpretations = clinicalInterpretations;
  }

  public String getIndividualId() {
    return individualId;
  }

  public void setIndividualId(String individualId) {
    this.individualId = individualId;
  }

  public CaseLevelData() {
    super();
  }

  public static CaseLevelData[] get(Object caseLevelData) {
    if (caseLevelData == null) {
      return null;
    }
    List<Map<String, Object>> caseLevelDataCast = (List<Map<String, Object>>) caseLevelData;
    CaseLevelData[] result = new CaseLevelData[caseLevelDataCast.size()];
    for (int i = 0; i < caseLevelDataCast.size(); i++) {
      Map map = caseLevelDataCast.get(i);
      CaseLevelData caseLevelResult = new CaseLevelData();
      if (map.get("individualId") != null) {
        Map<String, Object> individual = (HashMap<String, Object>) map.get("individualId");
        String id = TypeUtils.toString(individual.get("id"));
        caseLevelResult.setIndividualId(id);
      }
      if (map.get("clinicalInterpretations") != null) {
        caseLevelResult.setClinicalInterpretations(
            ClinicalInterpretations.get(map.get("clinicalInterpretations")));
      }
      result[i] = caseLevelResult;
    }
    return result;
  }
}
