package org.molgenis.emx2.cafevariome;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;

import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.cafevariome.request.Query;
import org.molgenis.emx2.cafevariome.response.DummyResponse;
import spark.Request;

public class CafeVariomeService {

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static String query(Request request, List<Table> tables) throws Exception {
    Query query = new Query(request);
    List<IndividualsResultSets> individualsResultSets = queryIndividuals(tables, "filters");
    // todo convert Query into gql and execute EMX2 database queries
    return DummyResponse.dummyResponse;
  }
}
