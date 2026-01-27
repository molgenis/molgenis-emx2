# GraphQL Fragments with Depth Levels

## Problem
Current `AllFields` fragments only expand refs to key fields (depth 1).
Users often need deeper nested data without manually writing complex queries.

## Proposal
Add numbered fragment variants indicating expansion depth:

| Fragment | Refs expand to |
|----------|----------------|
| `PetAllFields` | Key fields only (current, same as depth 1) |
| `PetAllFields1` | Key fields only (explicit depth 1) |
| `PetAllFields2` | All fields → then key fields |
| `PetAllFields3` | All fields → all fields → key fields |

## Example Output

### PetAllFields1 (depth 1)
```graphql
fragment PetAllFields1 on Pet {
  name
  weight
  category { name }  # key only
  tags { name }      # key only
}
```

### PetAllFields2 (depth 2)
```graphql
fragment PetAllFields2 on Pet {
  name
  weight
  category {
    name
    description
    parent { name }  # key only at depth 2
  }
  tags {
    name
    color
  }
}
```

## Decisions
1. Max depth: **3**
2. Keep `PetAllFields` as alias for depth 1: **yes** (backward compat)
3. Circular refs: **depth limit only** (simple, predictable)
4. Generate depths 1-3 always for all tables
