# GraphQL _groupBy Capabilities Analysis

## 1. _groupBy Field Definition (GraphqlTableFieldFactory.java)

Location: `/backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlTableFieldFactory.java`

### Method: tableGroupByField() - Lines 119-135

```java
public GraphQLFieldDefinition tableGroupByField(TableMetadata table) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(table.getIdentifier() + "_groupBy")
        .type(GraphQLList.list(createTableGroupByType(table)))
        .dataFetcher(fetcherForTableQueryField(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.FILTER_ARGUMENT)
                .type(getTableFilterInputType(table))
                .build())
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.SEARCH)
                .type(Scalars.GraphQLString)
                .build())
        .build();
}
```

### Key Finding: _groupBy has ONLY 2 arguments:
1. **filter** - supports filtering on specific values (e.g., `{ name: { equals: ["cat","dog"] } }`)
2. **search** - full-text search

**_groupBy does NOT support:**
- `limit` (parameter not defined, line 134)
- `offset` (parameter not defined, line 134)
- `orderby` (parameter not defined, line 134)

### Compare with tableQueryField() - Lines 81-117
Regular table queries support: filter, search, limit, offset, orderby

---

## 2. _groupBy Return Type (createTableGroupByType) - Lines 310-353

The _groupBy response returns an object with:
- **count** (Int) - line 322: the count for this group
- **sum** (conditional) - for numeric columns (lines 327-338)
- All non-reference columns (lines 340-349)
- Reference columns as nested objects (lines 340-349)

Example response structure:
```json
{
  "_groupBy": [
    { "name": "cat", "count": 15 },
    { "name": "dog", "count": 8 },
    { "name": "bird", "count": 3 }
  ]
}
```

---

## 3. Filter Support in _groupBy

**YES - Can filter on specific values**

The dataFetcher (lines 858-889) processes filters the same way for both regular queries and _groupBy queries:

```java
if (dataFetchingEnvironment.getArgument(GraphqlConstants.FILTER_ARGUMENT) != null) {
    q.where(convertMapToFilterArray(...));
}
```

This means you CAN do:
```graphql
query {
  myTable_groupBy(filter: { name: { equals: ["cat", "dog"] } }) {
    name
    count
  }
}
```

To get counts ONLY for those 20 specific names.

---

## 4. Fetcher Implementation - Lines 858-889

The `fetcherForTableQueryField()` dataFetcher:

1. Detects which operation by field name (line 862-866):
   - If ends with `_groupBy` → `q = table.groupBy()`
   - If ends with `_agg` → `q = table.agg()`
   - Otherwise → regular query

2. Applies arguments in this order:
   - select (line 869)
   - where/filter (lines 871-876)
   - limit (lines 877-878) ← SUPPORTED BY CODE
   - offset (lines 880-881) ← SUPPORTED BY CODE
   - orderby (lines 883-884) ← SUPPORTED BY CODE

**DISCREPANCY FOUND**: The backend dataFetcher CAN handle limit/offset/orderby for _groupBy (lines 877-885), but the GraphQL field definition doesn't expose these parameters (line 119-135).

---

## 5. Frontend: fetchTableData.ts - Lines 1-65

Current implementation:
- Queries regular table data with limit/offset/orderby
- Also fetches count via `{tableId}_agg` query
- Returns: `{ rows: [...], count: number }`

**Does NOT use _groupBy for option counting** - currently uses direct filtering approach

---

## RECOMMENDATIONS

### Option 1: Add limit/offset/orderby to _groupBy (Backend Change Required)
- Modify `tableGroupByField()` to add 3 parameters (like tableQueryField)
- This is technically already supported by dataFetcher - just needs GraphQL schema exposure
- Change: Add `.argument()` calls for limit/offset/orderby in lines 124-133

### Option 2: Use _groupBy without pagination (Works Now)
- Can immediately use: `{ filter: { name: { equals: [...20 names...] } } }`
- Cannot paginate results (no limit/offset)
- Good for: pre-filtered dropdowns, categorical counts

### Option 3: Keep current approach with dedicated filter query
- Build separate filtering endpoint
- More flexibility, but more implementation work
