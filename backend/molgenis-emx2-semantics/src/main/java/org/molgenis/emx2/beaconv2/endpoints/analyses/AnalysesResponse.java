package org.molgenis.emx2.beaconv2.endpoints.analyses;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.selectColumns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnalysesResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  AnalysesResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore String qId;

  public AnalysesResponse(Request request, List<Table> tables) throws Exception {

    List<AnalysesResultSets> rList = new ArrayList<>();
    qId = request.queryParams("id");

    for (Table t : tables) {
      Query q = t.query();
      selectColumns(t, q);

      if (qId != null) {
        q.where(f("id", EQUALS, qId));
      }

      /*
      todo
      could replace with reflection, but wouldn't work for the other more complex endpoints anyway
      e.g.
                Field field = a.getClass().getDeclaredField(colName);
                if (r.containsName(colName)) {
                field.set(a, r.getString(colName));
       */
      List<AnalysesResultSetsItem> aList = new ArrayList<>();
      for (Row r : q.retrieveRows()) {
        AnalysesResultSetsItem a = new AnalysesResultSetsItem();
        a.id = r.getString("id");
        a.runId = r.getString("runId");
        a.biosampleId = r.getString("biosampleId");
        a.individualId = r.getString("individualId");
        a.analysisDate = r.getString("analysisDate");
        a.pipelineName = r.getString("pipelineName");
        a.pipelineRef = r.getString("pipelineRef");
        a.aligner = r.getString("aligner");
        a.variantCaller = r.getString("variantCaller");
        aList.add(a);
      }
      if (aList.size() > 0) {
        AnalysesResultSets aSet =
            new AnalysesResultSets(
                t.getSchema().getName(),
                aList.size(),
                aList.toArray(new AnalysesResultSetsItem[aList.size()]));
        rList.add(aSet);
      }
    }

    this.resultSets = rList.toArray(new AnalysesResultSets[rList.size()]);
  }
}
