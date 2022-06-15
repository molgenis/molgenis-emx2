package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.selectColumns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;
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

      HashMap<String, BiosamplesResultSetsItem> aList = new HashMap<>();

      // todo better way to deal with duplicated rows when 1 row has multiple values in a column...
      // tmp store for 0..n variables
      HashMap<String, List<OntologyTerm>> sampleOriginType = new HashMap<>();

      for (Row r : q.retrieveRows()) {
        String id = r.getString("id");
        BiosamplesResultSetsItem a;
        if (!aList.containsKey(id)) {
          a = new BiosamplesResultSetsItem();
          // 0..1 variables, ok to put once
          a.id = id;
          a.collectionMoment = r.getString("collectionMoment");
          a.collectionDate = r.getString("collectionDate");
          a.biosampleStatus =
              new OntologyTerm(
                  r.getString("biosampleStatus-codesystem") + r.getString("biosampleStatus-code"),
                  r.getString("biosampleStatus-name"));
          a.obtentionProcedure =
              new ObtentionProcedure(
                  r.getString("obtentionProcedure-codesystem")
                      + r.getString("obtentionProcedure-code"),
                  r.getString("obtentionProcedure-name"));

          // tmp store for 0..n variables
          List<OntologyTerm> sampleOriginTypeList = new ArrayList<>();
          sampleOriginTypeList.add(
              new OntologyTerm(
                  r.getString("sampleOriginType-codesystem") + r.getString("sampleOriginType-code"),
                  r.getString("sampleOriginType-name")));
          sampleOriginType.put(id, sampleOriginTypeList);
          aList.put(id, a);
        } else {
          // tmp store for 0..n variables
          sampleOriginType
              .get(id)
              .add(
                  new OntologyTerm(
                      r.getString("sampleOriginType-codesystem")
                          + r.getString("sampleOriginType-code"),
                      r.getString("sampleOriginType-name")));
        }
      }

      for (String id : aList.keySet()) {
        BiosamplesResultSetsItem a = aList.get(id);
        a.sampleOriginType =
            sampleOriginType.get(id).toArray(new OntologyTerm[sampleOriginType.get(id).size()]);
      }

      BiosamplesResultSets aSet =
          new BiosamplesResultSets(
              t.getSchema().getName(),
              aList.size(),
              aList.values().toArray(new BiosamplesResultSetsItem[aList.size()]));
      rList.add(aSet);
    }

    this.resultSets = rList.toArray(new BiosamplesResultSets[rList.size()]);
  }
}
