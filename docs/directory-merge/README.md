# BBMRI Directory → Data Catalogue

A one-shot migration of the BBMRI-ERIC Directory (biobanks + their collections) into the MOLGENIS data catalogue — the first step toward a federated, multi-catalogue future. The core model change: separate organisation **identity** (a global `Organisations` table, one row per real-world org) from **attribution** (a per-resource `Organisation roles` table), make a biobank a `Biobanks` subtype, and give every dataset a legal **holder** (`held by`).

## Start here — the docs

- **[column-map.md](column-map.md) — THE column map + row-mapping rules.** How every directory table, column and row becomes catalogue rows: Biobanks triage, Collections & Studies, sub-collection re-expression. **Data managers start here.**
- **[proposed-changes.md](proposed-changes.md) — the model change.** The C1–C13 changes + Phase-1 implementation reconciliation.
- **[requirements.md](requirements.md) — why / what must be true.** R1–R10.
- **[analysis.md](analysis.md) — data analysis & migration risk.** Measured over the real dumps: where automation stops and curation starts.
- **[completion-roadmap.md](completion-roadmap.md) — PoC → production.** Ontology extension, curation, full-scale execution, federation model.

## Proof-of-concept status — what is proven

- **Regression-free model change** — loads, export→import round-trips, stays **DCAT-AP & HealthRI-SHACL-clean** with existing **and** directory data; **zero regressions**, 9 profiles / 91 tests green.
- **Whole directory dump migrated** deterministically: **849 biobanks, 4954 collections, 19,581 facts** → structural validation **0 violations**; every collection gets a holder; ambiguous cases surfaced for curation.
- **A curated directory slice loads + round-trips + is SHACL-clean** in the catalogue demo.
- **Conclusion:** ~60% of the hard structural decisions automate cleanly; the rest are surfaced for curation. The approach is validated.
