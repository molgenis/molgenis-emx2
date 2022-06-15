package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.common.QueryHelper.selectColumns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.analyses.AnalysesResultSets;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  CohortsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore String qId;

  public CohortsResponse(Request request, List<Table> tables) throws Exception {

    List<AnalysesResultSets> rList = new ArrayList<>();
    qId = request.queryParams("id");

    for (Table t : tables) {
      Query q = t.query();
      selectColumns(t, q);

      if (qId != null) {
        q.where(f("id", EQUALS, qId));
      }
    }
  }
}
