# FAIRmapper MVP Review - Python Fan Data Manager

**Reviewer perspective:** Python/pandas daily user, knows SQL/JSON, avoids Java

## 1. Documentation Clarity: 3.5/5

**What works:**
- Getting Started is excellent - step-by-step, builds up complexity gradually
- Schema Reference is comprehensive - table format is perfect
- Troubleshooting covers real errors with actual solutions
- Examples are copy-pasteable

**What's confusing:**
- "Bundle" terminology unclear initially - expected "project" or "pipeline"
- "Frame" requires JSON-LD knowledge, docs say "see JSON-LD spec" but don't explain WHY
- `endpoints` vs `mappings` confusion - beacon uses endpoints, others use mappings

**Missing:**
- When to use FAIRmapper vs Python script
- Performance expectations
- Debugging workflow

## 2. YAML Syntax Feedback

**Clear and intuitive:**
```yaml
steps:
  - fetch: ${SOURCE_URL}
  - transform: src/transforms/convert.jslt
  - mutate: src/mutations/save.gql
```

**Confusing parts:**
1. **`output-rdf: turtle`** - What is this? Should be `serialize: turtle` or `rdf: turtle`
2. **`frame:` parameter** - Buried in fetch step, not obvious it's not separate
3. **Test nesting inconsistency** - Sometimes on steps, sometimes on mappings
4. **Variable syntax** - `${SOURCE_URL}` vs `{schema}` - two different syntaxes

**Suggestions:**
- Rename `output-rdf` to `serialize` or `format-output`
- Consider `fetch-rdf:` to be explicit
- Unify substitution syntax

## 3. JSLT vs Python

**Where JSLT is readable:**
```jslt
{
  "id": extract-id(get-key(., "@id")),
  "name": get-key(., "dcterms:title")
}
```

**Where JSLT is painful:**

1. **`get-key()` everywhere** - In Python: `data["@id"]`, in JSLT: `get-key(., "@id")` every time
2. **Array normalization boilerplate** - Need helper function every transform
3. **No null-safe navigation** - Python: `data.get("publisher", {}).get("name")`, JSLT needs nested ifs
4. **Variable scoping** - `$` prefix inconsistent with function calls

**What would help:**
- Built-in `ensure-array()` function
- Built-in `safe-get(obj, "key", default)`
- More "Python equivalent" comments in examples

## 4. Top 3 Syntax Simplifications

**1. Auto-normalize arrays in JSON-LD context**
```jslt
// Current
let datasets = if (is-array($datasets_raw)) $datasets_raw else if ($datasets_raw) [$datasets_raw] else []

// Proposed - built-in
let datasets = get-key-array(., "dcat:dataset")
```

**2. Simplify JSON-LD key access**
Add `@context` awareness or auto-inherit from fetch frame step.

**3. Make step types more explicit**
```yaml
# Current
- output-rdf: turtle

# Proposed
- type: serialize
  format: turtle
```

## 5. Overall Impression: Would I Adopt This?

**YES, conditionally.**

**Strong positives:**
- Tool I can use without writing Java
- Testing built into pipeline is brilliant
- Dry-run mode saves hours of debugging
- YAML config is maintainable, can version control it

**Concerns:**
- JSLT learning curve (2-3 days stumbling)
- JSON-LD framing complexity
- Error messages quality unknown
- Performance unknowns

**Decision criteria:**
- 1-2 API integrations: **Yes, adopt immediately**
- 10+ integrations: **Adopt, but budget time for JSLT mastery**
- Non-JSON formats or heavy computation: **Stick with Python**

**Bottom line:** This is 80% of what I need. Willing to adopt and contribute feedback.
