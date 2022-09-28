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
import org.molgenis.emx2.utils.TypeUtils;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private IndividualsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

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
                  + "   ageOfOnset__age__iso8601duration,"
                  + "   ageAtDiagnosis__ageGroup{name,codesystem,code},"
                  + "   ageAtDiagnosis__age__iso8601duration,"
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
          individualsItem.setId(TypeUtils.toString(map.get("id")));
          individualsItem.setSex(mapToOntologyTerm((Map) map.get("sex")));
          individualsItem.setEthnicity(mapToOntologyTerm((Map) map.get("ethnicity")));
          individualsItem.setGeographicOrigin(mapToOntologyTerm((Map) map.get("geographicOrigin")));
          individualsItem.setDiseases(Diseases.get(map.get("diseases")));
          individualsItem.setMeasures(Measures.get(map.get("measures")));
          individualsItemList.add(individualsItem);
        }
      }

      if (individualsItemList.size() > 0) {
        IndividualsResultSets individualsResultSets =
            new IndividualsResultSets(
                table.getSchema().getName(),
                individualsItemList.size(),
                individualsItemList.toArray(
                    new IndividualsResultSetsItem[individualsItemList.size()]));
        resultSetsList.add(individualsResultSets);
      }
    }
    this.resultSets = resultSetsList.toArray(new IndividualsResultSets[resultSetsList.size()]);
  }
}
