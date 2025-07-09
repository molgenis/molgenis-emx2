package org.molgenis.emx2.web;

public class Constants {
  static final String TABLE = "table";
  public static final String CONTENT_TYPE = "Content-type";
  public static final String X_MOLGENIS_TOKEN = "x-molgenis-token";
  public static final String AUTH_KEY_TOKEN = "auth-key";
  public static final String[] MOLGENIS_TOKEN = new String[] {X_MOLGENIS_TOKEN, AUTH_KEY_TOKEN};
  public static final String LANDING_PAGE = "LANDING_PAGE";

  private Constants() {
    // hide constructor
  }

  public static final String ACCEPT_HTML =
      "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8";
  public static final String ACCEPT_JSON = "application/json";
  public static final String ACCEPT_YAML = "text/yaml";
  public static final String ACCEPT_CSV = "text/csv";
  public static final String ACCEPT_ZIP = "application/zip";
  public static final String ACCEPT_FORMDATA = "multipart/form-data";
  public static final String ACCEPT_EXCEL =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String ACCEPT_FORM_URL_ENC = "application/x-www-form-urlencoded";

  public static final String DETAIL = "detail";

  /*
  Optional api request param key to add system columns ( mg_..) to the result
  */
  public static final String INCLUDE_SYSTEM_COLUMNS = "includeSystemColumns";
}
