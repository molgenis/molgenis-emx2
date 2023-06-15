package org.molgenis.emx2.cafevariome.request;

import static org.molgenis.emx2.cafevariome.request.parser.EAVQueryParser.getEAVQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.EAVQueryParser.hasEAV;
import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.getHPOQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.hasHPO;
import static org.molgenis.emx2.cafevariome.request.parser.ORDOQueryParser.getORDOQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.ORDOQueryParser.hasORDO;
import static org.molgenis.emx2.cafevariome.request.parser.RequiredQueryParser.getRequiredQueryFromRequest;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.query.EAVQuery;
import org.molgenis.emx2.cafevariome.request.query.HPOQuery;
import org.molgenis.emx2.cafevariome.request.query.ORDOQuery;
import org.molgenis.emx2.cafevariome.request.query.RequiredQuery;

public class QueryComponents {

  private RequiredQuery requiredQuery;
  private HPOQuery hpoQuery;
  private EAVQuery eavQuery;
  private ORDOQuery ordoQuery;

  /**
   * @param request
   * @throws Exception
   */
  public QueryComponents(Map<String, String> request) throws Exception {
    this.requiredQuery = getRequiredQueryFromRequest(request);
    if (hasHPO(request)) {
      this.hpoQuery = getHPOQueryFromRequest(request);
    }
    if (hasORDO(request)) {
      this.ordoQuery = getORDOQueryFromRequest(request);
    }
    if (hasEAV(request)) {
      this.eavQuery = getEAVQueryFromRequest(request);
    }

    System.out.println("DEBUG -- req params:");
    for (String queryParam : request.keySet()) {
      System.out.println(queryParam + " = " + request.get(queryParam));
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
