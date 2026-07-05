# Requirements - Merging the BBMRI Directory into the Data Catalogue

> Colleague-facing summary of *why* and *what must be true*. The *how* is in `proposed-changes.md`.

## Goal

Do a **one-shot migration** of the BBMRI-ERIC Directory (biobanks + their collections) into the MOLGENIS data catalogue, after which the Directory codebase and app are to be **retired**. Do it in a way that is also a **first step toward a federated, multi-catalogue future** (many catalogue instances merging records into one or more central servers while each keeps maintaining its own records).

## Scope

The deliverable is (a) a **model change** so the catalogue can host directory-shaped records, plus (b) a **migration** that loads them. It is **not** an ongoing directory sync — the directory goes away.

## Requirements

| # | Requirement                                                                                                                                                                                                                                                                                                                            |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 | Model change + one-shot migration. No ongoing directory sync.                                                                                                                                                                                                                                                                          |
| R2 | **Existing catalogue data migrates automatically** to the changed model. Directory records migrate automatically where deterministic; **manual curation is allowed** for genuinely ambiguous cases.                                                                                                                                    |
| R3 | **Every dataset (Collection) has ≥1 legal holder organisation** (custody/controller). Which is not enforced today.                                                                                                                                                                                                                     |
| R4 | **Organisations are global identities** — one organisation row can be referenced by many resources, not re-declared per resource (as is now today). Prerequisite for R3/R5/R6.                                                                                                                                                         |
| R5 | **Federation-ready identification**: every record carries a `source` (owning catalogue) + a stable local id; global identity is namespaced (`source:id`); a merging instance **never rewrites another's ids**. Enables a central catalogue that lists/searches records but links back to the source. |
| R6 | **Provenance**: every record carries `source` / imported-from so merged records stay attributable to their origin.                                                                                                                                                                                                                     |
| R7 | **Migration  might be lossy only where loss is safe**: drop empty/duplicate biobank↔collection records (the 1:1 collapse); re-express nested sub-collections as facts / collection events / subpopulations / promoted collections. **Automated re-expression must be lossless**; human-in-the-loop is allowed where it can't be.       |
| R8 | Directory app + code retired after migration.                                                                                                                                                                                                                                                                                          |
| R9 | **End-users manage their own records** (submit/edit), not import-only. A record is stewarded by its `source` — distinct from the dataset's `creator`/`publisher` (DCAT separates the `CatalogRecord` from the `Dataset`). Users submit a Resource + attachments (today as documents; later a formalised EMX2 'parts' refback).         |
| R10 | **Target model is MIABIS Core v3 feature-complete** — every MIABIS v3 attribute (across Biobank / Collection / Research resource / Network) has a home (an existing column, a linked table, or a net-new column), even where the Directory doesn't populate it. The migration is then a *superset-ready step toward MIABIS v3*, not a regression. Audit: column map §J. |

## Non-goals (explicitly out)

- **Ongoing directory→catalogue sync** — one-shot only.
- **The SEO / central-listing serving mechanism** (canonical URLs pointing to source catalogues, avoiding duplicate-content penalties) — a serving-layer concern, out of the *model* scope. Flagged because R5 enables it.
- **Diamond / multiple inheritance** — kept in mind for later; nothing in this change depends on it.

## Success criteria

1. Catalogue schema loads; **existing catalogue data migrates and round-trips**.
2. A representative directory slice loads: biobanks → top level Organisations; collections → Collections **held by** an organisation; nested sub-collections re-expressed as collection facts, subpopulations and/or collection events.
3. Every migrated Collection has ≥1 holder (R3).
4. The **ambiguous cases are surfaced** (which sub-collections can't be migrated to solutions in (2)/ which "is this a real org?" calls need a human) — a key output of the proof of concept.
