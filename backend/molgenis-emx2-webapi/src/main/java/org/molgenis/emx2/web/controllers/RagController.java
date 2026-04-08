package org.molgenis.emx2.web.controllers;

import io.javalin.http.Context;
import java.util.List;
import org.molgenis.emx2.rag.QueryResult;
import org.molgenis.emx2.rag.RagService;

public class RagController {
  private RagService ragService;

  public RagController() {
    ragService = new RagService();
  }

  public void handleRequest(Context ctx) {
    String query = ctx.queryParam("search");
    List<QueryResult> results = ragService.query(query);
    ctx.status(200);
    ctx.json(results);
  }
}
