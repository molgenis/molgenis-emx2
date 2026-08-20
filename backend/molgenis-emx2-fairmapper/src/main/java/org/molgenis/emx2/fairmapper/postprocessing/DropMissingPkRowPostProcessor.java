package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops rows that are missing a value for one or more of their primary key columns, for each of the
 * configured tables.
 */
public class DropMissingPkRowPostProcessor implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(DropMissingPkRowPostProcessor.class);

  private final SchemaMetadata schema;
  private final List<String> tableNames;

  public DropMissingPkRowPostProcessor(SchemaMetadata schema, List<String> tableNames) {
    this.schema = schema;
    this.tableNames = tableNames;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    for (String tableName : tableNames) {
      TableMetadata table = schema.getTableMetadata(tableName);
      List<Column> pkColumns = table.getPrimaryKeyColumns();

      List<Row> rows = new ArrayList<>();
      for (Row row : tableStore.readTable(tableName)) {
        if (hasAllPkValues(row, pkColumns)) {
          rows.add(row);
        } else {
          logger.warn("Row is missing a primary key value, dropping: {}", row);
        }
      }

      tableStore.writeTable(
          tableName,
          rows.stream().flatMap(row -> row.getColumnNames().stream()).distinct().toList(),
          rows);
    }
  }

  private static boolean hasAllPkValues(Row row, List<Column> pkColumns) {
    return pkColumns.stream().allMatch(pk -> row.notNull(pk.getName()));
  }
}
