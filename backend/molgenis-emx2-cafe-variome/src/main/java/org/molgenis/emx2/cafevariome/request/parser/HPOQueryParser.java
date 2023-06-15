package org.molgenis.emx2.cafevariome.request.parser;

import static org.molgenis.emx2.cafevariome.request.parameters.HPOQueryParameters.*;

import org.molgenis.emx2.cafevariome.request.query.HPOQuery;
import spark.Request;

public class HPOQueryParser {

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasHPO(Request request) throws Exception {
    boolean A = request.queryParams().contains(HPO_TERM_PAIRWISE_SIMILARITY);
    boolean B = request.queryParams().contains(HPO_MINIMUM_MATCHED_TERMS);
    boolean C = request.queryParams().contains(HPO_INCLUDE_ORPHA);
    boolean D = request.queryParams().contains(HPO_SEARCH_TERMS);

    if (A && B && C && D) {
      return true;
    } else if (!A && !B && !C && !D) {
      return false;
    } else {
      throw new Exception("Partial HPO query parameters supplied, please check your request!");
    }
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static HPOQuery getHPOQueryFromRequest(Request request) throws Exception {
    HPOQuery hpoQuery = new HPOQuery();
    hpoQuery.setTermPairwiseSimilarity(
        Integer.parseInt(request.queryParams(HPO_TERM_PAIRWISE_SIMILARITY)));
    hpoQuery.setMinimumMatchedTerms(
        Integer.parseInt(request.queryParams(HPO_MINIMUM_MATCHED_TERMS)));
    hpoQuery.setIncludeOrpha(Boolean.parseBoolean(request.queryParams(HPO_INCLUDE_ORPHA)));
    // todo parse out array from request object if possible or even raw request.body() ?
    // hpoQuery.setSearchTerms(request.queryParams(HPO_SEARCH_TERMS));
    return hpoQuery;
  }
}
