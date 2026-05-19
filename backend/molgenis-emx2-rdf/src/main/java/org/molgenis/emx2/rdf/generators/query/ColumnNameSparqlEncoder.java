package org.molgenis.emx2.rdf.generators.query;

import org.molgenis.emx2.Column;

public class ColumnNameSparqlEncoder {

  private ColumnNameSparqlEncoder() {
    throw new IllegalStateException("Utility class");
  }

  public static String encodeSparqlVariable(Column column) {
    return encodeSparqlVariable(column.getName());
  }

  public static String encodeSparqlVariable(String... path) {
    return String.join(".", path).replace(".", "__").replace(" ", "___");
  }

  public static String decodeSparqlVariable(String sparql) {
    return sparql.replace("___", " ").replace("__", ".");
  }
}
