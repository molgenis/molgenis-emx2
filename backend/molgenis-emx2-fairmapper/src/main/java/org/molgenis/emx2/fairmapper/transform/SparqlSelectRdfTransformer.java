package org.molgenis.emx2.fairmapper.transform;

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;

public class SparqlSelectRdfTransformer implements RdfTransformer {

  private static final String ARRAY_SEPARATOR_REGEX = "\\|";

  private final QueryGenerator queryGenerator;
  private final SchemaMetadata schema;
  private final List<String> tables;

  public SparqlSelectRdfTransformer(
      QueryGenerator queryGenerator, SchemaMetadata schema, List<String> tables) {
    this.queryGenerator = queryGenerator;
    this.schema = schema;
    this.tables = tables;

    checkTableExistence(schema, tables);
  }

  private static void checkTableExistence(SchemaMetadata schema, List<String> tables) {
    String missing =
        tables.stream()
            .filter(name -> null == schema.getTableMetadata(name))
            .collect(Collectors.joining(", "));
    if (!missing.isBlank()) {
      throw new MolgenisException(
          "Unknown table(s) provided to transformer: "
              + missing
              + " for schema: "
              + schema.getName());
    }
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
    TableMetadata tableMetadata = schema.getTableMetadata(table);
    String query = queryGenerator.generate(tableMetadata);
    TupleQuery prepared = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

    try (TupleQueryResult evaluate = prepared.evaluate()) {
      List<String> columnNames =
          evaluate.getBindingNames().stream()
              .map(ColumnNameSparqlEncoder::decodeSparqlVariable)
              .toList();

      List<Row> rows = evaluate.stream().map(this::mapToRow).toList();
      mapRowArrays(rows, tableMetadata);

      tableStore.writeTable(table, columnNames, rows);
    }
  }

  /**
   * Sparql doesn't support array types out of the box. Thus, we expect to split array columns with
   * a set separator.
   */
  private void mapRowArrays(List<Row> rows, TableMetadata tableMetadata) {
    List<String> arrayColumnNames =
        tableMetadata.getDownloadColumnNames().stream()
            .filter(Column::isArray)
            .map(Column::getName)
            .toList();

    for (Row row : rows) {
      for (String name : arrayColumnNames) {
        if (row.notNull(name)) {
          String[] split = row.getString(name).split(ARRAY_SEPARATOR_REGEX);
          row.set(name, split);
        }
      }
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
