package org.molgenis.emx2.web.matomo;

import io.javalin.http.Context;

public interface TrackingService {
  void track(Context ctx);
}
