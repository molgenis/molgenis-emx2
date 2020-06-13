package org.molgenis.emx2.web;

public class Constants {
  static final String INPUT = "input";
  static final String TABLE = "table";
  public static final String ANONYMOUS = "anonymous";
  public static final String CONTENT_TYPE = "Content-type";

  private Constants() {
    // hide constructor
  }

  public static final String ACCEPT_HTML =
      "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8";
  public static final String ACCEPT_JSON = "application/json";
  public static final String ACCEPT_CSV = "text/csv";
  public static final String ACCEPT_ZIP = "application/zip";
  public static final String ACCEPT_FORMDATA = "multipart/form-data";
  public static final String ACCEPT_EXCEL =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  public static final String NAME = "name";
  public static final String FILTER = "filter";
  public static final String TABLES = "tables";
  public static final String MEMBERS = "members";
  public static final String SETTINGS = "settings";
  public static final String COLUMNS = "columns";
  public static final String TYPE = "type";
  public static final String VALUE = "value";

  public static final String DETAIL = "detail";
  static final String COUNT = "count";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String SEARCH = "search";
  public static final String ORDERBY = "orderby";
}
