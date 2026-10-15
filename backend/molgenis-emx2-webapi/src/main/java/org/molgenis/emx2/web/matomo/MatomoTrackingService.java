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

  private MatomoTrackingService() {
    // utility class
  }

  private static final String MATOMO_URL =
      (String) EnvironmentProperty.getParameter(MATOMO_URL_ENV, "http://localhost:8081", STRING);

  private static final int MATOMO_SITE_ID =
      (Integer) EnvironmentProperty.getParameter(MATOMO_SITE_ID_ENV, 1, INT);

  private static final TrackerConfiguration CONFIGURATION =
      TrackerConfiguration.builder()
          .apiEndpoint(URI.create(MATOMO_URL + "/matomo.php"))
          .defaultSiteId(MATOMO_SITE_ID)
          .build();

  private static final MatomoTracker TRACKER = new MatomoTracker(CONFIGURATION);

  public static void track(Context ctx) {
    if (RequestClassifier.isAiChatBotRequest(ctx)) {
      TRACKER.sendRequestAsync(
          MatomoRequest.request()
              .actionUrl(ctx.fullUrl())
              .headerUserAgent(ctx.userAgent())
              // record as bot request (Matomo BotTracking), otherwise Matomo drops it as a bot
              // visit
              .botRecordingMode(2)
              .build());
    }
  }
}
