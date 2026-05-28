package org.molgenis.emx2.fairmapper;

import java.util.List;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;

public class SparqlSelectRdfTransformer implements RdfTransformer {

  private final QueryGenerator queryGenerator;
  private final SchemaMetadata schema;
  private final List<String> tables;

  public SparqlSelectRdfTransformer(
      QueryGenerator queryGenerator, SchemaMetadata schema, List<String> tables) {
    this.queryGenerator = queryGenerator;
    this.schema = schema;
    this.tables = tables;
  }

  @Override
  public TableStore transform(SailRepository repository) {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    try (SailRepositoryConnection conn = repository.getConnection()) {
      tables.forEach(table -> addTableDataToStore(table, conn, tableStore));
    }
    return tableStore;
  }

  private void addTableDataToStore(
      String table, SailRepositoryConnection conn, InMemoryTableStore tableStore) {
    String query = queryGenerator.generate(schema.getTableMetadata(table));
    TupleQuery prepared = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try (TupleQueryResult evaluate = prepared.evaluate()) {
      List<String> columnNames =
          evaluate.getBindingNames().stream()
              .map(ColumnNameSparqlEncoder::decodeSparqlVariable)
              .toList();

      tableStore.writeTable(table, columnNames, evaluate.stream().map(this::mapToRow).toList());
    }
  }

  private Row mapToRow(BindingSet bindings) {
    Row row = new Row();
    for (Binding binding : bindings) {
      row.set(
          ColumnNameSparqlEncoder.decodeSparqlVariable(binding.getName()),
          binding.getValue().stringValue());
    }
    return row;
  }
}
