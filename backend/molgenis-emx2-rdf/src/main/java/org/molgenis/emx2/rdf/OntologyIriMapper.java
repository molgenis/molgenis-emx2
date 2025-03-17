package org.molgenis.emx2.rdf;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;

public class OntologyIriMapper {
  // schema name -> ontology name -> cell value (name) -> ontologyTermURI
  private final Map<String, Map<String, Map<String, IRI>>> irisPerSchema = new HashMap<>();

  public OntologyIriMapper(Collection<Table> tables) {
    addAll(tables);
  }

  public OntologyIriMapper(Table table) {
    add(table);
  }

  public OntologyIriMapper() {}

  private void addAll(Collection<Table> tables) {
    for (Table table : tables) {
      add(table);
    }
  }

  private void add(Table table) {
    switch (table.getMetadata().getTableType()) {
      case DATA -> addDataTable(table);
      case ONTOLOGIES -> addOntologyTable(table);
    }
  }

  private void addDataTable(Table table) {
    for (Column column : table.getMetadata().getColumns()) {
      if (column.isOntology()) {
        addOntologyTable(column.getRefTable().getTable());
      }
    }
  }

  private void addOntologyTable(Table ontology) {
    Map<String, Map<String, IRI>> irisPerOntology =
        irisPerSchema.getOrDefault(ontology.getSchema().getName(), new HashMap<>());
    Map<String, IRI> irisPerName =
        irisPerOntology.getOrDefault(ontology.getName(), new HashMap<>());
    // Skips adding if already done.
    if (irisPerName.isEmpty()) {
      for (Row row : queryOntology(ontology)) {
        if (row.getString("ontologyTermURI") == null) continue;
        irisPerName.put(row.getString("name"), Values.iri(row.getString("ontologyTermURI")));
      }
      irisPerOntology.put(ontology.getName(), irisPerName);
      irisPerSchema.put(ontology.getSchema().getName(), irisPerOntology);
    }
  }

  private List<Row> queryOntology(Table table) {
    return table.query().select(s("name"), s("ontologyTermURI")).retrieveRows();
  }

  /**
   * Retrieves ontologyTermURI for single value.
   *
   * @return IRI if match is found, otherwise {@code null}
   */
  public IRI get(String schemaName, String ontologyTableName, String value) {
    Map<String, Map<String, IRI>> schemaIris = irisPerSchema.get(schemaName);
    if (schemaIris == null) return null;
    Map<String, IRI> ontologyIris = schemaIris.get(ontologyTableName);
    if (ontologyIris == null) return null;
    return ontologyIris.get(value);
  }

  /**
   * Retrieves ontologyTermURI for multiple values.
   *
   * @return a {@link Map} with the input {@code values} as key and the found match (or {@code null}
   *     for no match) as value
   * @see #get(String, String, String)
   */
  public Map<String, IRI> map(String schemaName, String ontologyTableName, String... values) {
    Map<String, IRI> iriMap = new HashMap<>();
    for (String value : values) {
      iriMap.put(value, get(schemaName, ontologyTableName, value));
    }
    return iriMap;
  }
}
