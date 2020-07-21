package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {

  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_COLUMN_NAMES = "refColumns";
  public static final String MAPPED_BY = "mappedBy";
  public static final String CASCADE_DELETE = "cascadeDelete";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String DESCRIPTION = "description";
  public static final String DEFINITION = "definition";
  public static final String ROLE = "role";
  public static final String KEY = "key";

  private Constants() {
    // hide constructor
  }

  protected static final Operator[] ORDINAL_OPERATORS = {EQUALS, NOT_EQUALS, BETWEEN, NOT_BETWEEN};
  protected static final Operator[] STRING_OPERATORS = {
    EQUALS, NOT_EQUALS, LIKE, NOT_LIKE, TRIGRAM_SEARCH, TEXT_SEARCH
  };
  protected static final Operator[] EQUALITY_OPERATORS = {EQUALS, NOT_EQUALS};
}
