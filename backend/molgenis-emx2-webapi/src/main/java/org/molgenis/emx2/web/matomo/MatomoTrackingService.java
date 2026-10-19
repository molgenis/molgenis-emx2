package org.molgenis.emx2.web.matomo;

import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MATOMO_SITE_ID_ENV;
import static org.molgenis.emx2.Constants.MATOMO_URL_ENV;

import io.javalin.http.Context;
import java.net.URI;
import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class MatomoTrackingService {

  private static final String DEFAULT_MATOMO_URL = "http://localhost:8081/";
  private static final int DEFAULT_MATOMO_SITE_ID = 1;

  private final MatomoTracker tracker;

  public MatomoTrackingService(String matomoUrl, int siteId) {
    TrackerConfiguration configuration = buildConfig(matomoUrl, siteId);
    this.tracker = new MatomoTracker(configuration);
  }

  private static TrackerConfiguration buildConfig(String matomoUrl, int siteId) {
    return TrackerConfiguration.builder()
        .apiEndpoint(matomoUri(matomoUrl))
        .defaultSiteId(siteId)
        .build();
  }

  private static URI matomoUri(String matomoUrl) {
    return URI.create(matomoUrl.endsWith("/") ? matomoUrl : matomoUrl + "/").resolve("matomo.php");
  }

  public static MatomoTrackingService fromEnvironment() {
    String url =
        (String) EnvironmentProperty.getParameter(MATOMO_URL_ENV, DEFAULT_MATOMO_URL, STRING);
    int siteId =
        (Integer) EnvironmentProperty.getParameter(MATOMO_SITE_ID_ENV, DEFAULT_MATOMO_SITE_ID, INT);
    return new MatomoTrackingService(url, siteId);
  }

  public void track(Context ctx) {
    if (RequestClassifier.isAiChatBotRequest(ctx)) {
      tracker.sendRequestAsync(
          MatomoRequest.request()
              .actionUrl(ctx.fullUrl())
              .headerUserAgent(ctx.userAgent())
              // Only bot tracking is performed. Non-bot requests are discarded.
              .botRecordingMode(1)
              .build());
    }
  }
}
