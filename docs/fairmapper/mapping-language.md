# MOLGENIS Mapping Language

A declarative YAML-based language for mapping MOLGENIS data to RDF.

## Quick Start

### Simple Mapping

Map a MOLGENIS table to DCAT:

```yaml
_prefixes:
  dcat: http://www.w3.org/ns/dcat#
  dct: http://purl.org/dc/terms/

Resources:
  _id: "=baseUrl + '/resource/' + id"
  _type: dcat:Dataset
  name: dct:title
  description: dct:description
```

**Produces:**
```turtle
<http://example.org/resource/123> a dcat:Dataset ;
  dct:title "My Dataset" ;
  dct:description "A description" .
```

### Conditional Types

Map different MOLGENIS values to different RDF types:

```yaml
Resources:
  _type: |
    =type?.find(t => t.name === 'Catalogue') ? 'dcat:Catalog' :
     type?.find(t => t.name === 'Databank') ? 'dcat:Dataset' :
     'dcat:Distribution'
```

### Nested Objects

Create complex RDF structures:

```yaml
Resources:
  publisher:
    predicate: dct:publisher
    value:
      "@id": "=baseUrl + '/org/' + publisher?.id"
      "@type": foaf:Organization
      "foaf:name": "=publisher?.name"
```

**Produces:**
```turtle
<resource/123> dct:publisher [
  a foaf:Organization ;
  foaf:name "MOLGENIS"
] .
```

### Array Iteration with foreach

Create one RDF object per array item:

```yaml
Resources:
  contacts:
    predicate: dcat:contactPoint
    foreach: c in contacts
    value:
      "@id": "=contactBase + '/' + c.firstName + '-' + c.lastName"
      "@type": vcard:Kind
      "vcard:fn": "=c.firstName + ' ' + c.lastName"
      "vcard:hasEmail": "='mailto:' + c.email"
```

**Produces:**
```turtle
<resource/123> dcat:contactPoint <contact/John-Doe>, <contact/Jane-Smith> .

<contact/John-Doe> a vcard:Kind ;
  vcard:fn "John Doe" ;
  vcard:hasEmail <mailto:john@example.org> .

<contact/Jane-Smith> a vcard:Kind ;
  vcard:fn "Jane Smith" ;
  vcard:hasEmail <mailto:jane@example.org> .
```

### Scoped Variables

Define reusable expressions with `_let`:

```yaml
_context:
  baseUrl: "=_request.baseUrl"
  schema: "=_schema.name"

Resources:
  _let:
    resourceUrl: "=baseUrl + '/' + schema + '/resource/' + id"
  _id: "=resourceUrl"
  contacts:
    _let:
      contactBase: "=resourceUrl + '/contact'"
    foreach: c in contacts
    value:
      "@id": "=contactBase + '/' + c.firstName"
```

---

## Reference

### Document Structure

```yaml
_prefixes:     # Namespace prefix definitions
  prefix: uri

_context:      # Global variables (evaluated once)
  varName: "=expression"

_query:        # (Future) Query generation hints
  filter: ...
  include: [...]

TableName:     # One section per MOLGENIS table
  _let: ...    # Scoped variables
  _id: ...     # Subject URI
  _type: ...   # RDF type(s)
  column: ...  # Column mappings
```

### _prefixes

Define namespace prefixes for use in predicates and types.

```yaml
_prefixes:
  dcat: http://www.w3.org/ns/dcat#
  dct: http://purl.org/dc/terms/
  foaf: http://xmlns.com/foaf/0.1/
```

**Usage:** Once defined, use `prefix:localName` anywhere:
```yaml
name: dct:title
_type: dcat:Dataset
```

### _context

Define global variables available throughout the mapping.

```yaml
_context:
  baseUrl: "=_request.baseUrl"
  schema: "=_schema.name"
  contactEmail: "=_params.contactEmail || 'info@example.org'"
```

**Injected variables (available in all expressions):**

| Variable | Source | Description |
|----------|--------|-------------|
| `_request` | HTTP request | `baseUrl`, `schema`, `path` |
| `_params` | fairmapper.yaml | Bundle parameters (env vars resolved) |
| `settings` | Schema settings | Converted to object from key/value |
| `_schema` | GraphQL query | Schema metadata (name, etc.) |

**Scope:** `_context` variables are evaluated once and available everywhere.

### _let

Define scoped variables available in current block and children.

```yaml
Resources:
  _let:
    resourceUrl: "=baseUrl + '/' + schema + '/resource/' + id"
  _id: "=resourceUrl"

  contacts:
    _let:
      contactBase: "=resourceUrl + '/contact'"
    foreach: c in contacts
    value:
      "@id": "=contactBase + '/' + c.firstName"
```

**Scope:** Available in the block where defined and all nested blocks.

### _id

Define the subject URI for RDF triples.

```yaml
Resources:
  _id: "=baseUrl + '/' + schema + '/resource/' + id"
```

### _type

Define the RDF type(s) for the subject.

**Simple (single type):**
```yaml
Resources:
  _type: dcat:Dataset
```

**Conditional (expression):**
```yaml
Resources:
  _type: |
    =type?.find(t => t.name === 'Catalogue') ? 'dcat:Catalog' :
     type?.find(t => t.name === 'Databank') ? 'dcat:Dataset' :
     'dcat:Distribution'
```

### Column Mappings

#### Simple Mapping

Direct column-to-predicate mapping:

```yaml
name: dct:title
description: dct:description
website: dcat:landingPage
```

#### With Value Transform

Use `predicate` + `value` for transformations:

```yaml
email:
  predicate: vcard:hasEmail
  value: "='mailto:' + email"

theme:
  predicate: dcat:theme
  value:
    "@id": "=theme?.ontologyTermURI"
    "@type": skos:Concept
```

#### Nested Objects

Create complex RDF structures:

```yaml
publisher:
  predicate: dct:publisher
  value:
    "@id": "=baseUrl + '/org/' + publisher?.id"
    "@type": foaf:Organization
    "foaf:name": "=publisher?.name"
```

**Keys starting with `@` or containing `:` must be quoted.**

### foreach

Iterate over arrays, producing one RDF object per item.

**Syntax:**
```yaml
columnName:
  predicate: predicateUri
  foreach: varName in columnName
  value:
    "@id": "=expression using varName"
    "predicate": "=varName.field"
```

**Example:**
```yaml
contacts:
  predicate: dcat:contactPoint
  foreach: c in contacts
  value:
    "@id": "=contactBase + '/' + c.firstName + '-' + c.lastName"
    "@type": vcard:Kind
    "vcard:fn": "=c.firstName + ' ' + c.lastName"
```

**Scope:** The loop variable (`c`) is available only within the `value` block.

### Expressions

Expressions start with `=` and use JavaScript syntax (GraalVM).

**Available variables:**
| Context | Available Variables |
|---------|---------------------|
| Global | `_context` vars, `settings`, `_schema` |
| Table | Row fields (`id`, `name`, etc.) |
| `_let` block | Parent scope + defined vars |
| `foreach` | Parent scope + loop variable |

**Common patterns:**

```yaml
# String concatenation
"=baseUrl + '/resource/' + id"

# Null-safe access
"=publisher?.name"

# Conditional
"=type === 'A' ? 'uri:TypeA' : 'uri:TypeB'"

# Array mapping
"=items?.map(i => ({'@id': i.uri}))"

# Find in array
"=type?.find(t => t.name === 'X')"
```

---

## Complete Example

```yaml
_prefixes:
  dcat: http://www.w3.org/ns/dcat#
  dct: http://purl.org/dc/terms/
  foaf: http://xmlns.com/foaf/0.1/
  vcard: http://www.w3.org/2006/vcard/ns#
  skos: http://www.w3.org/2004/02/skos/core#

_context:
  baseUrl: "=_request.baseUrl"
  schema: "=_schema.name"

Resources:
  _let:
    resourceUrl: "=baseUrl + '/' + schema + '/resource/' + id"
  _id: "=resourceUrl"
  _type: |
    =type?.find(t => t.name === 'Catalogue') ? 'dcat:Catalog' :
     type?.find(t => t.name === 'Databank') ? 'dcat:Dataset' :
     'dcat:Distribution'
  name: dct:title
  description: dct:description
  keywords: dcat:keyword
  issued: dct:issued
  modified: dct:modified
  theme:
    predicate: dcat:theme
    foreach: t in theme
    value:
      "@id": "=t.ontologyTermURI"
      "@type": skos:Concept
  publisher:
    predicate: dct:publisher
    value:
      "@id": "=baseUrl + '/' + schema + '/org/' + publisher?.id"
      "@type": foaf:Organization
      "foaf:name": "=publisher?.name"
  contacts:
    _let:
      contactBase: "=baseUrl + '/' + schema + '/contact'"
    predicate: dcat:contactPoint
    foreach: c in contacts
    value:
      "@id": "=contactBase + '/' + c.firstName + '-' + c.lastName"
      "@type": vcard:Kind
      "vcard:fn": "=c.firstName + ' ' + c.lastName"
      "vcard:hasEmail": "='mailto:' + c.email"
  dataResources:
    predicate: dcat:dataset
    foreach: d in dataResources
    value:
      "@id": "=baseUrl + '/' + schema + '/resource/' + d.id"
```

---

## Implementation Notes

### Expression Evaluation

1. Parse YAML document
2. Evaluate `_context` expressions once (global scope)
3. For each table:
   - Evaluate `_let` expressions (table scope)
   - For each row from data source:
     - Bind row fields to scope
     - Evaluate `_id` and `_type`
     - For each column mapping:
       - If `foreach`: iterate array, bind loop var, evaluate `value` per item
       - Else: evaluate expression or use literal predicate
4. Collect all triples, serialize to requested RDF format

### Query Generation (Future)

Engine can infer GraphQL query from mapping:

1. Walk mapping tree
2. Collect referenced fields (`id`, `name`, `publisher.id`, etc.)
3. Build nested GraphQL selection
4. Apply `_query.filter` if specified
5. Include `_query.include` fields

### Scope Chain

```
_context
  └── _schema, settings
        └── Table _let
              └── Row fields (id, name, ...)
                    └── Column _let
                          └── foreach var (c, d, t, ...)
```

Inner scopes can access all outer scope variables. Shadowing allowed.
