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
  BiosamplesResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore String qId;

  public BiosamplesResponse(Request request, List<Table> tables) throws Exception {

    List<BiosamplesResultSets> rList = new ArrayList<>();
    qId = request.queryParams("id");

    for (Table t : tables) {
      System.out.println("## table: " + t.getName());

      Query q = t.query();
      selectColumns(t, q);

      if (qId != null) {
        q.where(f("id", EQUALS, qId));
      }

      List<BiosamplesResultSetsItem> sampleList = new ArrayList<>();

      String json = q.retrieveJSON();
      System.out.println(json);
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);
      List<Map<String, Object>> sampleListFromJSON =
          (List<Map<String, Object>>) result.get("Biosamples");

      if (sampleListFromJSON != null) {
        for (Map map : sampleListFromJSON) {
          BiosamplesResultSetsItem a = new BiosamplesResultSetsItem();
          a.id = (String) map.get("id");
          a.biosampleStatus = mapToOntologyTerm((Map) map.get("biosampleStatus"));
          a.sampleOriginType = mapListToOntologyTerms((List<Map>) map.get("sampleOriginType"));
          a.collectionMoment = (String) map.get("collectionMoment");
          a.collectionDate = (String) map.get("collectionDate");
          a.obtentionProcedure =
              new ObtentionProcedure(mapToOntologyTerm((Map) map.get("obtentionProcedure")));
          sampleList.add(a);
        }
      }

      if (sampleList.size() > 0) {
        BiosamplesResultSets aSet =
            new BiosamplesResultSets(
                t.getSchema().getName(),
                sampleList.size(),
                sampleList.toArray(new BiosamplesResultSetsItem[sampleList.size()]));
        rList.add(aSet);
      }
    }

    System.out.println("rlist size " + rList.size());

    this.resultSets = rList.toArray(new BiosamplesResultSets[rList.size()]);

    System.out.println("this.resultSets length " + this.resultSets.length);
  }
}
