package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsFields.*;
import static org.molgenis.emx2.semantics.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.AgeAndAgeGroup;
import org.molgenis.emx2.semantics.OntologyTerm;
import org.molgenis.emx2.utils.TypeUtils;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Diseases {

  private OntologyTerm diseaseCode;
  private AgeAndAgeGroup ageOfOnset;
  private AgeAndAgeGroup ageAtDiagnosis;
  private Boolean familyHistory;
  private OntologyTerm severity;
  private OntologyTerm stage;

  public static Diseases[] get(Object diseasesObj) {
    if (diseasesObj == null) {
      return null;
    }
    List<Map<String, Object>> diseasesCast = (List<Map<String, Object>>) diseasesObj;
    Diseases[] result = new Diseases[diseasesCast.size()];
    for (int i = 0; i < diseasesCast.size(); i++) {
      Map map = diseasesCast.get(i);
      Diseases diseases = new Diseases();
      diseases.diseaseCode = mapToOntologyTerm((Map) map.get(DISEASECODE));
      diseases.ageOfOnset =
          new AgeAndAgeGroup(
              mapToOntologyTerm((Map) map.get(AGEOFONSET_AGEGROUP)),
              TypeUtils.toString(map.get(AGEOFONSET_AGE_ISO8601DURATION)));
      diseases.ageAtDiagnosis =
          new AgeAndAgeGroup(
              mapToOntologyTerm((Map) map.get(AGEATDIAGNOSIS_AGEGROUP)),
              TypeUtils.toString(map.get(AGEATDIAGNOSIS_AGE_ISO8601DURATION)));
      diseases.familyHistory = (Boolean) map.get(FAMILYHISTORY);
      diseases.severity = mapToOntologyTerm((Map) map.get(SEVERITY));
      diseases.stage = mapToOntologyTerm((Map) map.get(STAGE));
      result[i] = diseases;
    }
    return result;
  }

  public OntologyTerm getDiseaseCode() {
    return diseaseCode;
  }

  public AgeAndAgeGroup getAgeOfOnset() {
    return ageOfOnset;
  }

  public AgeAndAgeGroup getAgeAtDiagnosis() {
    return ageAtDiagnosis;
  }

  public Boolean getFamilyHistory() {
    return familyHistory;
  }

  public OntologyTerm getSeverity() {
    return severity;
  }

  public OntologyTerm getStage() {
    return stage;
  }
}
