package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;
import org.molgenis.emx2.utils.TypeUtils;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Diseases {

  private OntologyTerm diseaseCode;
  private AgeOfOnset ageOfOnset;
  private Boolean familyHistory;
  private OntologyTerm severity;
  private OntologyTerm stage;

  public static Diseases[] get(Object diseases) {
    List<Map<String, Object>> diseasesCast = (List<Map<String, Object>>) diseases;
    Diseases[] result = new Diseases[diseasesCast.size()];
    for (int i = 0; i < diseasesCast.size(); i++) {
      Map map = diseasesCast.get(i);
      Diseases d = new Diseases();
      d.diseaseCode = mapToOntologyTerm((Map) map.get("diseaseCode"));
      d.ageOfOnset =
          new AgeOfOnset(
              mapToOntologyTerm((Map) map.get("ageOfOnset__ageGroup")),
              TypeUtils.toString(map.get("ageOfOnset__age__iso8601duration")));
      d.familyHistory = (Boolean) map.get("familyHistory");
      d.severity = mapToOntologyTerm((Map) map.get("severity"));
      d.stage = mapToOntologyTerm((Map) map.get("stage"));
      result[i] = d;
    }
    return result;
  }
}
