# FAIRmapper MVP Review Summary

**Date:** 2026-01-27
**Reviewers:** 3 Data Managers + 2 Code Reviewers

## Consensus on `output-rdf` vs `rdf`

**All 5 reviewers agree:** Simplify `output-rdf` → `rdf`

| Reviewer | Quote |
|----------|-------|
| Python DM | "Should be `serialize` or `rdf: turtle`" |
| SQL DM | (implicit - suggested SQL-like verbs) |
| Bioinformatician | Focused on content, no objection |
| Java Expert | N/A (code focus) |
| Polyglot | "Shorter, clearer, consistent... no ambiguity" |

**Recommendation:** Change `output-rdf:` → `rdf:` in next release.

## Documentation Scores

| Reviewer | Score | Main Issue |
|----------|-------|------------|
| Python DM | 3.5/5 | "Bundle" term unclear, JSON-LD frames not explained |
| SQL DM | 3/5 | Missing schema diagrams, "no code" claim misleading |
| Bioinformatician | N/A | Missing FAIR explanation, no ontology guidance |

## Top Recurring Themes

### 1. JSLT Pain Points (3/5 reviewers)
- `get-key()` verbosity for JSON-LD keys
- No null-safe navigation
- Array normalization boilerplate repeated everywhere
- **Suggestion:** Add built-in `ensure-array()`, `safe-get()`

### 2. Syntax Inconsistencies (3/5 reviewers)
- `endpoints:` vs `mappings:` in bundles
- Variable syntax: `${VAR}` vs `$var`
- Tests nested vs separate
- **Action:** Migrate beacon-v2 to `mappings:`, unify variable syntax

### 3. Missing Tests (2/5 reviewers)
- Java: Commands have 0 tests (ValidateCommand, TestCommand, RunCommand)
- Polyglot: dcat-fdp has test files but no `tests:` blocks in YAML
- **Action:** Add command integration tests, wire test files to CLI

### 4. Error Handling (2/5 reviewers)
- Java: FairMapperException is unchecked, hides error paths
- Java: Silent partial failures in FrameDrivenFetcher
- **Action:** Create exception hierarchy, return partial failure info

### 5. FAIR Gaps (1/5 reviewers)
- Missing `dcat:distribution` - can't access actual data
- No SHACL validation
- Hardcoded timestamps/licenses
- Beacon v2 incomplete (diseases always null)
- **Action:** Add mandatory DCAT fields, consider SHACL step

## Adoption Likelihood

| Reviewer | Would Adopt? |
|----------|--------------|
| Python DM | Yes, "80% of what I need" |
| SQL DM | Maybe, 6/10 "functional but not delightful" |
| Bioinformatician | Yes with reservations, needs DCAT completeness |

## Quick Wins (< 1 day each)

1. ✅ **Rename `output-rdf` → `rdf`** - All reviewers agree
2. ✅ **Migrate beacon-v2 to `mappings:`** - Show best practice
3. ✅ **Add test blocks to dcat-fdp YAML** - Wire existing test files
4. ✅ **Create `shared/array-helpers.jslt`** - Reduce boilerplate
5. ✅ **Create ObjectMapperFactory** - 1 hour fix

## Medium-term Improvements

1. **Extract shared FDP context** - 48 lines duplicated
2. **Add command integration tests** - Critical gap
3. **Fix exception hierarchy** - FairMapperValidationException (checked)
4. **Add JSLT-Python cheatsheet** - Reduce learning curve
5. **Fix hardcoded timestamps** - Query from database

## Long-term Considerations

1. **Declarative field mapping** (Phase 7.4) - Multiple reviewers want this
2. **SHACL validation step** - For production FDP publishing
3. **SQL transforms via DuckDB** - SQL DM suggestion
4. **JSON-LD compaction step** - Simplify get-key() verbosity

## Files

- `dm-python-fan.md` - Python-focused data manager
- `dm-sql-expert.md` - SQL/dbt-focused data manager
- `dm-bioinformatician.md` - FAIR data expert
- `code-java-expert.md` - Java code review
- `code-polyglot.md` - YAML/JSLT/GraphQL/JSON-LD review
