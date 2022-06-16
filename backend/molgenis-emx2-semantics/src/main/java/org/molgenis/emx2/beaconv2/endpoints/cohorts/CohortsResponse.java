package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapListToOntologyTerms;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.selectColumns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  CohortsResultSetsItem[] collections;

  // query parameters, ignore from output
  @JsonIgnore String qId;

  public CohortsResponse(Request request, List<Table> tables) throws Exception {

    qId = request.queryParams("cohortId");
    List<CohortsResultSetsItem> cohortList = new ArrayList<>();

    for (Table t : tables) {
      Query q = t.query();
      selectColumns(t, q);

      if (qId != null) {
        q.where(f("cohortId", EQUALS, qId));
      }

      String json = q.retrieveJSON();
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);
      List<Map<String, Object>> cohortListFromJSON =
          (List<Map<String, Object>>) result.get("Cohorts");

      if (cohortListFromJSON != null) {
        for (Map map : cohortListFromJSON) {
          CohortsResultSetsItem c = new CohortsResultSetsItem();
          c.cohortId = (String) map.get("cohortId");
          c.cohortName = (String) map.get("cohortName");
          c.cohortType = (String) map.get("cohortType");
          c.cohortDesign = mapListToOntologyTerms((List<Map>) map.get("cohortDesign"));
          c.cohortSize = (Integer) map.get("cohortSize");
          c.inclusionCriteria =
              new InclusionCriteria(
                  (String) map.get("inclusionCriteria_ageRange_start_iso8601duration"),
                  (String) map.get("inclusionCriteria_ageRange_end_iso8601duration"));
          c.locations = mapListToOntologyTerms((List<Map>) map.get("locations"));
          c.genders = mapListToOntologyTerms((List<Map>) map.get("genders"));
          c.cohortDataTypes = mapListToOntologyTerms((List<Map>) map.get("cohortDataTypes"));
          cohortList.add(c);
        }
      }
    }

    this.collections = cohortList.toArray(new CohortsResultSetsItem[cohortList.size()]);
  }
}
