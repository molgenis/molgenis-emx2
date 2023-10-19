package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.request.parameters.HPOQueryParameters.*;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.HPOQuery;

public class JsonHPOQueryParser {

  /**
   * Check if request has an HPO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasHPOParams(JsonQuery request) throws Exception {
    int simLength = request.getQuery().getComponents().getSim().length;

    if (simLength == 0) {
      return false;
    } else if (simLength > 1) {
      throw new Exception("More than 1 HPO component query is currently unsupported");
    }

    // placeholder code for more than 1 term
    for (int i = 0; i < simLength; i++) {
      boolean A = stringProvided(request.getQuery().getComponents().getSim()[i].getR());
      boolean B = stringProvided(request.getQuery().getComponents().getSim()[i].getS());
      boolean C = stringProvided(request.getQuery().getComponents().getSim()[i].getORPHA());
      boolean D = stringsInArrayProvided(request.getQuery().getComponents().getSim()[i].getIds());

      if (A && B && C && D) {
        continue;
        // a whole term is missing, is that OK? or also error?
      } else if (!A && !B && !C && !D) {
        return false;
      } else {
        throw new Exception("Partial HPO query parameters supplied, please check your request!");
      }
    }
    return true;
  }

  /**
   * Helper to check strings
   *
   * @return
   */
  public static boolean stringProvided(String str) {
    return str != null && !str.isEmpty() && !str.isBlank();
  }

  /**
   * Helper to check string array
   *
   * @return
   */
  public static boolean stringsInArrayProvided(String[] strArr) {
    if (strArr.length == 0) {
      return false;
    }
    for (String str : strArr) {
      if (!stringProvided(str)) {
        System.out.println("array problem");
        return false;
      }
    }
    return true;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static HPOQuery getHPOQueryFromRequest(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getSim().length > 1) {
      throw new Exception("More than 1 HPO component query is currently unsupported");
    }
    double r = Double.parseDouble(request.getQuery().getComponents().getSim()[0].getR());
    int s = Integer.parseInt(request.getQuery().getComponents().getSim()[0].getS());
    boolean ORPHA = Boolean.parseBoolean(request.getQuery().getComponents().getSim()[0].getORPHA());
    String[] termsSplit = request.getQuery().getComponents().getSim()[0].getIds();
    HPOQuery hpoQuery = new HPOQuery();
    hpoQuery.setTermPairwiseSimilarity(r);
    hpoQuery.setMinimumMatchedTerms(s);
    hpoQuery.setIncludeOrpha(ORPHA);
    hpoQuery.setSearchTerms(termsSplit);
    return hpoQuery;
  }
}
