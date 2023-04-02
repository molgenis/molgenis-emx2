package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {

  public static final String MG_EDIT_ROLE = "MG_EDIT_ROLE_";
  public static final String MG_ROLE_PREFIX = "MG_ROLE_";
  public static final String MG_USER_PREFIX = "MG_USER_";

  public static final String COMPOSITE_REF_SEPARATOR = ".";
  public static final String REF_SCHEMA_NAME = "refSchema";
  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_LINK = "refLink";
  public static final String REF_LABEL = "refLabel";
  public static final String REF_LABEL_DEFAULT = "refLabelDefault";
  public static final String REF_BACK = "refBack";
  public static final String CASCADE_DELETE = "cascadeDelete";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String FORM = "form";
  public static final String READONLY = "readonly";
  public static final String COMPUTED = "computed";
  public static final String VISIBLE_EXPRESSION = "visible";
  public static final String DESCRIPTION = "description";
  public static final String IS_CHANGELOG_ENABLED = "isChangelogEnabled";
  public static final String TEMPLATE = "template";
  public static final String INCLUDE_DEMO_DATA = "includeDemoData";
  public static final String SEMANTICS = "semantics";
  public static final String ROLE = "role";
  public static final String KEY = "key";
  public static final String INHERITED = "inherited";

  public static final String MG_TABLECLASS = "mg_tableclass";
  public static final String MG_DRAFT = "mg_draft";
  public static final String MG_INSERTEDBY = "mg_insertedBy";
  public static final String MG_INSERTEDON = "mg_insertedOn";
  public static final String MG_UPDATEDBY = "mg_updatedBy";
  public static final String MG_UPDATEDON = "mg_updatedOn";

  public static final String TEXT_SEARCH_COLUMN_NAME = "_TEXT_SEARCH_COLUMN";
  public static final String SETTINGS_TABLE = "molgenis_settings";
  public static final String SETTINGS = "settings";
  public static final String SETTINGS_NAME = "key";
  public static final String SETTINGS_VALUE = "value";
  public static final String SETTINGS_USER = "user";

  public static final String MOLGENIS_POSTGRES_URI = "MOLGENIS_POSTGRES_URI";
  public static final String MOLGENIS_POSTGRES_USER = "MOLGENIS_POSTGRES_USER";
  public static final String MOLGENIS_POSTGRES_PASS = "MOLGENIS_POSTGRES_PASS";
  public static final String MOLGENIS_HTTP_PORT = "MOLGENIS_HTTP_PORT";
  public static final String MOLGENIS_ADMIN_PW = "MOLGENIS_ADMIN_PW";

  public static final String IS_OIDC_ENABLED = "isOidcEnabled";
  public static final String MOLGENIS_OIDC_CLIENT_ID = "MOLGENIS_OIDC_CLIENT_ID";
  public static final String MOLGENIS_OIDC_CLIENT_SECRET = "MOLGENIS_OIDC_CLIENT_SECRET";
  public static final String MOLGENIS_OIDC_CLIENT_NAME = "MOLGENIS_OIDC_CLIENT_NAME";
  public static final String MOLGENIS_OIDC_DISCOVERY_URI = "MOLGENIS_OIDC_DISCOVERY_URI";
  public static final String MOLGENIS_OIDC_CALLBACK_URL = "MOLGENIS_OIDC_CALLBACK_URL";
  public static final String MOLGENIS_INCLUDE_CATALOGUE_DEMO = "MOLGENIS_INCLUDE_CATALOGUE_DEMO";

  public static final String MOLGENIS_JWT_SHARED_SECRET = "MOLGENIS_JWT_SHARED_SECRET";

  public static final String OIDC_LOGIN_PATH = "_login";
  public static final String OIDC_CALLBACK_PATH = "_callback";
  public static final String ASYNC = "async";
  public static final String ANONYMOUS = "anonymous";
  public static final String LOCALES = "locales";
  public static final String LOCALES_DEFAULT = "[\"en\"]";

  protected static final Operator[] EXISTS_OPERATIONS = {};
  protected static final Operator[] ORDINAL_OPERATORS = {EQUALS, NOT_EQUALS, BETWEEN, NOT_BETWEEN};
  protected static final Operator[] STRING_OPERATORS = {
    EQUALS, NOT_EQUALS, LIKE, NOT_LIKE, TRIGRAM_SEARCH, TEXT_SEARCH
  };
  protected static final Operator[] EQUALITY_OPERATORS = {EQUALS, NOT_EQUALS};

  // RFC 5322, see http://emailregex.com/
  protected static final String EMAIL_REGEX =
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]"
          + "+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\""
          + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")"
          + "@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
          + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\"
          + "[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
  // thank you to
  // https://www.geeksforgeeks.org/check-if-an-url-is-valid-or-not-using-regular-expression/
  protected static final String HYPERLINK_REGEX =
      "((https?)://)(www.)?"
          + "[a-zA-Z0-9@:%._\\+~#?&//=-]"
          + "{2,256}\\.[a-z]"
          + "{2,6}\\b([-a-zA-Z0-9@:%"
          + "._\\+~#?!&//=(\\)]*)";

  public static final String PRIVACY_POLICY_LEVEL = "PrivacyPolicyLevel";
  public static final String PRIVACY_POLICY_LEVEL_DEFAULT = "Level 4";
  public static final String PRIVACY_POLICY_TEXT = "PrivacyPolicyText";
  public static final String PRIVACY_POLICY_TEXT_DEFAULT = "Privacy data + medical";
  public static final String IS_PRIVACY_POLICY_ENABLED = "isPrivacyPolicyEnabled";

  private Constants() {
    // hide constructor
  }
}
