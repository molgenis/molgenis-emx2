package org.molgenis.emx2.cafevariome.request;

import static org.molgenis.emx2.cafevariome.request.parser.EAVQueryParser.getEAVQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.EAVQueryParser.hasEAVParams;
import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.getHPOQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.HPOQueryParser.hasHPOParams;
import static org.molgenis.emx2.cafevariome.request.parser.ORDOQueryParser.getORDOQueryFromRequest;
import static org.molgenis.emx2.cafevariome.request.parser.ORDOQueryParser.hasORDOParams;
import static org.molgenis.emx2.cafevariome.request.parser.RequiredQueryParser.getRequiredQueryFromRequest;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.query.EAVQuery;
import org.molgenis.emx2.cafevariome.request.query.HPOQuery;
import org.molgenis.emx2.cafevariome.request.query.ORDOQuery;
import org.molgenis.emx2.cafevariome.request.query.RequiredQuery;

public class Query {

  private RequiredQuery requiredQuery;
  private HPOQuery hpoQuery;
  private EAVQuery eavQuery;
  private ORDOQuery ordoQuery;

  /**
   * @param request
   * @throws Exception
   */
  public Query(Map<String, String> request) throws Exception {
    this.requiredQuery = getRequiredQueryFromRequest(request);
    if (hasHPOParams(request)) {
      this.hpoQuery = getHPOQueryFromRequest(request);
    }
    if (hasORDOParams(request)) {
      this.ordoQuery = getORDOQueryFromRequest(request);
    }
    if (hasEAVParams(request)) {
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

  public boolean hasHPO() {
    return hpoQuery != null;
  }

  public boolean hasORDO() {
    return ordoQuery != null;
  }

  public boolean hasEAV() {
    return eavQuery != null;
  }

  public RequiredQuery getRequiredQuery() {
    return requiredQuery;
  }

  public void setRequiredQuery(RequiredQuery requiredQuery) {
    this.requiredQuery = requiredQuery;
  }

  public HPOQuery getHpoQuery() {
    return hpoQuery;
  }

  public void setHpoQuery(HPOQuery hpoQuery) {
    this.hpoQuery = hpoQuery;
  }

  public EAVQuery getEavQuery() {
    return eavQuery;
  }

  public void setEavQuery(EAVQuery eavQuery) {
    this.eavQuery = eavQuery;
  }

  public ORDOQuery getOrdoQuery() {
    return ordoQuery;
  }

  public void setOrdoQuery(ORDOQuery ordoQuery) {
    this.ordoQuery = ordoQuery;
  }
}
