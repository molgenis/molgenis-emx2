package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;

/**
 * Derives the value of a single field from a prioritised list of candidate fields on the same row.
 *
 * <p>For every row of {@code table}, the candidate fields in {@code deriveFromFields} are checked
 * in order and the first non-null value found is written to {@code field}. This is useful when
 * source data may express the same information under different field names (e.g. mutually exclusive
 * alternatives), and downstream consumers should only need to read a single, canonical field.
 *
 * <p>If none of the candidate fields have a value, processing fails with a {@link
 * MolgenisException}.
 */
public class CoalesceFieldPostProcessor implements PostProcessor {

  private final String table;
  private final String field;
  private final String[] deriveFromFields;
  private final boolean strict;

  /**
   * @param deriveFromFields candidate fields to derive the value from, in priority order
   */
  public CoalesceFieldPostProcessor(
      String table, String field, boolean strict, String... deriveFromFields) {
    this.table = table;
    this.field = field;
    this.strict = strict;
    if (deriveFromFields == null || deriveFromFields.length == 0) {
      throw new MolgenisException("No fields to derive from for " + field + " in table " + table);
    }
    this.deriveFromFields = deriveFromFields;
  }

  @Override
  public void process(TableStore tableStore) {
    tableStore.processTable(
        table,
        (iterator, source) ->
            iterator.forEachRemaining(
                row -> {
                  Object value = getValue(row);

                  if (value == null && strict) {
                    throw new MolgenisException(
                        "Cannot derive value for field: " + field + " from table: " + table);
                  }

                  row.set(field, value);
                }));
  }

  @Nullable
  private Object getValue(Row row) {
    Map<String, Object> valueMap = row.getValueMap();
    return Arrays.stream(deriveFromFields)
        .map(valueMap::get)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
