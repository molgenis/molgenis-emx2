# Implement MappingStep for FAIRmapper

## Task
Implement `MappingStep.java` and `MappingEngine.java` for the FAIRmapper pipeline. This step transforms MOLGENIS GraphQL query results to JSON-LD using a declarative YAML mapping.

## Key Files to Read First
- `.plan/specs/fairmapper.md` - Full spec (search for "MappingStep Engine Implementation")
- `docs/fairmapper/mapping-language.md` - Language reference
- `fair-mappings/dcat-via-mapping/src/export-mapping.yaml` - Example mapping
- `fair-mappings/dcat-via-mapping/src/queries/get-all.gql` - Example query
- Look at existing steps for patterns: `TransformStep.java`, `QueryStep.java`, `SparqlConstructStep.java`

## Reuse Existing Code
- `JavaScriptUtils.executeJavascriptOnMap(script, values)` - GraalVM JS evaluation (in `molgenis-emx2/utils`)
- SnakeYAML for parsing mapping files
- Jackson for JSON handling

## Classes to Create

### 1. MappingStep.java
Location: `backend/molgenis-emx2-fairmapper/src/main/java/org/molgenis/emx2/fairmapper/step/`

```java
public class MappingStep implements PipelineStep {
    private final Path mappingFile;

    @Override
    public JsonNode execute(JsonNode input, PipelineContext context) {
        MappingEngine engine = new MappingEngine(mappingFile, context);
        return engine.transform(input);
    }
}
```

### 2. MappingEngine.java
Core logic:

```java
public class MappingEngine {
    public JsonNode transform(JsonNode input) {
        // 1. Parse mapping YAML
        // 2. Build base scope: _request, _params, settings, _schema
        // 3. Evaluate _context expressions
        // 4. For each table in mapping:
        //    - Get rows from input
        //    - For each row: build scope, evaluate _id, _type, columns
        //    - Handle foreach for arrays
        // 5. Wrap in @graph + @context from _prefixes
        // 6. Return JSON-LD
    }
}
```

### 3. MappingScope.java
Variable scope chain:

```java
public class MappingScope {
    private final Map<String, Object> variables;
    private final MappingScope parent;

    public MappingScope child() { ... }
    public void put(String name, Object value) { ... }
    public Map<String, Object> flatten() { ... }  // For JS evaluation
}
```

## Mapping Language Summary

### Structure
```yaml
_prefixes:
  dcat: http://www.w3.org/ns/dcat#

_context:
  baseUrl: "=_request.baseUrl"

TableName:
  _let:
    resourceUrl: "=baseUrl + '/resource/' + id"
  _id: "=resourceUrl"
  _type: dcat:Dataset
  name: dct:title
  contacts:
    predicate: dcat:contactPoint
    foreach: c in contacts
    value:
      "@id": "=resourceUrl + '/contact/' + c.firstName"
      "@type": vcard:Kind
```

### Expression Rules
- `=` prefix → JavaScript expression
- No prefix → literal value
- Scope chain: _request/_params/settings/_schema → _context → _let → row fields → foreach var

### Injected Variables
| Variable | Source |
|----------|--------|
| `_request` | `{baseUrl, schema, path}` from HTTP context |
| `_params` | fairmapper.yaml `parameters:` (env vars resolved) |
| `settings` | Schema settings converted from `[{key,value}]` to object |
| `_schema` | From query result |

### foreach Parsing
Pattern: `varName in arrayField`
```java
Pattern FOREACH = Pattern.compile("(\\w+)\\s+in\\s+(\\w+)");
```

## Output Format
JSON-LD with @graph:
```json
{
  "@context": {
    "dcat": "http://www.w3.org/ns/dcat#",
    "dct": "http://purl.org/dc/terms/"
  },
  "@graph": [
    {"@id": "...", "@type": "dcat:Dataset", "dct:title": "..."}
  ]
}
```

## Test Cases
Create in `backend/molgenis-emx2-fairmapper/src/test/`:
1. Simple mapping (column: predicate)
2. Expression evaluation
3. Nested _let scopes
4. foreach array iteration
5. Null handling (skip null @id)

## Safety Limits
- Max 10,000 rows per table
- 1 second timeout per expression
- Max 10 levels nesting

## Don't Forget
- Stage changes with `git add` before completing
- Follow existing patterns in codebase
- No comments in code
