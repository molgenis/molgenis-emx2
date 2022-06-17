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
  @JsonIgnore String qId;

  public IndividualsResponse(Request request, List<Table> tables) throws Exception {

    // TODO id query, and others!
    List<IndividualsResultSets> rList = new ArrayList<>();
    qId = request.queryParams("id");

    for (Table t : tables) {
      List<IndividualsResultSetsItem> indList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(t.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Individuals{"
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
          IndividualsResultSetsItem i = new IndividualsResultSetsItem();
          i.id = (String) map.get("id");
          i.sex = mapToOntologyTerm((Map) map.get("sex"));
          i.ethnicity = mapToOntologyTerm((Map) map.get("ethnicity"));
          i.geographicOrigin = mapToOntologyTerm((Map) map.get("geographicOrigin"));
          i.diseases = Diseases.get(map.get("diseases"));
          i.measures = Measures.get(map.get("measures"));
          indList.add(i);
        }
      }

      if (indList.size() > 0) {
        IndividualsResultSets aSet =
            new IndividualsResultSets(
                t.getSchema().getName(),
                indList.size(),
                indList.toArray(new IndividualsResultSetsItem[indList.size()]));
        rList.add(aSet);
      }
    }
    this.resultSets = rList.toArray(new IndividualsResultSets[rList.size()]);
  }
}
