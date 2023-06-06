package org.molgenis.emx2.cafevariome.request;

import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.getHPOQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.hasHPO;
import static org.molgenis.emx2.cafevariome.request.parser.RequiredQueryParser.getRequiredQueryFromRequest;

import org.molgenis.emx2.cafevariome.request.query.EAVQuery;
import org.molgenis.emx2.cafevariome.request.query.HPOQuery;
import org.molgenis.emx2.cafevariome.request.query.ORDOQuery;
import org.molgenis.emx2.cafevariome.request.query.RequiredQuery;
import spark.Request;

public class QueryComponents {

  private RequiredQuery requiredQuery;
  private HPOQuery hpoQuery;
  private EAVQuery eavQuery;
  private ORDOQuery ordoQuery;

  /**
   *
   * @param request
   * @throws Exception
   */
  public QueryComponents(Request request) throws Exception {
    this.requiredQuery = getRequiredQueryFromRequest(request);
    if (hasHPO(request)) {
      this.hpoQuery = getHPOQueryFromRequest(request);
    }

    System.out.println("DEBUG -- req body:");
    System.out.println(request.body());
    System.out.println("--");
    System.out.println("DEBUG -- req params:");
    for (String queryParam : request.queryParams()) {
      System.out.println(queryParam + " = " + request.queryParams(queryParam));
    }
    System.out.println("--");

  }

  @Override
  public String toString() {
    return "QueryComponents{"
        + "requiredQuery="
        + requiredQuery
        + ", hpoQuery="
        + hpoQuery
        + ", eavQuery="
        + eavQuery
        + ", ordoQuery="
        + ordoQuery
        + '}';
  }
}
