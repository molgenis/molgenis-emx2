package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {

  public static final String REF_SCHEMA_NAME = "refSchema";
  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_FROM = "refFrom";
  public static final String REF_TO = "refTo";
  public static final String REF_JS_TEMPLATE = "refJsTemplate";
  public static final String MAPPED_BY = "mappedBy";
  public static final String CASCADE_DELETE = "cascadeDelete";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String DESCRIPTION = "description";
  public static final String JSONLD_TYPE = "jsonldType";
  public static final String JSONLD_CONTEXT = "jsonldContext";
  public static final String DEFINITION = "definition";
  public static final String ROLE = "role";
  public static final String KEY = "key";
  public static final String INHERITED = "inherited";
  public static final String MG_TABLECLASS = "mg_tableclass";
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
