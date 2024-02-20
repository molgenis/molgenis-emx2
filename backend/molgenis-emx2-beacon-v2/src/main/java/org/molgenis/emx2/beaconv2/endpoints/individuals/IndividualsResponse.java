package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private IndividualsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public IndividualsResponse(Request request, List<Table> tables) throws Exception {
    String idFilter = request.queryParams("id");
    List<IndividualsResultSets> resultSetsList = queryIndividuals(tables, idFilter);
    this.resultSets = resultSetsList.toArray(new IndividualsResultSets[resultSetsList.size()]);
  }
}
