# FAIRmapper MVP Review - Polyglot (YAML/JSLT/GraphQL/JSON-LD)

**Reviewer perspective:** Strong in DSL design, transforms, JSON-LD, maintainability

**Overall Grade: B+** (solid implementation, room for polish)

## 1. YAML Schema Design

**Strengths:**
- Consistent structure across all bundles
- Clear step types: `transform`, `query`, `mutate`, `fetch`, `output-rdf`
- Test integration inline with steps
- Backwards compatibility (endpoints → mappings)

**Issues:**

### 1.1 Inconsistent top-level keys
beacon-v2 uses `endpoints:`, dcat-* uses `mappings:`. Migrate beacon-v2 to show best practice.

### 1.2 `output-rdf` verbosity
**Current:** `output-rdf: turtle`
**Proposed:** `rdf: turtle`

The `output-` prefix adds no semantic value. Every final step "outputs" something.

### 1.3 Missing test declarations
dcat-fdp has JSLT transforms with test files but no `tests:` blocks in YAML - can't validate via `fairmapper test`.

### 1.4 Variable syntax inconsistency
`${SOURCE_URL}` (bash-style) vs `$id` (GraphQL-style). Pick one.

## 2. JSLT Transform Quality

**Strengths:**
- Modular design with `shared/ontology.jslt`, `shared/meta.jslt`
- Clear variable naming
- Header comments
- Consistent object construction

**Issues:**

### 2.1 Inconsistent null handling
Mix of explicit `else null` and implicit null returns. Document convention.

### 2.2 Repetitive boilerplate in dcat-fdp
48 lines of `@context` duplication across 3 transforms. Extract to `shared/fdp-context.jslt`.

### 2.3 Hardcoded metadata
`"@value": "2024-01-01T00:00:00Z"` in all dcat-fdp transforms. Should come from database or use current timestamp.

### 2.4 Magic publisher fallback
```jslt
else {
  "foaf:name": "MOLGENIS"
}
```
Hides missing data. Prefer `null` or validation error over magic defaults.

### 2.5 get-key() verbosity
Seven `get-key()` calls in 8 lines for JSON-LD. Not JSLT's fault - inherent to JSON-LD. Document pattern + consider JSON-LD compaction step.

### 2.6 Array normalization pattern
Same pattern repeated:
```jslt
if (is-array($val)) $val else if ($val) [$val] else []
```
Extract to `shared/array-helpers.jslt` as `ensure-array()`.

### 2.7 Missing null checks
```jslt
"id": .genderAtBirth.codesystem + ":" + .genderAtBirth.code
```
Assumes codesystem/code exist if genderAtBirth truthy. Add defensive checks.

## 3. GraphQL Quality

**Strengths:**
- Proper field selection (no over-fetching)
- Named queries for debugging
- Consistent formatting
- Schema introspection usage

**Issues:**
- `get-dataset.gql:1` declares `$baseUrl` but never uses it
- Missing `Individuals_agg { count }` for pagination metadata

## 4. JSON-LD Frame Correctness

**Strengths:**
- Correct framing syntax with `@embed: "@always"`
- Minimal context (only used prefixes)
- Selective field extraction

**Issues:**
- Frame extracts `dcterms:identifier` for datasets but not catalog
- No nested publisher embedding for datasets (DCAT spec allows different publishers)

## 5. Top 5 Simplification Recommendations

### 1. Rename `output-rdf` → `rdf` (HIGH IMPACT)
**Files:** spec, dcat-fdp/fairmapper.yaml:14,25,36, OutputRdfStep.java
**Rationale:** Shorter, clearer, consistent

### 2. Extract shared JSON-LD context (MEDIUM IMPACT)
**Current:** 48 lines duplicated
**Proposed:** `shared/fdp-context.jslt` imported by all three
**Impact:** 40 lines saved, single source of truth

### 3. Add `shared/array-helpers.jslt` (MEDIUM IMPACT)
```jslt
def ensure-array(val)
  if (is-array($val)) $val
  else if ($val) [$val]
  else []
```

### 4. Consolidate null handling convention (LOW IMPACT, HIGH CLARITY)
Document: shared functions always explicit `else null`, inline expressions implicit OK.

### 5. Remove hardcoded timestamps (MEDIUM IMPACT)
Query `_meta.createdOn`, `_meta.lastUpdated` from database.

## 6. Naming Suggestions

| Current | Clarity | Recommendation |
|---------|---------|----------------|
| `fetch` | ★★★★☆ | **Keep** |
| `transform` | ★★★★★ | **Keep** |
| `query` | ★★★★★ | **Keep** |
| `mutate` | ★★★★☆ | **Keep** |
| `output-rdf` | ★★★☆☆ | **Change to `rdf`** |

**Action:** Update beacon-v2 bundle to use `mappings` to show best practice.

## 7. What Will Confuse Maintainers in 6 Months

1. **JSON-LD array ambiguity** - Why is `dcat:dataset` sometimes array, sometimes not?
2. **get-key() everywhere** - Bracket notation fails on keys with colons
3. **Null vs missing fields** - `"diseases": null` - intentional or unimplemented?
4. **Why two catalog bundles?** - dcat-harvester (ingest) vs dcat-fdp (publish)
5. **Test files not wired to CLI** - dcat-fdp has test files but no `tests:` blocks
6. **Hardcoded dates** - Production FDP shows 2024-01-01 timestamps

## 8. Anti-Patterns to Watch

1. **Silent fallbacks hiding missing data** - `else { "foaf:name": "MOLGENIS" }`
2. **Mixing variable syntax** - `${SOURCE_URL}` vs `$id`
3. **Test files not wired to CLI** - If exists, wire to `fairmapper test` or delete
4. **Comments explaining code rather than context**
5. **Over-nested ternaries** - Extract to functions

## Summary

**Strengths:**
- Clear, learnable YAML schema
- Well-structured JSLT with good reuse patterns
- Proper GraphQL field selection
- Correct JSON-LD framing

**Weaknesses:**
- Some naming verbosity (`output-rdf`)
- Repetition in dcat-fdp transforms
- Inconsistent null handling
- JSON-LD complexity not documented

**Priority Actions:**
1. Simplify `output-rdf` → `rdf`
2. Document JSON-LD quirks
3. Add test declarations to dcat-fdp
4. Extract shared FDP context
5. Create array-helpers.jslt
6. Fix hardcoded timestamps
7. Migrate beacon-v2 to `mappings`
