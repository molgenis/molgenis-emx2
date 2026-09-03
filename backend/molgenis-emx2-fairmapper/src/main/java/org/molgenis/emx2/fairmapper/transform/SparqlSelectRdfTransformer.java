package org.molgenis.emx2.fairmapper.transform;

import java.util.List;
import java.util.stream.Stream;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.rdf.generators.query.ColumnNameSparqlEncoder;
import org.molgenis.emx2.rdf.generators.query.QueryGenerator;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

public class SparqlSelectRdfTransformer implements RdfTransformer {

  private static final String ARRAY_SEPARATOR_REGEX = "\\|";
  private static final String SUBJECT_COLUMN_PREFIX =
      TableQueryGenerator.SUBJECT_VARIABLE.getVarName();

  private final QueryGenerator queryGenerator;

  public SparqlSelectRdfTransformer(QueryGenerator queryGenerator) {
    this.queryGenerator = queryGenerator;
  }

  @Override
  public InMemoryTableStore transform(
      Repository repository, SchemaMetadata schema, List<String> tables) {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    try (RepositoryConnection conn = repository.getConnection()) {
      tables.forEach(table -> addTableDataToStore(table, schema, conn, tableStore));
    }
    return tableStore;
  }

  private void addTableDataToStore(
      String table,
      SchemaMetadata schema,
      RepositoryConnection conn,
      InMemoryTableStore tableStore) {
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
    List<String> columnsToSplit =
        Stream.concat(
                arrayValueColumnNames(tableMetadata).stream(),
                refArraySubjectColumnNames(tableMetadata).stream())
            .toList();

    for (Row row : rows) {
      splitArrayColumns(row, columnsToSplit);
    }
  }

  private List<String> arrayValueColumnNames(TableMetadata tableMetadata) {
    return tableMetadata.getDownloadColumnNames().stream()
        .filter(Column::isArray)
        .map(Column::getName)
        .toList();
  }

  /**
   * Reference array columns (e.g. REF_ARRAY) get a "_subject_&lt;column&gt;" binding holding the
   * IRIs of the referenced rows, aggregated the same way as array values and thus also needing to
   * be split. Ontology (array) columns are excluded since their value already is the subject IRI,
   * so they don't get a separate "_subject_" binding.
   */
  private List<String> refArraySubjectColumnNames(TableMetadata tableMetadata) {
    return tableMetadata.getColumns().stream()
        .filter(column -> column.isReference() && column.isArray() && !column.isOntology())
        .map(column -> SUBJECT_COLUMN_PREFIX + column.getName())
        .toList();
  }

  private void splitArrayColumns(Row row, List<String> columnNames) {
    for (String name : columnNames) {
      if (row.notNull(name)) {
        String[] split = row.getString(name).split(ARRAY_SEPARATOR_REGEX);
        row.set(name, split);
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
