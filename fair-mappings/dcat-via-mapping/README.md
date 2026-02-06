# DCAT via Mapping

Export MOLGENIS catalogue data as DCAT metadata using the MappingStep declarative YAML DSL.

## What This Does

Queries MOLGENIS catalogue tables and transforms the data to DCAT RDF using a declarative mapping language instead of JSLT transformations.

**Pipeline:**
1. GraphQL query fetches Resources and Organisations
2. YAML mapping declares how fields map to DCAT predicates
3. Output serialized as Turtle (or other RDF formats)

**Benefits over JSLT approach:**
- More readable and maintainable
- No manual JSON-LD construction
- Declarative predicate mappings
- Built-in RDF serialization

## Quick Start

**Access via HTTP:**
```bash
curl -H "Accept: text/turtle" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

**Get as JSON-LD:**
```bash
curl -H "Accept: application/ld+json" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

**Run via CLI:**
```bash
java -jar fairmapper.jar run dcat-via-mapping export-dcat \
  --schema catalogue
```

## Files

| File | Purpose |
|------|---------|
| `fairmapper.yaml` | Bundle definition with route and steps |
| `src/get-all.gql` | GraphQL query for Resources and Organisations |
| `src/export-mapping.yaml` | YAML DSL mapping to DCAT predicates |

## Mapping Language Highlights

The `export-mapping.yaml` uses a declarative approach:

```yaml
_prefixes:
  dcat: http://www.w3.org/ns/dcat#
  dct: http://purl.org/dc/terms/

Resources:
  _id: "=baseUrl + '/' + schema + '/resource/' + id"
  _type: |
    =type?.find(t => t.name === 'Catalogue') ? 'dcat:Catalog' :
     type?.find(t => t.name === 'Databank') ? 'dcat:Dataset' :
     'dcat:Distribution'
  name: dct:title
  description: dct:description
  publisher:
    predicate: dct:publisher
    value:
      "@id": "=baseUrl + '/' + schema + '/organisation/' + publisher?.id"
      "@type": foaf:Organization
      "foaf:name": "=publisher?.name"
```

**Key features:**
- Simple column-to-predicate mappings (`name: dct:title`)
- Expressions for dynamic URIs (`_id: "=baseUrl + '/resource/' + id"`)
- Conditional types based on data
- Nested objects for complex structures
- Array iteration with `foreach`

See [Mapping Language documentation](../../docs/fairmapper/mapping-language.md) for full syntax.

## Example Output

```turtle
<http://localhost:8080/catalogue/resource/123> a dcat:Dataset ;
  dct:title "My Dataset" ;
  dct:description "Dataset description" ;
  dcat:keyword "genomics", "health" ;
  dct:publisher [
    a foaf:Organization ;
    foaf:name "Example Institute"
  ] ;
  dcat:contactPoint <http://localhost:8080/catalogue/contact/John-Doe> .

<http://localhost:8080/catalogue/contact/John-Doe> a vcard:Kind ;
  vcard:fn "John Doe" ;
  vcard:hasEmail <mailto:john@example.org> .
```

## Mapping Language vs JSLT

| Feature | Mapping Language | JSLT |
|---------|------------------|------|
| Syntax | Declarative YAML | Functional transforms |
| RDF output | Native (prefixes, types) | Manual JSON-LD construction |
| Learning curve | Lower for RDF experts | Lower for JSON experts |
| Flexibility | Structured patterns | Full programmability |

The Mapping Language is ideal when:
- Output is RDF/JSON-LD
- Mapping follows standard patterns (table → type)
- You want compact, readable definitions

JSLT is better when:
- Complex JSON restructuring needed
- Output is plain JSON
- Conditional logic is complex

Choose JSLT when you need fine-grained control or complex transformations. Choose Mapping DSL for straightforward field-to-predicate mappings.
