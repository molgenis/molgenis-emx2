package org.molgenis.emx2.fairmapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyResolver {

  private static final Logger logger = LoggerFactory.getLogger(OntologyResolver.class);
  private final Map<String, Map<String, String>> ontologyValues = new HashMap<>();

  public void resolve(Column column, Row row) {
    String key = referenceKey(column);
    Map<String, String> ontologyMapping =
        ontologyValues.computeIfAbsent(key, s -> generateMapping(column));
    String value = row.getString(column.getName());
    if (!ontologyMapping.containsKey(value)) {
      logger.warn("No ontology of type: {} for value: {}", key, value);
      row.clear(column.getName());
    } else {
      row.set(column.getName(), ontologyMapping.get(value));
    }
  }

  private Map<String, String> generateMapping(Column column) {
    List<Row> ontologyRows =
        column
            .getRefTable()
            .getTable()
            .query()
            .select(SelectColumn.s("ontologyTermURI"), SelectColumn.s("name"))
            .retrieveRows();

    return ontologyRows.stream()
        .filter(row -> row.getString("ontologyTermURI") != null)
        .collect(Collectors.toMap(r -> r.getString("ontologyTermURI"), r -> r.getString("name")));
  }

  private String referenceKey(Column column) {
    return column.getRefSchemaName() + "." + column.getRefTableName();
  }
}
