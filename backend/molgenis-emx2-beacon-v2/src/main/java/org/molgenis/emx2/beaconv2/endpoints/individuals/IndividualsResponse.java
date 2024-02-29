package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.endpoints.QueryDatatype.query;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private JsonNode resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public IndividualsResponse(Request request, List<Table> tables) throws Exception {
    String idFilter = request.queryParams("id");
    this.resultSets = query(tables, idFilter);
  }
}
