package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.COLUMNS;
import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import io.javalin.http.Context;
import java.util.Arrays;
import java.util.List;

public class DownloadApiUtils {

  private DownloadApiUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  static boolean includeSystemColumns(Context ctx) {
    return String.valueOf(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)).equalsIgnoreCase("true");
  }

  /**
   * Columns passed as {@code ?columns=a,b} and/or repeated {@code ?columns=a&columns=b}; empty
   * means all columns.
   */
  static List<String> requestedColumns(Context ctx) {
    return ctx.queryParams(COLUMNS).stream()
        .flatMap(value -> Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .toList();
  }
}
