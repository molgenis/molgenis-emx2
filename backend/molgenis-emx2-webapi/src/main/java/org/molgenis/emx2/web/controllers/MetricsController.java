package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.ColumnType.STRING;

import io.javalin.http.Context;
import io.prometheus.metrics.exporter.servlet.jakarta.PrometheusMetricsServlet;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class MetricsController {

  public static final boolean METRICS_ENABLED =
      (Boolean)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED, false, ColumnType.BOOL);
  public static final String METRICS_PATH =
      (String)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_METRICS_PATH, "api/metrics", STRING);

  private final PrometheusMetricsServlet metricsServlet;

  public MetricsController() {
    metricsServlet = new PrometheusMetricsServlet(PrometheusRegistry.defaultRegistry);
  }

  public void handleRequest(Context ctx) throws ServletException, IOException {
    metricsServlet.service(ctx.req(), ctx.res());
  }
}
