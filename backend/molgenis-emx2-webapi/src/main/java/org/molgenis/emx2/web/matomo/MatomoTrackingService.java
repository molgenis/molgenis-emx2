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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatomoTrackingService implements TrackingService {

  private static final Logger logger = LoggerFactory.getLogger(MatomoTrackingService.class);

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

  public static TrackingService fromEnvironment() {
    String url = (String) EnvironmentProperty.getParameter(MATOMO_URL_ENV, null, STRING);
    int siteId =
        (Integer) EnvironmentProperty.getParameter(MATOMO_SITE_ID_ENV, DEFAULT_MATOMO_SITE_ID, INT);
    return new MatomoTrackingService(url, siteId);
  }

  @Override
  public void track(Context ctx) {
    try {
      if (RequestClassifier.isAiChatBotRequest(ctx)) {
        tracker.sendRequestAsync(
            MatomoRequest.request()
                .actionUrl(ctx.fullUrl())
                .headerUserAgent(ctx.userAgent())
                // Only bot tracking is performed. Non-bot requests are discarded.
                .botRecordingMode(1)
                .build());
      }
    } catch (Exception e) {
      logger.warn("Unable to send tracking request tot Matomo", e);
    }
  }
}
