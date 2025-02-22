package org.molgenis.emx2;

public enum Operator {
  // in case of a nested query
  OR("or", "Used to define  complex subqueries that should be combined using OR operator"),
  AND("and", "Used to define  complex subqueries that should be combined using AND operator"),
  // equality
  EQUALS(
      "equals",
      "Uses '=' operator. In case of arrays '= ANY'. Will be deprecated for arrays, use CONTAINS_ANY."),
  MATCH_ANY(
      "match_any", "For arrays if there is any overlap with column values. Used to be 'equals'"),
  MATCH_ALL("match_all", "For arrays if all values are included in the column value"),
  MATCH_NONE("match_none", "For arrays to check if none of the values are include"),
  NOT_EQUALS(
      "not_equals",
      "Uses != operator. In case of array 'NOT (= ANY)'. Will be deprecated for arrays, use CONTAINS_NONE"),
  IS_NULL("is", "When 'true' then nulls will be returned, when 'false' not nulls will be returned"),
  // ordinal
  BETWEEN("between", "Uses BETWEEN operator"),
  NOT_BETWEEN("not_between", "Uses NOT BETWEEN operator"),
  // text
  LIKE("like", "Uses ILIKE '%value%'"),
  NOT_LIKE("not_like", "Uses column NOT ILIKE '%value%' OR column IS NULL"), // ilike
  TEXT_SEARCH("text_search", "Uses to_tsquery('value:*') text search operator"), // text search
  TRIGRAM_SEARCH("trigram_search", "Uses WORD_SIMILARITY operator based on trigram matches"),
  // ontology
  MATCH_ANY_INCLUDING_CHILDREN(
      "match_any_including_children",
      "Can be used for ontology(array) to find if (any of) the term exists in ontology subtree including itself"),
  MATCH_ANY_INCLUDING_PARENTS(
      "match_any_including_parents",
      "Can be used for ontology(array) to find if (any of) the term exists in ontology parents including itself"),
  MATCH_PATH(
      "match_path",
      "Can be used for ontology(array), is match_any_including_children OR match_any_including_parents");
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
