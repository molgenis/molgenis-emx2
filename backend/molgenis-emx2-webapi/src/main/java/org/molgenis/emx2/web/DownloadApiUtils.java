package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import io.javalin.http.Context;

public final class DownloadApiUtils {

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return "true".equalsIgnoreCase(String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)));
  }
}
