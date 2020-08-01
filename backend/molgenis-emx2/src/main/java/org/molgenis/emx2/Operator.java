package org.molgenis.emx2;

public enum Operator {
  // in case of a nested query
  OR("or", "Used to define  complex subqueries that should be combined using OR operator"),
  AND("and", "Used to define  complex subqueries that should be combined using AND operator"),
  // equality
  EQUALS("equals", "Uses '=' operator. In case of arrays '= ANY'"),
  NOT_EQUALS("not_equals", "Uses != operator. In case of array 'NOT (= ANY)'"),
  // ordinal
  BETWEEN("between", "Uses BETWEEN operator"),
  NOT_BETWEEN("not_between", "Uses NOT BETWEEN operator"),
  // text
  LIKE("like", "Uses ILIKE '%value%'"),
  NOT_LIKE("not_like", "Uses column NOT ILIKE '%value%' OR column IS NULL"), // ilike
  TEXT_SEARCH("text_search", "Uses to_tsquery('value:*') text search operator"), // text search
  TRIGRAM_SEARCH("trigram_search", "Uses WORD_SIMILARITY operator based on trigram matches");

  private String name;
  private String description;

  Operator(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public static Operator fromAbbreviation(String abbreviation) {
    for (Operator o : Operator.values()) {
      if (o.name.equals(abbreviation)) return o;
    }
    return null;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
