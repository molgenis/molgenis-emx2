package org.molgenis.emx2.cafevariome.request.parser;

import static org.molgenis.emx2.cafevariome.request.parameters.HPOQueryParameters.*;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.query.HPOQuery;

public class HPOQueryParser {

  /**
   * Check if request has an HPO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasHPOParams(Map<String, String> request) throws Exception {
    boolean A = request.containsKey(HPO_TERM_PAIRWISE_SIMILARITY);
    boolean B = request.containsKey(HPO_MINIMUM_MATCHED_TERMS);
    boolean C = request.containsKey(HPO_INCLUDE_ORPHA);
    boolean D = request.containsKey(HPO_SEARCH_TERMS);

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
  public static HPOQuery getHPOQueryFromRequest(Map<String, String> request) throws Exception {
    HPOQuery hpoQuery = new HPOQuery();
    hpoQuery.setTermPairwiseSimilarity(
        Double.parseDouble(request.get(HPO_TERM_PAIRWISE_SIMILARITY)));
    hpoQuery.setMinimumMatchedTerms(Integer.parseInt(request.get(HPO_MINIMUM_MATCHED_TERMS)));
    hpoQuery.setIncludeOrpha(Boolean.parseBoolean(request.get(HPO_INCLUDE_ORPHA)));
    String[] termsSplit = request.get(HPO_SEARCH_TERMS).split(",", -1);
    hpoQuery.setSearchTerms(termsSplit);
    return hpoQuery;
  }
}
