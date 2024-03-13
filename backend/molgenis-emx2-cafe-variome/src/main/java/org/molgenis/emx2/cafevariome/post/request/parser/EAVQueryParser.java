package org.molgenis.emx2.cafevariome.post.request.parser;

import static org.molgenis.emx2.cafevariome.post.request.parameters.EAVQueryParameters.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.cafevariome.post.request.query.EAVQuery;
import org.molgenis.emx2.cafevariome.post.request.query.EAVTriple;

public class EAVQueryParser {

  public static final int MAX_EAV_QUERIES = 1000;

  /**
   * Check if request has an EAV query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasEAVParams(Map<String, String> request) throws Exception {
    boolean A = request.containsKey(EAV_ATTRIBUTE(0));
    boolean B = request.containsKey(EAV_OPERATOR(0));
    boolean C = request.containsKey(EAV_VALUE(0));

    if (A && B && C) {
      return true;
    } else if (!A && !B && !C) {
      return false;
    } else {
      throw new Exception(
          "Partial EAV parameters for query[0] supplied, please check your request!");
    }
  }

  /**
   * Retrieve EAV triples from request until a specified maximum of queries
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static EAVQuery getEAVQueryFromRequest(Map<String, String> request) throws Exception {
    EAVQuery eavQuery = new EAVQuery();
    List<EAVTriple> eavTriples = new ArrayList<>();
    for (int i = 0; i <= MAX_EAV_QUERIES; i++) {
      if (i == MAX_EAV_QUERIES) {
        throw new Exception(
            "No more than " + MAX_EAV_QUERIES + " EAV queries can be combined in one request");
      }
      if (request.containsKey(EAV_ATTRIBUTE(i))) {
        String attribute = request.get(EAV_ATTRIBUTE(i));
        String operator = request.get(EAV_OPERATOR(i));
        String value = request.get(EAV_VALUE(i));
        EAVTriple eavTriple = new EAVTriple(attribute, operator, value);
        eavTriples.add(eavTriple);
      }
    }
    eavQuery.setEavTriples(eavTriples);
    return eavQuery;
  }
}
