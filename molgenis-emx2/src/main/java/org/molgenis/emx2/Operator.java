package org.molgenis.emx2;

public enum Operator {
  // equality
  IS(true, "is", "Uses '=' operator. In case of arrays '= ANY'"),
  IS_NOT(true, "is_not", "Uses <> operator. In case of array 'NOT (= ANY)'"),
  // ordinal
  BETWEEN(true, "between", "Uses BETWEEN operator"),
  NOT_BETWEEN(true, "not_between", "Uses NOT BETWEEN operator"),
  GREATER_THAN(false, "gt", "greather than"),
  GREATER_THAN_EQUAL(false, "gte", "greather than or equal"),
  LESS_THAN(false, "lt", "less than"),
  LESS_THAN_EQUAL(false, "lte", "less than or equal"),
  // text
  CONTAINS(true, "contains", "Uses ILIKE '%value%'"), // ilike
  DOES_NOT_CONTAIN(
      true, "does_not_contain", "Uses column NOT ILIKE '%value%' OR column IS NULL"), // ilike
  LEXICAL_MATCH(
      true, "lexical_match", "Uses to_tsquery('value:*') text search operator"), // text search
  SIMILAR_TEXT(true, "similar_to", "Uses WORD_SIMILARITY operator based on trigram matches"),
  ANY(true, "any", "TODO");

  private boolean multivalue;
  private String abbreviation;
  private String description;

  Operator(boolean multivalue, String abbreviation, String description) {
    this.multivalue = multivalue;
    this.abbreviation = abbreviation;
    this.description = description;
  }

  public static Operator fromAbbreviation(String abbreviation) {
    for (Operator o : Operator.values()) {
      if (o.abbreviation.equals(abbreviation)) return o;
    }
    return null;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public boolean isMultivalue() {
    return multivalue;
  }
}
