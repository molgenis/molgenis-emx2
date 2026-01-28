# FAIRmapper Troubleshooting

Common errors and their solutions.

## Setup Issues

### "Command not found: fairmapper"

**Problem**: Running `./fairmapper` gives "command not found" or "permission denied".

**Solution**: Make sure you're in the `fair-mappings/` directory:
```bash
cd fair-mappings
./fairmapper --help
```

Or use the full path from project root:
```bash
fair-mappings/fairmapper --help
```

### "Build failed" on First Run

**Problem**: First run fails during Gradle build.

**Solutions**:
1. Check Java version (requires Java 21+):
   ```bash
   java -version
   ```

2. Clear Gradle cache:
   ```bash
   ./gradlew clean
   ./fairmapper --help
   ```

3. Check for syntax errors in any `fairmapper.yaml` files in `fair-mappings/`.

## Bundle Validation Errors

### "Bundle not found"

**Problem**: `./fairmapper validate my-bundle` says bundle not found.

**Solution**: Bundle directory must be inside `fair-mappings/`:
```
fair-mappings/
└── my-bundle/
    └── fairmapper.yaml
```

Run from `fair-mappings/`:
```bash
./fairmapper validate my-bundle
```

### "Invalid YAML syntax"

**Problem**: YAML parsing fails.

**Common causes**:
- Tabs instead of spaces (YAML requires spaces)
- Missing colons after keys
- Incorrect indentation

**Debug**: Use a YAML validator or check with:
```bash
python -c "import yaml; yaml.safe_load(open('fairmapper.yaml'))"
```

### "Step file not found"

**Problem**: Validation fails with "file not found" for a step.

**Solution**: Paths are relative to the bundle directory. Check:
```yaml
steps:
  - transform: src/transforms/my-transform.jslt  # Must exist
```

Verify:
```bash
ls my-bundle/src/transforms/my-transform.jslt
```

## Fetch Step Issues

### "No data returned from fetch"

**Problem**: Fetch step returns empty or null.

**Causes**:
1. **URL returns no RDF**: Check the URL in a browser or with curl
2. **Frame doesn't match**: The frame filters by `@type` - if no resources match, result is empty

**Debug**:
```bash
# Fetch without frame to see raw data
./fairmapper fetch-rdf https://your-url --format json-ld

# Then try with frame
./fairmapper fetch-rdf https://your-url --frame my-bundle/frame.jsonld
```

### "Frame doesn't match expected structure"

**Problem**: Framed output is missing expected fields.

**Causes**:
1. **Wrong `@type`**: Frame filters to specific types. Check the source RDF for actual types.
2. **Missing `@embed`**: Related resources aren't included without `"@embed": "@always"`
3. **Wrong property URIs**: JSON-LD uses full URIs. Check your `@context` prefixes.

**Debug**: Fetch raw RDF and inspect:
```bash
./fairmapper fetch-rdf https://your-url --format turtle
```

Look for the actual type URIs and property names.

### "Connection refused" or "Host not found"

**Problem**: Network error when fetching.

**Solutions**:
1. Check URL is correct and accessible
2. Check network/firewall settings
3. Try with curl: `curl -v https://your-url`

## Transform (JSLT) Issues

### "get-key not defined"

**Problem**: JSLT error about undefined function.

**Solution**: `get-key` is a built-in JSLT function. This error usually means:
1. Typo in function name (case-sensitive)
2. Old JSLT version (update dependencies)

### "Bracket notation fails for JSON-LD keys"

**Problem**: `.["@id"]` or `."dcterms:title"` returns null or errors.

**Solution**: JSLT bracket notation doesn't work for keys with special characters. Use `get-key()`:
```jslt
// Wrong
.["@id"]
."dcterms:title"

// Correct
get-key(., "@id")
get-key(., "dcterms:title")
```

### "Transform output missing fields"

**Problem**: Expected fields are null or missing in output.

**Debug steps**:
1. Check input structure with `--show-data`:
   ```bash
   ./fairmapper run my-bundle mapping --source URL --dry-run --show-data
   ```

2. Test transform in isolation:
   ```bash
   echo '{"test": "data"}' | ./fairmapper transform my-bundle/transform.jslt
   ```

3. Add debug output in JSLT:
   ```jslt
   let $debug = get-key(., "dcterms:title")
   // Check $debug value in output
   ```

### "Array expected, got single value" (or vice versa)

**Problem**: RDF sometimes returns arrays, sometimes single values.

**Solution**: Normalize in your transform:
```jslt
def to-array(val)
  if (is-array($val)) $val
  else if ($val) [$val]
  else []

{
  "keywords": to-array(get-key(., "dcat:keyword"))
}
```

## Mutation Issues

### "GraphQL error: Field not found"

**Problem**: Mutation fails because field doesn't exist.

**Causes**:
1. **Schema mismatch**: Your transform output doesn't match the MOLGENIS schema
2. **Wrong field name**: GraphQL uses camelCase (`fdpEndpoint` not `fdp endpoint`)

**Debug**:
1. Check your schema fields:
   ```
   http://localhost:8080/yourSchema/api/graphql
   ```

2. Use dry-run to see what would be sent:
   ```bash
   ./fairmapper run my-bundle mapping --source URL --dry-run -v
   ```

### "Insert failed: Duplicate key"

**Problem**: Record with same ID already exists.

**Solution**: MOLGENIS `insert` mutation does upsert by default. Check:
1. Is the ID field correct?
2. Does your transform generate unique IDs?

### "Foreign key constraint violation"

**Problem**: Referenced record doesn't exist.

**Example error**:
```
Key (fdp endpoint)=(main_fdp) is not present in table "Endpoint"
```

**Causes**:
1. **Missing related record**: The referenced record must exist first
2. **Default value**: Schema may have a default that references non-existent record

**Solutions**:
1. Create the referenced record first
2. Set the field to null in your transform to override defaults:
   ```jslt
   {
     "id": "my-resource",
     "fdpEndpoint": null
   }
   ```

### "Transaction failed"

**Problem**: Mutation partially succeeded then rolled back.

**Cause**: MOLGENIS mutations are transactional. If any part fails, everything rolls back.

**Debug**: Check the error message for which specific constraint failed.

## E2e Test Issues

### "E2e test schema not found"

**Problem**: E2e tests fail because schema doesn't exist.

**Solution**: Ensure you have a running MOLGENIS with the test schema:
```yaml
e2e:
  schema: fairmapperTest  # Must exist
```

### "E2e output doesn't match"

**Problem**: E2e test fails with JSON comparison error.

**Debug**:
1. Run with verbose to see actual output:
   ```bash
   ./fairmapper e2e my-bundle -v
   ```

2. JSON comparison ignores key order but requires exact values. Check for:
   - Extra/missing fields
   - Type differences (string vs number)
   - Null vs missing

## CLI Issues

### "Unknown option"

**Problem**: CLI doesn't recognize an option.

**Solution**: Check exact option syntax:
```bash
./fairmapper run --help
```

Common mistakes:
- `--verbose` should be `-v`
- `--dryrun` should be `--dry-run`

### "Missing required option"

**Problem**: Required option not provided.

**Solution**: For `run` command, required options depend on mode:

Dry run (no server needed):
```bash
./fairmapper run bundle mapping --source URL --dry-run
```

Full run (server required):
```bash
./fairmapper run bundle mapping \
  --source URL \
  --server http://localhost:8080 \
  --schema mySchema \
  --token myToken
```

## Performance Issues

### "Out of memory"

**Problem**: FAIRmapper crashes with OutOfMemoryError.

**Causes**:
1. Fetching too much RDF data
2. `maxCalls` set too high

**Solutions**:
1. Reduce `maxCalls` in fetch step:
   ```yaml
   - fetch: ${SOURCE_URL}
     maxCalls: 20
   ```

2. Reduce `maxDepth`:
   ```yaml
   - fetch: ${SOURCE_URL}
     maxDepth: 1
   ```

3. Increase Java heap (if needed):
   ```bash
   JAVA_OPTS="-Xmx2g" ./fairmapper run ...
   ```

### "Fetch takes too long"

**Problem**: Fetch step is slow.

**Causes**:
1. Following too many links
2. Slow source server

**Solutions**:
1. Reduce `maxDepth` and `maxCalls`
2. Use a more specific frame that filters early
3. Cache fetched data for testing:
   ```bash
   ./fairmapper fetch-rdf URL --frame frame.jsonld > cached.json
   ```

## Getting Help

If your issue isn't covered here:

1. Run with verbose mode (`-v`) to see detailed output
2. Check the bundle examples in `fair-mappings/dcat-fdp/`
3. Review the [Schema Reference](schema_reference.md)
