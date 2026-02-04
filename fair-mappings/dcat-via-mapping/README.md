# DCAT via Mapping

Export MOLGENIS Resources as DCAT using the declarative Mapping Language.

## Quick Start

```bash
curl -H "Accept: text/turtle" \
  http://localhost:8080/catalogue/api/fair/dcat-via-mapping/dcat
```

## How It Works

This bundle demonstrates the `mapping` step type, which uses a declarative YAML-based language instead of JSLT transforms.

**fairmapper.yaml:**
```yaml
name: dcat-via-mapping
version: 1.0.0

mappings:
  - name: export-dcat
    route: dcat
    methods: [GET]
    output: turtle
    steps:
      - query: src/get-all.gql
      - mapping: src/export-mapping.yaml
```

**Pipeline:**
1. `query` - Fetch Resources and Organisations from MOLGENIS
2. `mapping` - Transform to DCAT RDF using declarative mapping

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

## Mapping Example

From `src/export-mapping.yaml`:

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

See [Mapping Language Reference](../../docs/fairmapper/mapping-language.md) for full documentation.
