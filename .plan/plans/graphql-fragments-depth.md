# GraphQL Fragments Depth - Plan

## Status: Implementation complete - ready for review

## Current Implementation
- Location: `GraphqlTableFieldFactory.java` lines 119-161
- `getGraphqlFragments(table, pkeyOnly)` - recursive with boolean
- Refs always expand to key fields only (pkeyOnly=true on recursion)

## Proposed Changes

### 1. Modify `getGraphqlFragments` signature
```java
// Old: getGraphqlFragments(TableMetadata table, boolean pkeyOnly)
// New: getGraphqlFragments(TableMetadata table, int depth, int maxDepth)
```

### 2. Recursion logic
- depth 0: key fields only (terminal)
- depth > 0: all fields, refs recurse with depth-1

### 3. Fragment registration
Generate fragments for depths 1, 2, 3:
- `PetAllFields` → alias for `PetAllFields1`
- `PetAllFields1` → depth 1 (current behavior)
- `PetAllFields2` → depth 2
- `PetAllFields3` → depth 3

### 4. Circular reference handling
Depth limit only - simple and predictable. At max depth, refs become key fields.

## Files to Modify
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlTableFieldFactory.java`
- `backend/molgenis-emx2-graphql/src/main/java/org/molgenis/emx2/graphql/GraphqlApi.java`
- `docs/molgenis/dev_graphql.md`

## Decisions Made
- Max depth: 3
- Keep `PetAllFields` as alias for `PetAllFields1`: yes
- Circular refs: depth limit only

## Completed
1. ✅ Implement depth parameter in GraphqlTableFieldFactory
2. ✅ Register fragments for depths 1-3 in GraphqlApi
3. ✅ Add tests for depth 2 and 3 (11 tests)
4. ✅ Nyx upgrade to 3.1.4 (worktree support)

## Next Steps
1. Update docs
2. Code review
