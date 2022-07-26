package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private BiosamplesResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public BiosamplesResponse(Request request, List<Table> tables) throws Exception {

    List<BiosamplesResultSets> resultSetsList = new ArrayList<>();
    idForQuery = request.queryParams("id");

    for (Table table : tables) {

      Query query = selectColumns(table);

      if (idForQuery != null) {
        query.where(f("id", EQUALS, idForQuery));
      }

      List<BiosamplesResultSetsItem> biosamplesItemList = new ArrayList<>();

      String queryResultJSON = query.retrieveJSON();
      Map<String, Object> queryResult = new ObjectMapper().readValue(queryResultJSON, Map.class);
      List<Map<String, Object>> biosampleListFromJSON =
          (List<Map<String, Object>>) queryResult.get("Biosamples");

      if (biosampleListFromJSON != null) {
        for (Map map : biosampleListFromJSON) {
          BiosamplesResultSetsItem biosamplesItem = new BiosamplesResultSetsItem();
          biosamplesItem.setId((String) map.get("id"));
          biosamplesItem.setBiosampleStatus(mapToOntologyTerm((Map) map.get("biosampleStatus")));
          biosamplesItem.setSampleOriginType(mapToOntologyTerm((Map) map.get("sampleOriginType")));
          biosamplesItem.setCollectionMoment((String) map.get("collectionMoment"));
          biosamplesItem.setCollectionDate((String) map.get("collectionDate"));
          biosamplesItem.setObtentionProcedure(
              new ObtentionProcedure(mapToOntologyTerm((Map) map.get("obtentionProcedure"))));
          biosamplesItemList.add(biosamplesItem);
        }
      }
      if (biosamplesItemList.size() > 0) {
        BiosamplesResultSets biosamplesResultSets =
            new BiosamplesResultSets(
                table.getSchema().getName(),
                biosamplesItemList.size(),
                biosamplesItemList.toArray(
                    new BiosamplesResultSetsItem[biosamplesItemList.size()]));
        resultSetsList.add(biosamplesResultSets);
      }
    }
    this.resultSets = resultSetsList.toArray(new BiosamplesResultSets[resultSetsList.size()]);
  }
}
