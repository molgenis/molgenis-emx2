package org.molgenis.emx2;

public enum Operator {
  // equality
  EQUALS("equals", "Uses '=' operator. In case of arrays '= ANY'"),
  NOT_EQUALS("not_equals", "Uses <> operator. In case of array 'NOT (= ANY)'"),
  // ordinal
  BETWEEN("between", "Uses BETWEEN operator"),
  NOT_BETWEEN("not_between", "Uses NOT BETWEEN operator"),
  // text
  LIKE("like", "Uses ILIKE '%value%'"), // ilike
  NOT_LIKE("not_like", "Uses column NOT ILIKE '%value%' OR column IS NULL"), // ilike
  TEXT_SEARCH("text_search", "Uses to_tsquery('value:*') text search operator"), // text search
  TRIGRAM_SEARCH("trigram_search", "Uses WORD_SIMILARITY operator based on trigram matches"),
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
