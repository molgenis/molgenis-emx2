package org.molgenis.emx2.cafevariome.response;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSetsItem;

public class ResultSetToResponse {

  public static CVResponse transform(List<IndividualsResultSets> resultSets) {
    CVResponse response = new CVResponse();
    Map<String, QueryResult> sources = new HashMap<>();
    for (int i = 0; i < resultSets.size(); i++) {

      QueryResult queryResult = new QueryResult();
      queryResult.setCount(resultSets.get(i).getResultsCount());
      queryResult.setType("list");

      Payload payload = new Payload();
      String[] subjectIds = new String[resultSets.get(i).getResults().length];
      for (int j = 0; j < resultSets.get(i).getResults().length; j++) {
        IndividualsResultSetsItem item = resultSets.get(i).getResults()[j];
        subjectIds[j] = item.getId();
      }
      payload.setSubjects(subjectIds);
      payload.setAttributes(new String[] {});
      queryResult.setPayload(payload);

      Source source = new Source();
      source.setUid("Source UID");
      source.setName("MOLGENIS");
      source.setDisplay_name("MOLGENIS EMX2");
      source.setDescription("The MOLGENIS EMX2 data platform");
      source.setOwner_name("Todo the owner");
      source.setOwner_email("owner@molgenis.org");
      source.setUri("Todo reconstruct URI");
      source.setDate_created(Instant.now().toEpochMilli());
      source.setRecord_count(999);
      source.setLocked(false);
      source.setStatus(true);
      queryResult.setSource(source);

      sources.put(resultSets.get(i).getId(), queryResult);
    }
    response.setSources(sources);
    return response;
  }
}
