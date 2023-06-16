package org.molgenis.emx2.cafevariome;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;
import static org.molgenis.emx2.cafevariome.request.gql.Filters.makeHPOFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.cafevariome.request.Query;
import org.molgenis.emx2.cafevariome.request.parser.RequestBodyParser;
import org.molgenis.emx2.cafevariome.response.Response;
import org.molgenis.emx2.cafevariome.response.ResultSetToResponse;
import spark.Request;

public class CafeVariomeService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static Response query(Request request, List<Table> tables) throws Exception {
    Map<String, String> requestMap = RequestBodyParser.parse(request.body());
    Query query = new Query(requestMap);

    List<String> filters = new ArrayList<>();
    if (query.hasHPO()) {
      filters.addAll(makeHPOFilter(query.getHpoQuery()));
    }

    List<IndividualsResultSets> individualsResultSets =
        queryIndividuals(tables, filters.toArray(new String[0]));

    // fixme: a way to wrap a parent array?
    // ArrayNode parentArray = jsonMapper.createArrayNode();
    // CafeVariomeJSONResponse response =
    // ResultSetToCafeVariomeJSON.transform(individualsResultSets);
    // parentArray.add(String.valueOf(response));

    Response response = ResultSetToResponse.transform(individualsResultSets);
    return response;
    // return DummyResponse.dummyResponse;

  }
}
