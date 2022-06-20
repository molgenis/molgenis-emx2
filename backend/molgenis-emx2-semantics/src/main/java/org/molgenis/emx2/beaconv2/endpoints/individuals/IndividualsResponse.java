package org.molgenis.emx2.beaconv2.endpoints.individuals;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.*;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  IndividualsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore String idForQuery;

  public IndividualsResponse(Request request, List<Table> tables) throws Exception {

    List<IndividualsResultSets> resultSetsList = new ArrayList<>();
    idForQuery = request.queryParams("id");

    for (Table table : tables) {
      List<IndividualsResultSetsItem> individualsItemList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Individuals"
                  + (idForQuery != null ? "(filter:{id: {equals:\"" + idForQuery + "\"}})" : "")
                  + "{"
                  + "id,"
                  + "sex{name,codesystem,code},"
                  + "ethnicity{name,codesystem,code},"
                  + "geographicOrigin{name,codesystem,code},"
                  + "diseases{"
                  + "   diseaseCode{name,codesystem,code},"
                  + "   ageOfOnset__ageGroup{name,codesystem,code},"
                  + "   familyHistory,"
                  + "   severity{name,codesystem,code},"
                  + "   stage{name,codesystem,code}},"
                  + "measures{"
                  + "   assayCode{name,codesystem,code},"
                  + "   date,"
                  + "   measurementVariable,"
                  + "   measurementValue__value,"
                  + "   measurementValue__units{name,codesystem,code},"
                  + "   observationMoment__age__iso8601duration"
                  + "}}}");

      Map<String, Object> result = executionResult.toSpecification();

      List<Map<String, Object>> individualsListFromJSON =
          (List<Map<String, Object>>)
              ((HashMap<String, Object>) result.get("data")).get("Individuals");

      if (individualsListFromJSON != null) {
        for (Map map : individualsListFromJSON) {
          IndividualsResultSetsItem individualsItem = new IndividualsResultSetsItem();
          individualsItem.id = (String) map.get("id");
          individualsItem.sex = mapToOntologyTerm((Map) map.get("sex"));
          individualsItem.ethnicity = mapToOntologyTerm((Map) map.get("ethnicity"));
          individualsItem.geographicOrigin = mapToOntologyTerm((Map) map.get("geographicOrigin"));
          individualsItem.diseases = Diseases.get(map.get("diseases"));
          individualsItem.measures = Measures.get(map.get("measures"));
          individualsItemList.add(individualsItem);
        }
      }

      if (individualsItemList.size() > 0) {
        IndividualsResultSets aSet =
            new IndividualsResultSets(
                table.getSchema().getName(),
                individualsItemList.size(),
                individualsItemList.toArray(
                    new IndividualsResultSetsItem[individualsItemList.size()]));
        resultSetsList.add(aSet);
      }
    }
    this.resultSets = resultSetsList.toArray(new IndividualsResultSets[resultSetsList.size()]);
  }
}
