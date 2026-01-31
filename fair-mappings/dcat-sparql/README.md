# dcat-sparql

Example bundle demonstrating SPARQL CONSTRUCT transforms (Phase 11 - planned).

## Structure

```
dcat-sparql/
├── fairmapper.yaml
├── harvest/           # External RDF → MOLGENIS
│   ├── to-molgenis.sparql
│   ├── output.jsonld
│   └── upsert.gql
└── export/            # MOLGENIS → DCAT RDF
    ├── get-all.sql
    └── to-dcat.sparql
```

## Mappings

| Name | Direction | Steps |
|------|-----------|-------|
| harvest | inbound | SPARQL → frame → mutate |
| export | outbound | SQL → SPARQL |
| harvest-auto | inbound | auto mode (uses schema semantics) |

## Status

Planned for Phase 11. Requires:
- `rdf4j-repository-sail`
- `rdf4j-sail-memory`
- `rdf4j-queryparser-sparql`

See `.plan/plans/fairmapper.md` Phase 11 for implementation details.
