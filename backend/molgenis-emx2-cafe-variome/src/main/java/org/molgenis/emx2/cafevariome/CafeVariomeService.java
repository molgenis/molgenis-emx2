package org.molgenis.emx2.cafevariome;

import org.molgenis.emx2.cafevariome.request.Query;
import org.molgenis.emx2.cafevariome.response.DummyResponse;
import spark.Request;

public class CafeVariomeService {

  /**
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static String query(Request request) throws Exception {
    Query query = new Query(request);
    // todo convert Query into gql and execute EMX2 database queries
    return DummyResponse.dummyResponse;
  }
}
