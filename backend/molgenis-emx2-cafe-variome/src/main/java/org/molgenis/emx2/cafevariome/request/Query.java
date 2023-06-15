package org.molgenis.emx2.cafevariome.request;

import spark.Request;

public class Query {

  private Request request;

  /**
   * @param request
   * @throws Exception
   */
  public Query(Request request) throws Exception {
    this.request = request;
    QueryComponents queryComponents = new QueryComponents(request);
    System.out.println(queryComponents);
  }
}
