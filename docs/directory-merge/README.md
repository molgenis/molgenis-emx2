# BBMRI Directory → Data Catalogue

A one-shot migration of the BBMRI-ERIC Directory (biobanks + their collections) into the MOLGENIS data catalogue — the first step toward a federated, multi-catalogue future. The core model change: separate organisation **identity** (a global `Organisations` table, one row per real-world org) from **attribution** (a per-resource `Organisation roles` table), make a biobank a `Biobanks` subtype, and give every dataset a legal **holder** (`held by`).

## Start here

- **[column-map.md](column-map.md) — the mapping manual.** The single reference: goal + requirements, where every directory table/column/row lands, the model change, RDF, and what needs curation. **Everyone starts here.**
- **[mapping_ledger.csv](mapping_ledger.csv) — every directory record, case by case** (29,013 rows). Each row's target table/id, the rule applied, and its `disposition` (`auto` / `needs_curation` / `dropped`). Filter `disposition = needs_curation` for the worklist, or `from_UMCG = TRUE` to fix at your own source.

## Proof-of-concept status — what is proven

- **Regression-free model change** — loads, export→import round-trips, stays **DCAT-AP & HealthRI-SHACL-clean** with existing **and** directory data; **zero regressions**, 9 profiles / 91 tests green.
- **Whole directory dump migrated** deterministically: **849 biobanks, 4954 collections, 19,581 facts** → structural validation **0 violations**; every collection gets a holder; ambiguous cases surfaced for curation.
- **A curated directory slice loads + round-trips + is SHACL-clean** in the catalogue demo.
- **Conclusion:** ~60% of the hard structural decisions automate cleanly; the rest are surfaced for curation. The approach is validated.
