package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {
  protected static final Operator[] ORDINAL_OPERATORS = {IS, IS_NOT, BETWEEN, NOT_BETWEEN};
  protected static final Operator[] STRING_OPERATORS = {
    IS, IS_NOT, CONTAINS, DOES_NOT_CONTAIN, TRIGRAM_MATCH, LEXICAL_MATCH
  };
  protected static final Operator[] EQUALITY_OPERATORS = {IS, IS_NOT};
}
