package org.molgenis.emx2.cafevariome.request.parser;

import static org.molgenis.emx2.cafevariome.request.parameters.ORDOQueryParameters.*;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.query.ORDOQuery;

public class ORDOQueryParser {

  /**
   * Check if request has an ORDO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasORDOParams(Map<String, String> request) throws Exception {
    boolean A = request.containsKey(ORDO_TERM_PAIRWISE_SIMILARITY);
    boolean B = request.containsKey(ORDO_MATCH_SCALE);
    boolean C = request.containsKey(ORDO_INCLUDE_HPO);
    boolean D = request.containsKey(ORDO_SEARCH_TERM);

    if (A && B && C && D) {
      return true;
    } else if (!A && !B && !C && !D) {
      return false;
    } else {
      throw new Exception("Partial ORDO query parameters supplied, please check your request!");
    }
  }

  /**
   * Make an ORDOQuery object from the request parameters
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static ORDOQuery getORDOQueryFromRequest(Map<String, String> request) throws Exception {
    ORDOQuery ordoQuery = new ORDOQuery();
    ordoQuery.setTermPairwiseSimilarity(
        Double.parseDouble(request.get(ORDO_TERM_PAIRWISE_SIMILARITY)));
    ordoQuery.setMatchScale(Integer.parseInt(request.get(ORDO_MATCH_SCALE)));
    ordoQuery.setIncludeHPO(Boolean.parseBoolean(request.get(ORDO_INCLUDE_HPO)));
    ordoQuery.setSearchTerm(request.get(ORDO_SEARCH_TERM));
    return ordoQuery;
  }
}
