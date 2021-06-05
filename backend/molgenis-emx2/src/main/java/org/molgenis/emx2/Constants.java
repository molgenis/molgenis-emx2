package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {

  public static final String COMPOSITE_REF_SEPARATOR = ".";
  public static final String REF_SCHEMA_NAME = "refSchema";
  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_LINK = "refLink";
  public static final String REF_LABEL = "refLabel";
  public static final String REF_BACK = "refBack";
  public static final String CASCADE_DELETE = "cascadeDelete";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String FORM = "form";
  public static final String VISIBLE_EXPRESSION = "visible";
  public static final String DESCRIPTION = "description";
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
  public static final String SETTINGS_NAME = "key";
  public static final String SETTINGS_VALUE = "value";

  public static final String MOLGENIS_POSTGRES_URI = "MOLGENIS_POSTGRES_URI";
  public static final String MOLGENIS_POSTGRES_USER = "MOLGENIS_POSTGRES_USER";
  public static final String MOLGENIS_POSTGRES_PASS = "MOLGENIS_POSTGRES_PASS";
  public static final String MOLGENIS_HTTP_PORT = "MOLGENIS_HTTP_PORT";
  protected static final Operator[] EXISTS_OPERATIONS = {};
  protected static final Operator[] ORDINAL_OPERATORS = {EQUALS, NOT_EQUALS, BETWEEN, NOT_BETWEEN};
  protected static final Operator[] STRING_OPERATORS = {
    EQUALS, NOT_EQUALS, LIKE, NOT_LIKE, TRIGRAM_SEARCH, TEXT_SEARCH
  };
  protected static final Operator[] EQUALITY_OPERATORS = {EQUALS, NOT_EQUALS};

  private Constants() {
    // hide constructor
  }
}
