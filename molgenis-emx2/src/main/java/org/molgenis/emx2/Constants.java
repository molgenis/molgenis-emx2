package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {
  public static Operator[] ORDINAL_OPERATORS = {IS, IS_NOT, BETWEEN, NOT_BETWEEN};
  public static Operator[] STRING_OPERATORS = {
    IS, IS_NOT, CONTAINS, DOES_NOT_CONTAIN, TRIGRAM_MATCH, LEXICAL_MATCH
  };
  public static Operator[] EQUALITY_OPERATORS = {IS, IS_NOT};
}
