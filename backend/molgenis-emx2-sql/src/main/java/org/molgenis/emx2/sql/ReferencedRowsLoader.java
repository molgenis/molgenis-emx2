package org.molgenis.emx2.sql;

import static org.molgenis.emx2.FilterBean.and;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisObjectMapper;
import org.molgenis.emx2.utils.TypeUtils;

final class ReferencedRowsLoader {

  private static final String ARRAY_LENGTH = "length";

  private final Database database;
  private final Map<String, Column> referenceColumnsByIdentifier = new LinkedHashMap<>();
  private final Map<String, Selection> selectionsByIdentifier = new LinkedHashMap<>();
  private final Map<String, Map<List<Object>, Map<String, Object>>> loadedByIdentifier =
      new HashMap<>();

  ReferencedRowsLoader(Database database, List<Column> columns) {
    this.database = database;
    for (Column column : columns) {
      if (column.isRef() || column.isRefArray()) {
        referenceColumnsByIdentifier.put(column.getIdentifier(), column);
      }
    }
    for (Column column : columns) {
      for (List<String> path : column.getExpressionPaths()) {
        addPath(path);
      }
    }
  }

  private void addPath(List<String> path) {
    Column column = referenceColumnsByIdentifier.get(path.getFirst());
    List<String> fieldPath = path.subList(1, path.size());
    if (column == null || isAvailableWithoutLoading(column, fieldPath)) {
      return;
    }
    TableMetadata refTable = column.getRefTable();
    selectionsByIdentifier
        .computeIfAbsent(column.getIdentifier(), identifier -> Selection.ofKeys(refTable))
        .addPath(refTable, fieldPath);
  }

  private static boolean isAvailableWithoutLoading(Column column, List<String> fieldPath) {
    if (column.isRefArray() && fieldPath.equals(List.of(ARRAY_LENGTH))) {
      return true;
    }
    return getKeyReferences(column).stream()
        .map(Reference::getPath)
        .anyMatch(
            keyPath ->
                keyPath.size() >= fieldPath.size()
                    && keyPath.subList(0, fieldPath.size()).equals(fieldPath));
  }

  void load(List<Row> rows) {
    loadedByIdentifier.clear();
    if (database == null) {
      return;
    }
    for (Map.Entry<String, Selection> entry : selectionsByIdentifier.entrySet()) {
      Column column = referenceColumnsByIdentifier.get(entry.getKey());
      List<List<Object>> keys =
          rows.stream()
              .flatMap(row -> getKeys(row, column).stream())
              .filter(key -> !key.contains(null))
              .distinct()
              .toList();
      if (!keys.isEmpty()) {
        loadedByIdentifier.put(entry.getKey(), retrieve(column, entry.getValue(), keys));
      }
    }
  }

  void updateContext(Map<String, Object> context, Row row, Column column) {
    Map<List<Object>, Map<String, Object>> loaded = loadedByIdentifier.get(column.getIdentifier());
    if (loaded == null) {
      return;
    }

    List<List<Object>> keys = getKeys(row, column);
    if (column.isRefArray()) {
      List<?> current = (List<?>) context.get(column.getIdentifier());
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < current.size(); i++) {
        Map<String, Object> loadedRow = i < keys.size() ? loaded.get(keys.get(i)) : null;
        values.add(loadedRow != null ? loadedRow : current.get(i));
      }
      context.put(column.getIdentifier(), values);
    } else if (!keys.isEmpty() && loaded.containsKey(keys.getFirst())) {
      context.put(column.getIdentifier(), loaded.get(keys.getFirst()));
    }
  }

  private Map<List<Object>, Map<String, Object>> retrieve(
      Column column, Selection selection, List<List<Object>> keys) {
    Schema schema = database.getSchema(column.getRefSchemaName());
    Table table = schema == null ? null : schema.getTable(column.getRefTableName());
    if (table == null) {
      return Map.of();
    }

    List<Reference> keyReferences = getKeyReferences(column);
    List<Filter> filters = new ArrayList<>();
    for (int i = 0; i < keyReferences.size(); i++) {
      int keyPart = i;
      List<Object> values = keys.stream().map(key -> key.get(keyPart)).distinct().toList();
      filters.add(toFilter(table.getMetadata(), keyReferences.get(i).getPath(), values));
    }

    String json = table.select(selection.toSelectColumns()).where(and(filters)).retrieveJSON();
    Map<List<Object>, Map<String, Object>> rowsByKey = new HashMap<>();
    for (Map<String, Object> referencedRow : parseRows(json)) {
      rowsByKey.put(getKey(referencedRow, keyReferences), referencedRow);
    }
    return rowsByKey;
  }

  private static Filter toFilter(TableMetadata table, List<String> path, List<Object> values) {
    Column column = table.getColumnByIdentifier(path.getFirst());
    if (path.size() == 1) {
      return f(column.getName(), EQUALS, values);
    }
    return f(
        column.getName(), toFilter(column.getRefTable(), path.subList(1, path.size()), values));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parseRows(String json) {
    try {
      Map<String, Object> result =
          MolgenisObjectMapper.INTERNAL.getReader().forType(Map.class).readValue(json);
      Iterator<Object> rootValues = result.values().iterator();
      Object rows = rootValues.hasNext() ? rootValues.next() : null;
      return rows == null ? List.of() : (List<Map<String, Object>>) rows;
    } catch (JsonProcessingException e) {
      throw new MolgenisException("Failed to read referenced rows", e);
    }
  }

  private static List<Object> getKey(
      Map<String, Object> referencedRow, List<Reference> references) {
    List<Object> key = new ArrayList<>();
    for (Reference reference : references) {
      Object value = referencedRow;
      for (String identifier : reference.getPath()) {
        value = value instanceof Map<?, ?> map ? map.get(identifier) : null;
      }
      key.add(toKeyValue(value, reference));
    }
    return key;
  }

  private static List<List<Object>> getKeys(Row row, Column column) {
    List<Reference> references = getKeyReferences(column);
    if (!column.isRefArray()) {
      List<Object> key = new ArrayList<>();
      for (Reference reference : references) {
        key.add(
            toKeyValue(
                row.get(reference.getColumnName(), reference.getPrimitiveType()), reference));
      }
      return key.stream().allMatch(Objects::isNull) ? List.of() : List.of(key);
    }

    List<List<Object>> keys = new ArrayList<>();
    for (int part = 0; part < references.size(); part++) {
      Reference reference = references.get(part);
      Object[] values = (Object[]) row.get(reference.getColumnName(), reference.getPrimitiveType());
      for (int i = 0; values != null && i < values.length; i++) {
        if (i == keys.size()) {
          keys.add(new ArrayList<>(Collections.nCopies(references.size(), null)));
        }
        keys.get(i).set(part, toKeyValue(values[i], reference));
      }
    }
    return keys;
  }

  private static Object toKeyValue(Object value, Reference reference) {
    if (value == null) {
      return null;
    }
    ColumnType type = reference.getPrimitiveType();
    ColumnType elementType =
        type.isArray() ? ColumnType.valueOf(type.getBaseType().name().replace("_ARRAY", "")) : type;
    return TypeUtils.getTypedValue(value, elementType);
  }

  private static List<Reference> getKeyReferences(Column column) {
    return column.getReferences().stream().filter(reference -> !reference.isOverlapping()).toList();
  }

  private static final class Selection {

    private final Map<String, Selection> children = new LinkedHashMap<>();

    private static Selection ofKeys(TableMetadata table) {
      Selection selection = new Selection();
      selection.addKeys(table);
      return selection;
    }

    private void addKeys(TableMetadata table) {
      for (Column key : table.getPrimaryKeyColumns()) {
        Selection child = children.computeIfAbsent(key.getName(), name -> new Selection());
        if (key.isReference()) {
          child.addKeys(key.getRefTable());
        }
      }
    }

    private void addPath(TableMetadata table, List<String> path) {
      if (path.isEmpty()) {
        return;
      }
      Column column = table.getColumnByIdentifier(path.getFirst());
      if (column == null) {
        addAllColumns(table);
        return;
      }
      if (column.isFile() || column.isHeading()) {
        return;
      }
      Selection child = children.computeIfAbsent(column.getName(), name -> new Selection());
      if (column.isReference()) {
        child.addKeys(column.getRefTable());
        child.addPath(column.getRefTable(), path.subList(1, path.size()));
      }
    }

    private void addAllColumns(TableMetadata table) {
      for (Column column : table.getColumns()) {
        if (column.isFile() || column.isHeading() || column.isRefback()) {
          continue;
        }
        Selection child = children.computeIfAbsent(column.getName(), name -> new Selection());
        if (column.isReference()) {
          child.addKeys(column.getRefTable());
        }
      }
    }

    private SelectColumn[] toSelectColumns() {
      return children.entrySet().stream()
          .map(entry -> s(entry.getKey(), entry.getValue().toSelectColumns()))
          .toArray(SelectColumn[]::new);
    }
  }
}
