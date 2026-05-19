package org.molgenis.emx2.rdf.generators.query;

import org.molgenis.emx2.Column;

/**
 * Encodes and decodes column names to and from valid SPARQL variable names.
 *
 * <p>Database column names may contain characters that are not valid in SPARQL variable names, such
 * as dots (e.g. in composite keys like {@code "ref.id"}) and spaces. This class provides a
 * consistent encoding scheme to safely represent such names as SPARQL variables, and to recover the
 * original column name from the encoded form.
 *
 * <p>Encoding scheme:
 *
 * <ul>
 *   <li>{@code '.'} is encoded as {@code "__"} (double underscore)
 *   <li>{@code ' '} is encoded as {@code "___"} (triple underscore)
 * </ul>
 *
 * <p><b>Limitation:</b> column names that already contain {@code "__"} or {@code "___"} will not
 * round-trip correctly through encode and decode.
 */
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
