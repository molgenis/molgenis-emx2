package org.molgenis.emx2.beaconv2.responses.biosamples;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.common.Ontology;
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
      Query q = t.query();
      for (Column c : t.getMetadata().getColumns()) {
        switch (c.getName()) {
          case "id":
          case "biosampleStatus_id":
          case "biosampleStatus_label":
          case "sampleOriginType_id":
          case "sampleOriginType_label":
          case "collectionMoment":
          case "collectionDate":
          case "obtentionProcedure_procedureCode_id":
          case "obtentionProcedure_procedureCode_label":
            q.select(s(c.getName()));
        }
      }

      if (qId != null) {
        q.where(f("id", EQUALS, qId));
      }

      List<BiosamplesResultSetsItem> aList = new ArrayList<>();
      for (Row r : q.retrieveRows()) {
        BiosamplesResultSetsItem a = new BiosamplesResultSetsItem();
        a.id = r.getString("id");
        a.biosampleStatus =
            new Ontology(r.getString("biosampleStatus_id"), r.getString("biosampleStatus_label"));
        a.sampleOriginType =
            new Ontology(r.getString("sampleOriginType_id"), r.getString("sampleOriginType_label"));
        a.collectionMoment = r.getString("collectionMoment");
        a.collectionDate = r.getString("collectionDate");
        a.obtentionProcedure =
            new ObtentionProcedure(
                r.getString("obtentionProcedure_procedureCode_id"),
                r.getString("obtentionProcedure_procedureCode_label"));
        aList.add(a);
      }
      BiosamplesResultSets aSet =
          new BiosamplesResultSets(
              t.getSchema().getName(),
              aList.size(),
              aList.toArray(new BiosamplesResultSetsItem[aList.size()]));
      rList.add(aSet);
    }

    this.resultSets = rList.toArray(new BiosamplesResultSets[rList.size()]);
  }
}
