# Full-surface fixture

Exercises every attribute of the YAML model format against a CSV twin (`molgenis.csv`).
`Emx2YamlTest#fullSurfaceCsvParity` asserts the YAML bundle parses to the same
`SchemaMetadata` as the CSV twin; `#exportFidelity` and `#fileConvergence` assert the
model survives parse/export round-trips.

The `molgenis.csv` twin authors table-meta and column rows in the same order as the YAML
weaves them, so column positions match. Ontology refs are cross-schema (dotted
`CatalogueOntologies.Keywords`) so neither format auto-creates a same-schema ontology
table, keeping the two sides comparable.

Attributes that have no CSV column (document-layer only) are covered by fidelity and
convergence instead of parity: bundle-root `namespaces`, per-column `previousNames`,
schema/table/column `settings`.

## Attribute checklist

Bundle root:
- [x] formatVersion
- [x] version
- [x] namespaces (CURIE prefix map — YAML only, document layer)
- [x] settings (schema-level — not in CSV parity)
- [x] tables (file entries)

Table level:
- [x] name / label / label@nl / description / description@nl
- [x] semantics (table-level)
- [x] profiles (table-level)
- [x] settings (table-level — not in CSV parity)
- [x] subclasses (single parent: Cohorts, Networks)
- [x] subclasses multi-parent / diamond (CohortNetwork extends Cohorts + Networks)
- [x] modules (QuantitativeInfo, QualitativeInfo)

Column level:
- [x] key composite (id=1, orgCode=2, regionCode=2)
- [x] type variants: email, bool, int, text, enum, enum_array, ref, refback,
      ontology_array, module_array
- [x] values (enum / enum_array / module_array)
- [x] defaultValue
- [x] required literal (true) and required expression (`sex != null`)
- [x] readonly
- [x] computed
- [x] visible expression
- [x] validation
- [x] formLabel
- [x] semantics (column-level)
- [x] profiles (column-level)
- [x] label / label@nl and description / description@nl (i18n)
- [x] refTable, refLabel, refLink, refBack
- [x] refTable cross-schema dotted (ontology_array keywords)
- [x] previousNames (ordered rename chain — YAML only, document layer)

Form structure:
- [x] section heading (identity)
- [x] heading inside a module (quantHeading)
- [x] subclass-marked columns (cohortType, networkScope, jointField)
- [x] module-marked columns (sampleCount, narrative)
