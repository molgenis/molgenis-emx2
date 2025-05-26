package org.molgenis.emx2.rdf.generators.schema;

import java.util.List;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.writers.RdfWriter;

/**
 * {@link RdfGenerator} where every layer (schema/table/row) simply is a combination of all
 * lower-level outputs: <br>
 * - Row -> a single row <br>
 * - Table -> all rows in a table <br>
 * - Schema -> all rows of all tables in a schema <br>
 */
public abstract class RdfRowsGenerator extends RdfGenerator {
  public RdfRowsGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  @Override
  public void generate(Schema schema) {
    schema.getTablesSorted().forEach(this::generate);
  }

  @Override
  public void generate(Table table) {
    retrieveRows(table, null);
  }

  @Override
  public void generate(Table table, PrimaryKey primaryKey) {
    retrieveRows(table, primaryKey);
  }

  private void retrieveRows(Table table, PrimaryKey primaryKey) {
    tablesToDescribe(table.getSchema(), table).forEach(i -> processRows(i, getRows(i, primaryKey)));
  }

  abstract void processRows(Table table, List<Row> rows);
}
