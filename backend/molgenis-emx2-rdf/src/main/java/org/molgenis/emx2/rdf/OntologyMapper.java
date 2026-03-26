package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

public class OntologyMapper {
  private final Map<String, String> alternativeIdToName = new HashMap<>();
  private final Map<String, String> ontologyTermUriToName = new HashMap<>();
  private final Map<String, String> lowerNameToName = new HashMap<>();

  public OntologyMapper(Table ontologyTable) {
    List<Row> rows =
        ontologyTable
            .query()
            .select(s("name"), s("ontologyTermURI"), s("alternativeIds"))
            .retrieveRows();

    for (Row row : rows) {
      String name = row.getString("name");
      if (name == null) continue;

      String uri = row.getString("ontologyTermURI");
      if (uri != null) {
        ontologyTermUriToName.put(uri, name);
      }

      String[] altIds = row.getStringArray("alternativeIds");
      if (altIds != null) {
        for (String altId : altIds) {
          alternativeIdToName.put(altId, name);
        }
      }

      lowerNameToName.put(name.toLowerCase(), name);
    }
  }

  public String resolve(String uri) {
    String match = alternativeIdToName.get(uri);
    if (match != null) return match;
    match = ontologyTermUriToName.get(uri);
    if (match != null) return match;
    return lowerNameToName.get(uri.toLowerCase());
  }
}
