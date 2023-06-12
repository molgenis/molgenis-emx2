package org.molgenis.emx2.beaconv2.endpoints.datasets;

import static org.molgenis.emx2.beaconv2.endpoints.datasets.Queries.queryDatasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private DatasetsResultSets[] resultSets; // Collection instead?

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public DatasetsResponse(Request request, List<Table> tables) {
    idForQuery = request.queryParams("id");
    String idFilter = (idForQuery != null ? "{id: {equals:\"" + idForQuery + "\"}}" : "");
    List<DatasetsResultSets> resultSetsList = queryDatasets(tables, idFilter);
    this.resultSets = resultSetsList.toArray(new DatasetsResultSets[resultSetsList.size()]);
  }
}
