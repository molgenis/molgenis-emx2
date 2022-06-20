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
  @JsonIgnore String idForQuery;

  public AnalysesResponse(Request request, List<Table> tables) throws Exception {

    List<AnalysesResultSets> resultSetsList = new ArrayList<>();
    idForQuery = request.queryParams("id");

    for (Table table : tables) {
      Query query = table.query();
      selectColumns(table, query);

      if (idForQuery != null) {
        query.where(f("id", EQUALS, idForQuery));
      }

      List<AnalysesResultSetsItem> analysesItemList = new ArrayList<>();
      for (Row row : query.retrieveRows()) {
        AnalysesResultSetsItem analysesItem = new AnalysesResultSetsItem();
        analysesItem.id = row.getString("id");
        analysesItem.runId = row.getString("runId");
        analysesItem.biosampleId = row.getString("biosampleId");
        analysesItem.individualId = row.getString("individualId");
        analysesItem.analysisDate = row.getString("analysisDate");
        analysesItem.pipelineName = row.getString("pipelineName");
        analysesItem.pipelineRef = row.getString("pipelineRef");
        analysesItem.aligner = row.getString("aligner");
        analysesItem.variantCaller = row.getString("variantCaller");
        analysesItemList.add(analysesItem);
      }
      if (analysesItemList.size() > 0) {
        AnalysesResultSets analysesResultSets =
            new AnalysesResultSets(
                table.getSchema().getName(),
                analysesItemList.size(),
                analysesItemList.toArray(new AnalysesResultSetsItem[analysesItemList.size()]));
        resultSetsList.add(analysesResultSets);
      }
    }
    this.resultSets = resultSetsList.toArray(new AnalysesResultSets[resultSetsList.size()]);
  }
}
