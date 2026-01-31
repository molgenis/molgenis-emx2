# JSON-LD Reference Flattening Plan

## Problem

JSON-LD export creates nested reference objects:
```json
{
  "name": "pooky",
  "category": {"name": "cat"},
  "tags": [{"name": "blue"}, {"name": "green"}]
}
```

But `table.save()` via `Row` doesn't resolve these nested objects to existing foreign key references. Error:
```
Key (category)=({name=cat}) is not present in table "Category"
```

## Goal

Enable true JSON-LD roundtrip: export → import without transformation.

## Current Flow

```
importJsonLd(table, jsonLdData)
  → stripJsonLdKeywords(row)      // removes @type, @id, etc.
  → new Row(cleanedMap)           // creates Row directly
  → table.save(rows)              // fails on nested refs
```

## Proposed Solution

Add `flattenReferences(table, rowMap)` that converts nested ref objects to simple key values.

### Implementation

1. **Detect ref columns** from `table.getMetadata().getColumns()` where `column.isReference()`

2. **For each ref column**, get the referenced table's primary key columns

3. **Flatten logic**:
   - Single ref: `{category: {name: "cat"}}` → `{category: "cat"}` (if single PK)
   - Single ref with composite PK: `{ref: {a: "x", b: "y"}}` → `{ref: {a: "x", b: "y"}}` (keep as-is, Row handles it)
   - Array ref: `{tags: [{name: "blue"}]}` → `{tags: ["blue"]}` (if single PK)
   - Array ref with composite PK: keep nested objects

4. **Edge cases**:
   - Null refs → keep null
   - Already flat refs (string/number) → keep as-is
   - Nested refs in nested refs → recursive flatten

### Code Location

Add to `RestOverGraphql.java`:

```java
private static Map<String, Object> flattenReferences(
    TableMetadata table, Map<String, Object> row) {

  Map<String, Object> result = new LinkedHashMap<>(row);

  for (Column col : table.getColumns()) {
    if (!col.isReference()) continue;

    String colId = col.getIdentifier();
    Object value = result.get(colId);
    if (value == null) continue;

    List<Column> refPkeys = col.getRefTable().getPrimaryKeyColumns();
    boolean singlePk = refPkeys.size() == 1;
    String pkName = singlePk ? refPkeys.get(0).getIdentifier() : null;

    if (value instanceof Map && singlePk) {
      // {category: {name: "cat"}} → {category: "cat"}
      result.put(colId, ((Map<?,?>) value).get(pkName));
    } else if (value instanceof List && singlePk) {
      // [{name: "blue"}] → ["blue"]
      List<Object> flat = new ArrayList<>();
      for (Object item : (List<?>) value) {
        if (item instanceof Map) {
          flat.add(((Map<?,?>) item).get(pkName));
        } else {
          flat.add(item);
        }
      }
      result.put(colId, flat);
    }
    // Composite PK: keep as-is, Row/TypeUtils handles it
  }
  return result;
}
```

### Update importJsonLd

```java
public static int importJsonLd(Table table, Map<String, Object> jsonLdData) {
  // ... existing extraction logic ...

  List<Row> cleanedRows = new ArrayList<>();
  for (Map<String, Object> row : rows) {
    Map<String, Object> cleaned = stripJsonLdKeywords(row);
    Map<String, Object> flattened = flattenReferences(table.getMetadata(), cleaned);
    cleanedRows.add(new Row(flattened));
  }
  return table.save(cleanedRows);
}
```

## Testing

Update `testImportJsonLd()` to test:
1. Pet with category ref (single PK)
2. Pet with tags ref_array (single PK)
3. Table with composite PK ref (if exists in pet store)

## Alternative: Use TypeUtils.convertToRows

Instead of flattening, could use existing `TypeUtils.convertToRows(tableMetadata, rowMaps)` which already handles nested refs via `addFieldObjectToRow()`. But that's in molgenis-emx2 core, would need to check if it handles all cases.

## Dependencies

None - self-contained change in RestOverGraphql.java

## Effort

Small - ~50 lines of code + tests
