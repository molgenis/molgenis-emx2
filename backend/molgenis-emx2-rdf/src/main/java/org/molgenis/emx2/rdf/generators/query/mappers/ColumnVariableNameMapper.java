package org.molgenis.emx2.rdf.generators.query.mappers;

import org.molgenis.emx2.Column;

public class ColumnVariableNameMapper {

  public static String columnToSparql(Column column) {
    return columnNameToSparql(column.getName());
  }

  public static String columnNameToSparql(String columnName) {
    return columnName.replace(".", "__").replace(" ", "___");
  }

  public static String sparqlToColumnName(String sparql) {
    return sparql.replace("___", " ").replace("__", ".");
  }
}
