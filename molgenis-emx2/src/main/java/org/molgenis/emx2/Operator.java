package org.molgenis.emx2;

public enum Operator {
  // equality
  EQUALS("equals", "Uses '=' operator. In case of arrays '= ANY'"),
  NOT_EQUALS("not_equals", "Uses <> operator. In case of array 'NOT (= ANY)'"),
  // ordinal
  BETWEEN("between", "Uses BETWEEN operator"),
  NOT_BETWEEN("not_between", "Uses NOT BETWEEN operator"),
  // text
  CONTAINS("contains", "Uses ILIKE '%value%'"), // ilike
  NOT_CONTAINS("not_contains", "Uses column NOT ILIKE '%value%' OR column IS NULL"), // ilike
  LEXICAL_MATCH("lexical_match", "Uses to_tsquery('value:*') text search operator"), // text search
  TRIGRAM_MATCH("trigram_match", "Uses WORD_SIMILARITY operator based on trigram matches"),
  ANY("any", "TODO");

  private String abbreviation;
  private String description;

  Operator(String abbreviation, String description) {
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
}
