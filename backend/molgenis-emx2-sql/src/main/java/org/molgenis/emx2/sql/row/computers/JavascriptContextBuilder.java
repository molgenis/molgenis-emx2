package org.molgenis.emx2.sql.row.computers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;

/**
 * Builds a JavaScript evaluation context from a {@link Row}, used to execute column expressions
 * (computed values, default values, validation scripts, visibility rules).
 *
 * <p>Column values are keyed by {@link Column#getIdentifier()} (camelCase) so they are valid
 * JavaScript identifiers. References become nested maps; file columns are omitted. Database-level
 * JavaScript bindings (e.g. current user, session context) are merged in last.
 */
public class JavascriptContextBuilder {

  private JavascriptContextBuilder() {
    throw new IllegalStateException("Utility class");
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
    Map<String, Object> map = new LinkedHashMap<>();
    for (Column c : columns) {
      if (c.isReference()) {
        map.put(c.getIdentifier(), getRefFromRow(row, c));
      } else if (c.isFile()) {
        // skip file
      } else {
        map.put(c.getIdentifier(), row.get(c));
      }
    }
    addJavaScriptBindings(columns, map);
    return map;
  }

  private static Object getRefFromRow(Row row, Column c) {
    if (c.isRefArray() || c.isRefback()) {
      List<Map> result = new ArrayList<>();
      for (Reference ref : c.getReferences()) {
        if (!ref.isOverlapping()) {
          // must be a list
          if (row.getValueMap().get(ref.getName()) != null) {
            int i = 0;
            for (Object value : (Object[]) row.get(ref.getName(), ref.getPrimitiveType())) {
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
          List<String> path = new ArrayList();
          path.add(c.getIdentifier());
          path.addAll(ref.getPath());
          putMap(result, ref.getPath(), row.get(ref.getName(), ref.getPrimitiveType()));
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
      if (result.get(path.get(0)) == null) {
        result.put(path.get(0), new LinkedHashMap<>());
      }
      putMap((Map) result.get(path.get(0)), path.subList(1, path.size()), value);
    }
  }

  private static void addJavaScriptBindings(List<Column> columns, Map<String, Object> values) {
    if (columns.isEmpty()) return;
    Column column = columns.get(0);
    if (column.getTable() == null) return;
    if (column.getSchema() == null) return;
    if (column.getSchema().getDatabase() == null) return;
    Map<String, Supplier<Object>> bindings =
        column.getSchema().getDatabase().getJavaScriptBindings();
    for (String key : bindings.keySet()) {
      values.put(key, bindings.get(key).get());
    }
  }
}
