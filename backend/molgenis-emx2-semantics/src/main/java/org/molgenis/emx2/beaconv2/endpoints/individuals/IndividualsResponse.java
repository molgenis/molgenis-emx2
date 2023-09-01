package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private List<GenericResultSet> resultSets = new ArrayList<>();

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  private ObjectMapper mapper = new ObjectMapper();

  public IndividualsResponse(Request request, List<Table> tables) throws Exception {
    idForQuery = request.queryParams("id");
    for (Table t : tables) {
      Schema s = t.getSchema();
      String results = s.retrieveSql(sql).get(0).getString("results");
      resultSets.add(new GenericResultSet(idForQuery, mapper.readValue(results, List.class)));
    }

    //    idForQuery = request.queryParams("id");
    //    String idFilter = (idForQuery != null ? "{id: {equals:\"" + idForQuery + "\"}}" : "");
    //    List<IndividualsResultSets> resultSetsList = queryIndividuals(tables, idFilter);
    //    this.resultSets = resultSetsList.toArray(new
    // IndividualsResultSets[resultSetsList.size()]);
  }

  private static String sql =
      """
SELECT
           json_agg(json_build_object(
                    'id', t.id,
                    'sex', (select json_build_object('id', s.name, 'label', s.codesystem || ':' || s.code)
                            from (select * from "GenderAtBirth" s where name = t.sex) as s),
                    'diseaseCausalGenes',
                    (select json_agg(json_build_object('id', s.name, 'label', s.codesystem || ':' || s.code))
                     from (select * from "Genes" s where s.name = ANY (t."diseaseCausalGenes")) as s)
                )) as results

FROM (select * from "Individuals") as t
""";
}
