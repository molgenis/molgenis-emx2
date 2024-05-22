package org.molgenis.emx2.cafevariome.post;

import static org.molgenis.emx2.beaconv2.endpoints.individuals.QueryIndividuals.queryIndividuals;
import static org.molgenis.emx2.cafevariome.post.request.gql.Filters.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResultSets;
import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.Query;
import org.molgenis.emx2.cafevariome.post.response.QueryResponse;
import org.molgenis.emx2.cafevariome.post.response.ResultSetToResponse;
import spark.Request;

public class CafeVariomeQueryService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static QueryResponse query(Request request, List<Table> tables) throws Exception {
    JsonQuery requestBody = new ObjectMapper().readValue(request.body(), JsonQuery.class);

    // normally, we authenticate using sessions or via JWT token
    // here, we need to automate communication across CV-MOLGENIS
    // and use keycloak, so we must do a bit extra: accept keycloak tokens for authentication
    // steps to doing this:

    // implement auth via keycloak, see:
    // https://github.com/CafeVariomeUoL/CafeVariomeFlask/blob/main/cvf_app/models/node.py
    // assuming user is logged in, steps:
    // extract access token from header
    // validate against public key from a keycloak server
    // check 3 things:
    // (1) key valid, (2) expiration time, (3) azp (authorized party)

    Query query = new Query(requestBody);
    List<String> filters = new ArrayList<>();
    if (query.hasHPO()) {
      filters.addAll(makeHPOFilter(query.getHpoQuery()));
    }
    if (query.hasORDO()) {
      filters.addAll(makeORDOFilter(query.getOrdoQuery()));
    }
    if (query.hasReactome()) {
      filters.addAll(makeReactomeFilter(query.getReactomeQuery()));
    }
    if (query.hasGene()) {
      filters.addAll(makeGeneFilter(query.getGeneQuery()));
    }
    if (query.hasDemography()) {
      filters.addAll(makeDemographyFilter(query.getDemographyQuery()));
    }
    // TODO how about EAV ?

    List<IndividualsResultSets> individualsResultSets =
        queryIndividuals(tables, filters.toArray(new String[0]));

    // TODO: the HPO filter is now one big 'or' filter, but that is wrong
    // must postprocess: if 's' for HPO query was 2, select only individuals that match 2 or more
    // HPO terms
    // this counts per expanded term! e.g. 5 terms, all expanded, 2 + their expansion should match

    QueryResponse response = ResultSetToResponse.transform(tables, individualsResultSets);
    return response;
  }
}
