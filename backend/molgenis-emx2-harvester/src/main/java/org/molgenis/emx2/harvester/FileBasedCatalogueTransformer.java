package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedCatalogueTransformer {

  private static final Logger logger = LoggerFactory.getLogger(FileBasedCatalogueTransformer.class);

  private final SailRepository repository;
  private final SchemaMetadata schema;

  public FileBasedCatalogueTransformer(SailRepository repository, SchemaMetadata schema) {
    this.repository = repository;
    this.schema = schema;
  }

  public TableStore transform() {
    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();

    TupleQuery query =
        repository.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, getScriptStream());

    TableMetadata resourcesTable = schema.getTableMetadata("Resources");

    TupleQueryResult evaluate = query.evaluate();
    List<Row> rows = new ArrayList<>();
    for (BindingSet bindings : evaluate) {
      Row row = Row.row();
      for (Column column : resourcesTable.getDownloadColumnNames()) {
        if (column.getName().equals("type")) {
          // TODO: This doesn't work yet...
          row.set(column.getName(), "Cohort study");
        }

        String normalizeColumnName = normalizeColumnName(column);
        Optional<Object> mapped =
            RdfToRowMapper.mapToColumnType(bindings.getValue(normalizeColumnName), column);
        if (mapped.isPresent()) {
          row.set(column.getName(), mapped.get());
        } else {
          row.set(normalizeColumnName, null);
        }
      }
      rows.add(row);
    }

    store.writeTable(
        "Resources",
        resourcesTable.getDownloadColumnNames().stream().map(Column::getName).toList(),
        rows);

    return store;
  }

  private String normalizeColumnName(Column column) {
    return column.getQualifiedName().replace("Resources.", "").replace(".", "__").replace(" ", "_");
  }

  private String getScriptStream() {
    try {
      return new String(
          Objects.requireNonNull(
                  FileBasedCatalogueTransformer.class.getResourceAsStream(
                      "queries/resources.sparql"))
              .readAllBytes());
    } catch (IOException e) {
      throw new MolgenisException("Unable to read resources sparql script", e);
    }
  }
}
