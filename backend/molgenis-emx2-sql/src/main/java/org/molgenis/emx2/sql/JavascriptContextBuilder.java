package org.molgenis.emx2.sql;

import java.util.*;
import java.util.function.Supplier;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;

/**
 * Builds the variable context for evaluating column expressions (computed values, default values,
 * validation scripts, visibility rules) against a {@link Row}.
 *
 * <p>Values are keyed by {@link Column#getIdentifier()} (camelCase) so they are valid JavaScript
 * identifiers. Reference columns become nested maps; file columns are omitted. Database-level
 * bindings (e.g. current user) are merged in last.
 */
public class JavascriptContextBuilder {

  private JavascriptContextBuilder() {
    // hide constructor
  }

  /**
   * Converts {@code row} to a {@code Map<identifier, value>} ready for use in Javascript
   * interactions.
   *
   * @param columns Columns that should be included in the context, used to determine reference
   *     structure and bindings
   * @param row the row whose values are mapped
   * @return a mutable map keyed by column identifier; reference columns are nested maps or lists
   */
  public static Map<String, Object> fromRow(List<Column> columns, Row row) {
    Map<String, Object> context = new HashMap<>();
    for (Column column : columns) {
      updateContext(context, row, column);
    }

    addJavaScriptBindings(context, columns);

    return context;
  }

  public static void updateContext(Map<String, Object> context, Row row, Column column) {
    if (column.isReference()) {
      context.put(column.getIdentifier(), getRefFromRow(row, column));
    } else if (column.isFile()) {
      // skip file
    } else if (column.isArray()) {
      Object value = Optional.ofNullable(row.get(column)).orElse(new Object[0]);
      context.put(column.getIdentifier(), value);
    } else {
      context.put(column.getIdentifier(), row.get(column));
    }
  }

  private static Object getRefFromRow(Row row, Column c) {
    if (c.isRefArray() || c.isRefback()) {
      List<Map> result = new ArrayList<>();
      for (Reference ref : c.getReferences()) {
        if (!ref.isOverlapping()) {
          // must be a list
          Object values = row.get(ref.getColumnName(), ref.getPrimitiveType());
          if (values != null) {
            int i = 0;
            for (Object value : (Object[]) values) {
              if (i == result.size()) {
                result.add(new LinkedHashMap<>());
              }
              putMap(result.get(i), ref.getPath(), value);
              i++;
            }
          }
        }
      }
      return result;
    } else {
      // returns a value
      Map<String, Object> result = new LinkedHashMap<>();
      for (Reference ref : c.getReferences()) {
        if (!ref.isOverlapping()) {
          putMap(result, ref.getPath(), row.get(ref.getColumnName(), ref.getPrimitiveType()));
        }
      }
      return result;
    }
  }

  // put map hierarchy
  private static void putMap(Map<String, Object> result, List<String> path, Object value) {
    if (path.size() == 1) {
      result.put(path.get(0), value);
    } else {
      result.computeIfAbsent(path.getFirst(), k -> new LinkedHashMap<>());
      putMap((Map) result.get(path.getFirst()), path.subList(1, path.size()), value);
    }
  }

  private static void addJavaScriptBindings(Map<String, Object> context, List<Column> columns) {
    if (columns.isEmpty()) return;
    Column column = columns.getFirst();
    if (column.getTable() == null) return;
    if (column.getSchema() == null) return;
    if (column.getSchema().getDatabase() == null) return;
    Map<String, Supplier<Object>> bindings =
        column.getSchema().getDatabase().getJavaScriptBindings();

    for (Map.Entry<String, Supplier<Object>> entry : bindings.entrySet()) {
      context.put(entry.getKey(), entry.getValue().get());
    }
  }
}
