package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

import java.util.Arrays;
import java.util.stream.Stream;

public class Constants {

  public static final String SYS_COLUMN_NAME_PREFIX = "mg_";
  public static final String MG_EDIT_ROLE = "MG_EDIT_ROLE_";
  public static final String MG_ROLE_PREFIX = "MG_ROLE_";
  public static final String MG_USER_PREFIX = "MG_USER_";

  public static final String COMPOSITE_REF_SEPARATOR = ".";
  public static final String SUBSELECT_SEPARATOR = "-";
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
  public static final String PARENT_JOB = "parentJob";
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
  public static final String MOLGENIS_INSTANCE_ID = "MOLGENIS_INSTANCE_ID";

  public static final String IS_OIDC_ENABLED = "isOidcEnabled";
  public static final String MOLGENIS_OIDC_CLIENT_ID = "MOLGENIS_OIDC_CLIENT_ID";
  public static final String MOLGENIS_OIDC_CLIENT_SECRET = "MOLGENIS_OIDC_CLIENT_SECRET";
  public static final String MOLGENIS_OIDC_CLIENT_NAME = "MOLGENIS_OIDC_CLIENT_NAME";
  public static final String MOLGENIS_OIDC_DISCOVERY_URI = "MOLGENIS_OIDC_DISCOVERY_URI";
  public static final String MOLGENIS_OIDC_CALLBACK_URL = "MOLGENIS_OIDC_CALLBACK_URL";
  public static final String MOLGENIS_OIDC_UNSIGNED_TOKEN = "MOLGENIS_OIDC_UNSIGNED_TOKEN";
  public static final String MOLGENIS_INCLUDE_CATALOGUE_DEMO = "MOLGENIS_INCLUDE_CATALOGUE_DEMO";
  public static final String MOLGENIS_INCLUDE_DIRECTORY_DEMO = "MOLGENIS_INCLUDE_DIRECTORY_DEMO";
  public static final String MOLGENIS_EXCLUDE_PETSTORE_DEMO = "MOLGENIS_EXCLUDE_PETSTORE_DEMO";
  public static final String MOLGENIS_INCLUDE_TYPE_TEST_DEMO = "MOLGENIS_INCLUDE_TYPE_TEST_DEMO";
  public static final String MOLGENIS_INCLUDE_PATIENT_REGISTRY_DEMO =
      "MOLGENIS_INCLUDE_PATIENT_REGISTRY_DEMO";

  public static final String MOLGENIS_JWT_SHARED_SECRET = "MOLGENIS_JWT_SHARED_SECRET";

  public static final String OIDC_LOGIN_PATH = "_login";
  public static final String OIDC_CALLBACK_PATH = "_callback";
  public static final String ASYNC = "async";
  public static final String ANONYMOUS = "anonymous";
  public static final String LOCALES = "locales";
  public static final String LOCALES_DEFAULT = "[\"en\"]";

  protected static final Operator[] EXISTS_OPERATIONS = {EQUALS};

  protected static final Operator[] ORDINAL_OPERATORS = {
    EQUALS, NOT_EQUALS, MATCH_ANY, BETWEEN, NOT_BETWEEN, IS_NULL, MATCH_NONE
  };
  protected static final Operator[] ORDINAL_ARRAY_OPERATORS =
      Stream.concat(Arrays.stream(ORDINAL_OPERATORS), Stream.of(MATCH_ALL))
          .toArray(Operator[]::new);

  protected static final Operator[] STRING_OPERATORS = {
    EQUALS, NOT_EQUALS, LIKE, NOT_LIKE, TRIGRAM_SEARCH, TEXT_SEARCH, IS_NULL, MATCH_ANY, MATCH_NONE
  };

  protected static final Operator[] STRING_ARRAY_OPERATORS =
      Stream.concat(Arrays.stream(STRING_OPERATORS), Stream.of(MATCH_ALL)).toArray(Operator[]::new);

  protected static final Operator[] EQUALITY_OPERATORS = {
    EQUALS, NOT_EQUALS, IS_NULL, MATCH_ANY, MATCH_NONE
  };

  protected static final Operator[] EQUALITY_ARRAY_OPERATORS =
      Stream.concat(Arrays.stream(EQUALITY_OPERATORS), Stream.of(MATCH_ALL))
          .toArray(Operator[]::new);

  // n.b. we allow _SYSTEM_
  protected static final String SCHEMA_NAME_REGEX = "^(?!.* _|.*_ )[a-zA-Z][-a-zA-Z0-9 _]{0,62}$";

  protected static final String TABLE_NAME_REGEX = "^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,30}$";

  protected static final String COLUMN_NAME_REGEX = "^(?!.* _|.*_ )[a-zA-Z][a-zA-Z0-9 _]{0,62}$";

  protected static final String EMAIL_REGEX =
      "^(([^<>()[\\\\]\\\\.,;:\\s@\"]+(\\.[^<>()[\\\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@"
          + "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|"
          + "(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$|^$";
  // thank you to
  // https://www.geeksforgeeks.org/check-if-an-url-is-valid-or-not-using-regular-expression/
  // updated to also allow localhost / localhost:8080 as valid hyperlink, needed to testing
  protected static final String HYPERLINK_REGEX =
      "((https?)://)((www.)?[a-zA-Z0-9@:%._\\+~#?&//=-]{2,256}\\.[a-z]{2,6}|localhost)(:[0-9]+)?([-a-zA-Z0-9@:%._\\+~#?!&//=(\\)]*)$";

  public static final String PRIVACY_POLICY_LEVEL = "PrivacyPolicyLevel";
  public static final String PRIVACY_POLICY_LEVEL_DEFAULT = "Level 4";
  public static final String PRIVACY_POLICY_TEXT = "PrivacyPolicyText";
  public static final String PRIVACY_POLICY_TEXT_DEFAULT = "Privacy data + medical";
  public static final String IS_PRIVACY_POLICY_ENABLED = "isPrivacyPolicyEnabled";
  public static final String COMPUTED_AUTOID_TOKEN = "${mg_autoid}";
  public static final String SYSTEM_SCHEMA = "_SYSTEM_";

  public static final String CONTACT_RECIPIENTS_QUERY_SETTING_KEY = "contactRecipientsQuery";
  public static final String CONTACT_BCC_ADDRESS = "contactBccAddress";

  public static final String API_RDF = "/api/rdf";
  public static final String API_TTL = "/api/ttl";
  public static final String API_JSONLD = "/api/jsonld";
  public static final String API_FILE = "/api/file";

  private Constants() {
    // hide constructor
  }
}
