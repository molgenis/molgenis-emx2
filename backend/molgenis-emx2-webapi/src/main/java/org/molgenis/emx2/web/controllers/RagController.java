package org.molgenis.emx2.web.controllers;

import io.javalin.http.Context;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.molgenis.emx2.rag.QueryResult;
import org.molgenis.emx2.rag.RagService;

public class RagController {

  private final RagService ragService;

  public RagController() {
    ragService = new RagService();
  }

  public void handleRequest(Context ctx) throws ServletException, IOException {
    String search = ctx.queryParam("search");

    if (search != null) {
      List<QueryResult> results = ragService.query(search);
      ctx.status(200);
      ctx.json(results);
    } else {
      ctx.status(404);
    }
  }
}
