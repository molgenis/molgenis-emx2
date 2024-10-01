package org.molgenis.emx2.cafevariome.post.jsonrequest.parser;

import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringProvided;
import static org.molgenis.emx2.cafevariome.post.jsonrequest.parser.JsonHPOQueryParser.stringsInArrayProvided;

import org.molgenis.emx2.cafevariome.post.jsonrequest.JsonQuery;
import org.molgenis.emx2.cafevariome.post.request.query.ORDOQuery;

public class JsonORDOQueryParser {

  /**
   * Check if request has an ORDO query
   *
   * @param request
   * @return
   * @throws Exception
   */
  public static boolean hasORDOParams(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getOrdo() == null) {
      return false;
    }
    int ordoLength = request.getQuery().getComponents().getOrdo().length;
    if (ordoLength == 0) {
      return false;
    } else if (ordoLength > 1) {
      throw new Exception("More than 1 ORDO component query is currently unsupported");
    }

    // placeholder code for more than 1 term
    for (int i = 0; i < ordoLength; i++) {
      boolean A = stringProvided(request.getQuery().getComponents().getOrdo()[i].getR());
      boolean B = stringProvided(request.getQuery().getComponents().getOrdo()[i].getS());
      boolean C = stringProvided(request.getQuery().getComponents().getOrdo()[i].getHPO());
      boolean D = stringsInArrayProvided(request.getQuery().getComponents().getOrdo()[i].getId());

      if (A && B && C && D) {
        continue;
        // a whole term is missing, is that OK? or also error?
      } else if (!A && !B && !C && !D) {
        return false;
      } else {
        throw new Exception("Partial ORDO query parameters supplied, please check your request!");
      }
    }
    return true;
  }

  /**
   * @param request
   * @return
   * @throws Exception
   */
  public static ORDOQuery getORDOQueryFromRequest(JsonQuery request) throws Exception {
    if (request.getQuery().getComponents().getOrdo().length > 1) {
      throw new Exception("More than 1 ORDO component query is currently unsupported");
    }
    double r = Double.parseDouble(request.getQuery().getComponents().getOrdo()[0].getR());
    int s = Integer.parseInt(request.getQuery().getComponents().getOrdo()[0].getS());
    boolean HPO = Boolean.parseBoolean(request.getQuery().getComponents().getOrdo()[0].getHPO());
    String[] termsSplit = request.getQuery().getComponents().getOrdo()[0].getId();
    if (termsSplit.length > 1) {
      throw new Exception("More than 1 ID term in ORDO query is currently unsupported");
    }
    ORDOQuery ordoQuery = new ORDOQuery();
    ordoQuery.setTermPairwiseSimilarity(r);
    ordoQuery.setMatchScale(s);
    ordoQuery.setIncludeHPO(HPO);
    ordoQuery.setSearchTerm(termsSplit[0]);
    return ordoQuery;
  }
}
