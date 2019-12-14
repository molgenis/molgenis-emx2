package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.*;

public class Constants {
  protected static final Operator[] ORDINAL_OPERATORS = {EQUALS, NOT_EQUALS, BETWEEN, NOT_BETWEEN};
  protected static final Operator[] STRING_OPERATORS = {
    EQUALS, NOT_EQUALS, CONTAINS, DOES_NOT_CONTAIN, TRIGRAM_MATCH, LEXICAL_MATCH
  };
  protected static final Operator[] EQUALITY_OPERATORS = {EQUALS, NOT_EQUALS};
}
