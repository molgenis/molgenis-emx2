package org.molgenis.emx2.beaconv2.endpoints.analyses;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.beaconv2.endpoints.QueryHelper.selectColumns;

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
  private AnalysesResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public AnalysesResponse(Request request, List<Table> tables) throws Exception {

    List<AnalysesResultSets> resultSetsList = new ArrayList<>();
    idForQuery = request.queryParams("id");

    for (Table table : tables) {
      Query query = selectColumns(table);

      if (idForQuery != null) {
        query.where(f("id", EQUALS, idForQuery));
      }

      List<AnalysesResultSetsItem> analysesItemList = new ArrayList<>();
      for (Row row : query.retrieveRows()) {
        AnalysesResultSetsItem analysesItem = new AnalysesResultSetsItem();
        analysesItem.setId(row.getString("id"));
        analysesItem.setRunId(row.getString("runId"));
        analysesItem.setBiosampleId(row.getString("biosampleId"));
        analysesItem.setIndividualId(row.getString("individualId"));
        analysesItem.setAnalysisDate(row.getString("analysisDate"));
        analysesItem.setPipelineName(row.getString("pipelineName"));
        analysesItem.setPipelineRef(row.getString("pipelineRef"));
        analysesItem.setAligner(row.getString("aligner"));
        analysesItem.setVariantCaller(row.getString("variantCaller"));
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
