package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.rdf.QueryHelper.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.TypeUtils;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private CohortsResultSetsItem[] collections;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public CohortsResponse(Request request, List<Table> tables) throws Exception {

    idForQuery = request.queryParams("cohortId");
    List<CohortsResultSetsItem> cohortItemList = new ArrayList<>();

    for (Table table : tables) {
      Query query = selectColumns(table);

      if (idForQuery != null) {
        query.where(f("cohortId", EQUALS, idForQuery));
      }

      String queryResultJSON = query.retrieveJSON();
      Map<String, Object> queryResult = new ObjectMapper().readValue(queryResultJSON, Map.class);
      List<Map<String, Object>> cohortList = (List<Map<String, Object>>) queryResult.get("Cohorts");

      if (cohortList != null) {
        for (Map map : cohortList) {
          CohortsResultSetsItem cohortsItem = new CohortsResultSetsItem();
          cohortsItem.setCohortId(TypeUtils.toString(map.get("cohortId")));
          cohortsItem.setCohortName(TypeUtils.toString(map.get("cohortName")));
          cohortsItem.setCohortType(TypeUtils.toString(map.get("cohortType")));
          cohortsItem.setCohortDesign(mapToOntologyTerm((Map) map.get("cohortDesign")));
          cohortsItem.setCohortSize((Integer) map.get("cohortSize"));
          cohortsItem.setInclusionCriteria(
              new InclusionCriteria(
                  TypeUtils.toString(map.get("inclusionCriteria_ageRange_start_iso8601duration")),
                  TypeUtils.toString(map.get("inclusionCriteria_ageRange_end_iso8601duration"))));
          cohortsItem.setLocations(mapListToOntologyTerms((List<Map>) map.get("locations")));
          cohortsItem.setGenders(mapListToOntologyTerms((List<Map>) map.get("genders")));
          cohortsItem.setCohortDataTypes(
              mapListToOntologyTerms((List<Map>) map.get("cohortDataTypes")));
          cohortItemList.add(cohortsItem);
        }
      }
    }
    this.collections = cohortItemList.toArray(new CohortsResultSetsItem[cohortItemList.size()]);
  }
}
