# BBMRI Directory → Data Catalogue

A one-shot migration of the BBMRI-ERIC Directory (biobanks + their collections) into the MOLGENIS data catalogue, done as the first step toward a federated, multi-catalogue future. The core idea: separate organisation **identity** (a global `Organisations` table, one row per real-world org) from **attribution** (a per-resource `Organisation roles` table), and give every dataset a legal **holder** (`held by`). Everything else in the change follows from that split.

## Why (the requirements)

See [requirements.md](requirements.md). The change is driven by:

- **R3** — every Collection has ≥1 legal holder organisation (not enforced today).
- **R4** — organisations become global identities: one org row referenced by many resources, not re-declared per resource.
- **R5 / R6** — federation-ready: every record carries a `source` (owning catalogue) + a stable local id; global identity is namespaced (`source:id`) and a merging instance never rewrites another's ids.
- **R10** — the target model is MIABIS Core v3 feature-complete: every MIABIS v3 attribute has a home.

## What changes (the model)

See [proposed-changes.md](proposed-changes.md). In one line: split organisation **identity** (`Organisations extends Resources`) from **attribution** (`Organisation roles`), add new tables `Collection facts`, `Services` and `Quality info`, rename the ROR code-list ontology `Organisations → ROR`, and add `Collections.held by` (custody/rights-holder). The formal DCAT agent columns (`publisher`/`creator`) and the new `held by` all resolve to the one stable `Organisations` identity, which is what makes the RDF serialisation clean.

## How the Directory maps onto the catalogue
- **Biobank → an `Organisations` identity** that **holds** its datasets — optionally a **`Biobanks extends Organisations`** subtype where biobank-operation attributes warrant it (not required now; the base identity carries id/provenance and links to `Services`/`Quality info`).
- **Collection → a `Collections` row, `held by`** that org (custody). A 1:1 biobank that is really just its one dataset collapses to a single `Collections` held by its legal entity (no org minted).
- **Nested sub-collections (`parent_collection`)** are re-expressed by intent → **`Collection facts`** (default aggregate), **`Collection events`** (timepoints), **`Subpopulations`** (site arms), or a promoted **`Collections`** — human-in-the-loop where ambiguous.
- **CollectionFacts → `Collection facts`** (the dimensioned sex × age × sample-type × disease aggregate).
- **`juridical_person` (free text) → minted legal-entity `Organisations`**, linked via **`part of`** (self-ref).
- **Persons → `Contacts`**, **Networks → a `Networks` container + an `Organisations` coordinating body**, **Studies → typed `Collections` + `Linkages`**, **QualityInfo → `Quality info`**, **Services → `Services`**, **national nodes → `Organisations` identities** (used as `source`).

Full per-column detail: see `column-map.md`; the mapping rationale is in `proposed-changes.md`.

## Proof-of-concept status — what is proven

- **The model change is regression-free.** The model loads, export→import round-trips, and stays **DCAT-AP & HealthRI-SHACL-clean** with existing **and** directory data — **zero regressions** across 9 profiles / 91 tests green.
- **The whole directory dump migrated** under deterministic rules into the new model: **849 biobanks, 4954 collections, 19,581 facts** → structural validation **0 violations**. Auto-vs-curation is quantified:
  - biobank shapes: **374 auto / 475 flagged** for the 1:1 "is-it-a-real-org?" call;
  - `juridical_person`: **500 distinct → 512 legal-entity identities, 31 fuzzy pairs** to confirm;
  - `parent_collection`: **92 auto → Collection facts / 114 flagged**;
  - every collection gets a holder.
- **A curated directory slice loads + round-trips + is SHACL-clean** in the catalogue demo.
- **Conclusion:** ~60% of the hard structural decisions automate cleanly; the rest are surfaced for curation. The approach is validated.

## What remains

See [completion-roadmap.md](completion-roadmap.md). The model is proven; the remaining work is reference-data (ontology) + curation + full-scale execution — **not** model changes.

## Doc index

- [requirements.md](requirements.md) — why, and what must be true.
- [proposed-changes.md](proposed-changes.md) — the model changes (C1–C13) + Phase-1 implementation reconciliation.
- [column-map.md](column-map.md) — field-by-field Directory → catalogue mapping (data-manager view).
- [analysis.md](analysis.md) — data analysis & migration risk, measured against the real dumps.
- [completion-roadmap.md](completion-roadmap.md) — what is needed to go from PoC to production.
