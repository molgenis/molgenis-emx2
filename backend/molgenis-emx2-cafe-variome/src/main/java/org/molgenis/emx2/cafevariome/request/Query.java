package org.molgenis.emx2.cafevariome.request;

import java.util.Map;
import org.molgenis.emx2.cafevariome.request.parser.RequestBodyParser;
import spark.Request;

public class Query {

  private Request request;

  /**
   * @param request
   * @throws Exception
   */
  public Query(Request request) throws Exception {
    this.request = request;
    Map requestMap = RequestBodyParser.parse(request.body());
    QueryComponents queryComponents = new QueryComponents(requestMap);
    System.out.println(queryComponents);
  }
}
