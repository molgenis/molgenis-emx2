package org.molgenis.emx2.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.web.service.PodiumService;

public class PodiumApi {
  private static PodiumService podiumService = null;

  private PodiumApi() {}

  public static void create(Javalin app) {
    final String reportPath = "/api/podium";
    app.post(reportPath, PodiumApi::handlePodiumRequest);
    podiumService = new PodiumService(HttpClient.newHttpClient());
  }

  private static void handlePodiumRequest(@NotNull Context context)
      throws IOException, InterruptedException {
    HttpResponse<String> response = podiumService.getResponse(context);
    podiumService.updateContext(context, response);
    context.result(response.body());
  }
}
