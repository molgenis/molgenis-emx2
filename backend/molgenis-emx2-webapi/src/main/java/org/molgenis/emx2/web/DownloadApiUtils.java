package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import spark.Request;

public class DownloadApiUtils {
  static boolean includeSystemColumns(Request request) {
    return String.valueOf(request.queryParams(INCLUDE_SYSTEM_COLUMNS)).equalsIgnoreCase("true");
  }
}
