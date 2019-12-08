package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {
  public static Operator[] ORDINAL_OPERATORS = {
    IS, IS_NOT, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL, BETWEEN, NOT_BETWEEN
  };

  public static Operator[] STRING_OPERATORS = {
    IS, IS_NOT, CONTAINS, DOES_NOT_CONTAIN, SIMILAR_TEXT, LEXICAL_MATCH
  };

  public static Operator[] EQUALITY_OPERATORS = {IS, IS_NOT};
}
