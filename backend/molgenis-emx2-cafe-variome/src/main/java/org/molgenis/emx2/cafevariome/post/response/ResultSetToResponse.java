package org.molgenis.emx2.cafevariome.post.response;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSetsItem;

public class ResultSetToResponse {

  public static QueryResponse transform(List<Table> tables, List<IndividualsResultSets> resultSets)
      throws Exception {
    QueryResponse response = new QueryResponse();
    Map<String, QueryResult> sources = new HashMap<>();

    for (int i = 0; i < tables.size(); i++) {

      String schemaName = tables.get(i).getSchema().getName();

      QueryResult queryResult = new QueryResult();
      queryResult.setType("list");

      Source source = new Source();
      source.setUid("Source UID");
      source.setName("MOLGENIS " + schemaName);
      source.setDisplay_name("MOLGENIS EMX2 " + schemaName);
      source.setDescription("The MOLGENIS EMX2 data platform, running at xx, schema " + schemaName);
      source.setOwner_name("Todo the owner");
      source.setOwner_email("owner@molgenis.org");
      source.setUri("Todo reconstruct URI");
      source.setDate_created(Instant.now().toEpochMilli());
      source.setLocked(false);
      source.setStatus(true);
      queryResult.setSource(source);

      IndividualsResultSets resultSet = getById(schemaName, resultSets);
      if (resultSet != null) {
        Payload payload = new Payload();
        String[] subjectIds = new String[resultSet.getResults().length];
        for (int j = 0; j < resultSet.getResults().length; j++) {
          IndividualsResultSetsItem item = resultSet.getResults()[j];
          subjectIds[j] = item.getId();
        }
        payload.setSubjects(subjectIds);
        payload.setAttributes(new String[] {});
        queryResult.setCount(resultSet.getResultsCount());
        queryResult.setPayload(payload);
        sources.put(resultSet.getId(), queryResult);
        source.setRecord_count(resultSet.getResultsCount());
      } else {
        queryResult.setCount(0);
        sources.put(schemaName, queryResult);
        source.setRecord_count(0);
      }
    }

    response.setSources(sources);
    return response;
  }

  /**
   * Match schema name to result set identifier
   *
   * @param schemaName
   * @param resultSets
   * @return
   * @throws Exception
   */
  private static IndividualsResultSets getById(
      String schemaName, List<IndividualsResultSets> resultSets) throws Exception {
    for (IndividualsResultSets i : resultSets) {
      if (i.getId().equals(schemaName)) {
        return i;
      }
    }
    return null;
  }
}
