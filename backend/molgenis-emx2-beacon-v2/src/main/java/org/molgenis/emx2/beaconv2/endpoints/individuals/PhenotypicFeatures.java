package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.semantics.QueryHelper.mapListToOntologyTerms;
import static org.molgenis.emx2.semantics.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.semantics.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PhenotypicFeatures {

  private OntologyTerm featureType;
  private OntologyTerm[] modifiers;
  private OntologyTerm severity;

  public static PhenotypicFeatures[] get(Object phenotypicFeatures) {
    if (phenotypicFeatures == null) {
      return null;
    }
    List<Map<String, Object>> phenotypicFeaturesCast =
        (List<Map<String, Object>>) phenotypicFeatures;
    PhenotypicFeatures[] result = new PhenotypicFeatures[phenotypicFeaturesCast.size()];
    for (int i = 0; i < phenotypicFeaturesCast.size(); i++) {
      Map map = phenotypicFeaturesCast.get(i);
      PhenotypicFeatures pf = new PhenotypicFeatures();
      pf.featureType = mapToOntologyTerm((Map) map.get("featureType"));
      pf.modifiers = mapListToOntologyTerms(map.get("modifiers"));
      pf.severity = mapToOntologyTerm((Map) map.get("severity"));
      result[i] = pf;
    }
    return result;
  }
}
