package org.molgenis.emx2.beaconv2.endpoints.runs;

import static org.molgenis.emx2.beaconv2.common.QueryHelper.mapToOntologyTerm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RunsResponse {

  // annotation to print empty array as "[ ]" as required per Beacon spec
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private RunsResultSets[] resultSets;

  // query parameters, ignore from output
  @JsonIgnore private String idForQuery;

  public RunsResponse(Request request, List<Table> tables) throws Exception {

    List<RunsResultSets> resultSetsList = new ArrayList<>();
    idForQuery = request.queryParams("id");

    for (Table table : tables) {
      List<RunsResultSetsItem> runsItemList = new ArrayList<>();

      GraphQL grapql = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
      ExecutionResult executionResult =
          grapql.execute(
              "{Runs"
                  + (idForQuery != null ? "(filter:{id: {equals:\"" + idForQuery + "\"}})" : "")
                  + "{"
                  + "id,"
                  + "biosampleId,"
                  + "individualId,"
                  + "runDate,"
                  + "librarySource{name,codesystem,code},"
                  + "librarySelection,"
                  + "libraryStrategy,"
                  + "libraryLayout,"
                  + "platform,"
                  + "platformModel{name,codesystem,code}"
                  + "}}");

      Map<String, Object> result = executionResult.toSpecification();
      List<Map<String, Object>> runsListFromJSON =
          (List<Map<String, Object>>) ((HashMap<String, Object>) result.get("data")).get("Runs");

      if (runsListFromJSON != null) {
        for (Map map : runsListFromJSON) {
          RunsResultSetsItem runsItem = new RunsResultSetsItem();
          runsItem.setId((String) map.get("id"));
          runsItem.setBiosampleId((String) map.get("biosampleId"));
          runsItem.setIndividualId((String) map.get("individualId"));
          runsItem.setRunDate((String) map.get("runDate"));
          runsItem.setLibrarySource(mapToOntologyTerm((Map) map.get("librarySource")));
          runsItem.setLibrarySelection((String) map.get("librarySelection"));
          runsItem.setLibraryStrategy((String) map.get("libraryStrategy"));
          runsItem.setLibraryLayout((String) map.get("libraryLayout"));
          runsItem.setPlatform((String) map.get("platform"));
          runsItem.setPlatformModel(mapToOntologyTerm((Map) map.get("platformModel")));
          runsItemList.add(runsItem);
        }
      }

      if (runsItemList.size() > 0) {
        RunsResultSets runsResultSets =
            new RunsResultSets(
                table.getSchema().getName(),
                runsItemList.size(),
                runsItemList.toArray(new RunsResultSetsItem[runsItemList.size()]));
        resultSetsList.add(runsResultSets);
      }
    }
    this.resultSets = resultSetsList.toArray(new RunsResultSets[resultSetsList.size()]);
  }
}
