# Merging the Directory — Proposed Catalogue Model Changes

> Section two of four. Human readable document. Requirements: `requirements.md`.

## The one idea

Today an organisation is a **per-resource role row**: named the `Organisations` table it extends the per-resource `Agents` role table and is keyed `(resource, id)`, so the *same* real-world org is re-declared once per resource it touches. Change: we **separate identity from attribution**:

- a **global `Organisations` identity** table extends Resource (one row per real-world org), and
- a renamed **`Organisation roles`** attribution table (the reified "org X did Y on resource Z").

Everything else follows from that split.

## Current (relevant slice)

```
Resources → { Networks → Catalogues, Collections }
Agents            polymorphic person|organisation role, keyed per-resource
Organisations     extends Agents  → per-resource, keyed (resource, id)   ← the problem
CatalogueOntologies.Organisations   ROR code-list  ← 3-way name collision
Resources.publisher / creator / organisations involved → Organisations (per-resource role rows)
```

## Proposed changes

| # | Change                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C1 | **New `Organisations extends Resources`** = top level identity (`foaf:Agent`). Own key (name/pid); `ror id` reference; inherits `source`/provenance; address/contact. A **biobank is a `Biobanks extends Organisations` row** — a real subtype that shares the Resources PK; the 3 *capability* columns live on `Biobanks`, the identity fields on the `Organisations` base, and refs to `Organisations` polymorphically accept `Biobanks`. *(2026-07-06: promoted from optional → adopted, for directory-user recognisability — reverses the earlier "no Biobanks subtype required" line.)*                                                                               |
| C2 | **Rename current `Organisations` table → `Organisation roles`** (per-resource attribution). Its `organisation` link repoints from the ROR ontology to the **new `Organisations` identity**. Since `Agents` is discontinued (C9), it becomes a standalone table holding the columns it needs (`resource`, `organisation`, `role`, and a `department`/org-unit text so informal departments live on the role, not as separate top level identities).          |
| C3 | **Rename the ROR ontology** `CatalogueOntologies.Organisations → ROR`. It becomes an external-identifier reference on the identity table (`Organisations.ror id → ROR`); keep the ROR **id** as the key but give its label a human-readable name (curator-aligned to our org names) for data-entry UX. Ends the name collision.                                                                                                                          |
| C4 | **Repoint the formal agent columns to the top level organisation identity** — `Resources.publisher`, `creator`, `organisations involved`, `Endpoint.publisher`, `Contacts.organisation` → new `Organisations`. (Fixes today's oddity of `publisher` pointing at a per-resource role row.)                                                                                                                                                                |
| C5 | **New `Collections.held by`** (required, ref_array → `Organisations`) = custody/controller — the directory's `Collection.biobank`. A **DCAT agent property** (`dcterms:rightsHolder`) that refs the `Organisations` identity (a `foaf:Agent`, so RDF serialises cleanly), alongside `publisher`/`creator` but distinct (GDPR controller + provenance). Enforces R3.                                                                                      |
| C6 | **Prune the `Organisation roles` role ontology**: delete control/identity values now carried by dedicated links (custodian/holder/publisher/creator); keep only narrative "what did this partner do" roles. **Principle: dedicated links for control + formal DCAT properties; the role table only for contribution.**                                                                                                                                   |
| C7 | **Networks & Catalogues stay Resource containers** (option 2) — a **`Networks` is *not* an `Organisations`**; it is a grouping **managed by** an `Organisations` (its existing `publisher` → Organisations). A directory Network → a `Networks` container **plus** the `Organisations` row for the coordinating body that manages it. A **national node → an `Organisations` identity used as the record's `source`** (the federation *owner/steward*, an Agent — not a content-grouping, so not a `Networks`).                                                                                                                                                                                                   |
| C8 | **Federation**: add `source` **+ `imported from`** + a namespaced identifier scheme on `Resources` (R5/R6). **`source`** = the record's **original** owner (R5) — global id = `source:localid`, **never rewritten** on merge (enables link-back to origin). **`imported from`** (now in scope) = the **immediate** upstream this catalogue imported the record from (R6). For the directory migration: `source` = the national node; `imported from` = the **BBMRI-ERIC Directory** (marks records that came from this one-shot migration). Both are `ref → Organisations`. (The multi-hop source *chain* stays future — see completion-roadmap §8.) |
| C9 | **Discontinue the `Agents` table.** It is one polymorphic person-or-org role table; its jobs are fully absorbed by `Contacts` (person-in-a-role — already has role, orcid, names) and `Organisation roles` (org-in-a-role). `Endpoint.publisher`/`contact` repoint to `Organisations` identity / `Contacts`. Migration: `Agents(type=Individual)` → `Contacts`; `Agents(type=Organisation)` → `Organisation roles` + minted identity. See column map §0. |
| C10 | **Upgrade `juridical_person` → an `Organisations` identity.** Biobank/Network `juridical_person` is free text — 500 distinct legal entities across 849 biobanks, 85% distinct from the biobank name. Mint legal-entity `Organisations`; link via `Organisations.part of` (self-`ref`); curator dedups near-duplicates. There is **no `parent organisation` ref** in the Directory — this replaces it.                                                    |
| C11 | **Add `Collection facts` [NEW]** (in scope). Dimensioned aggregate (sex × age × sample-type × disease + sample/donor measures), grounded in the 19,581-row export; receives the majority of `parent_collection` sub-collections. New table, sibling of `Resource counts`/`Subpopulation counts` (not an extension — different grain). Design: column map §E. |
| C12 | **Emit `prov:qualifiedAttribution`** (in scope). Serialise `Organisation roles` as reified attribution (row = `prov:Attribution`, `.organisation`=`prov:agent`→identity, `.role`=`dcat:hadRole`); the dedicated links (`publisher`/`creator`/`held by`) as flat DC triples. Generator enhancement (`PROV` ns already declared; not emitted today). Needs role-ontology terms to carry URIs. See §RDF serialisation. |
| C13 | **Add the 12 MIABIS-v3 gap columns** (in scope, R10). `Organisations`: *infrastructural* / *organisational* / *bioprocessing-&-analytical capabilities*. `Collections`: *sample source*, *sex*, *age low/high unit*, *storage temperature*, *sample collection setting*. `Networks`: *status*, *common collaboration topics*. (materials → existing `biospecimen collected`; data_categories → existing `areas of information` — not new.) Audit: column map §J. |

## Phase-1 implementation reconciliation (2026-07-05)

> The table above is the design intent. Phase 1 **implemented** C1–C11 + C13 (C12 deferred) and revealed these
> refinements — read alongside the C-rows.

- **C1 key.** `Organisations extends Resources` → **primary key = the inherited `Resources.id`**, not `name`. `name` is a *required secondary unique* key (human-readable + dedup). All refs (`publisher`/`creator`/`held by`/`part of`/`Organisation roles.organisation`) resolve by `id`. ("Own key (name/pid)" was imprecise.)
- **C3 ROR key.** EMX2 ontology tables are keyed by `name`. Implemented as: ROR keyed by `name` (human-readable org name), ROR **id in `code`**. The *intent* (human label + ROR id both retained) holds; "keep the ROR id as the key" is not literally how EMX2 keys ontologies.
- **C3 ripple (broader than "Main risk").** Renaming the ontology `Organisations→ROR` also broke `refTable=Organisations` on `Processes`/`Materials`/`Individuals` (Patient-registry / FAIR-Genomes profiles) — **outside the 9 catalogue profiles**. Any column referencing the ROR ontology must be swept, not just the 9.
- **C4.** `Resources.publisher`/`creator` → identity (by `id`). **`organisations involved` → `Organisation roles`** (a refback needs the child's `resource` FK, which only the roles table has) — NOT the identity directly. Only publisher/creator/held by point at the identity.
- **C5 held by = SOFT-required (Option C).** Implemented **not** model-`required`. R3 ("every Collection ≥1 holder") is enforced by the catalogue **data** (demo = 97/97), not a hard constraint — because `PatientRegistry` inherits `DataCatalogueFlat` and has orphan Collections (0 orgs), so required-everywhere is unloadable. Hard-enforce = trivial later follow-up. Migration rule: **held by derives from the old custody ("Data holder") role, not `publisher`** (R7 lossless) — publisher is only the fallback.
- **C12 DEFERRED** out of Phase 1 (was "in scope"). RDF `qualifiedAttribution`/`hadRole` + role URIs = a separate follow-up; the two SHACL checks (`ejp-rd-vp`, `hri-v2.0.2`) stay a clean regression guard. `foaf:name` was added to `Resources.name` (needed for the FDP OrgShape) — so it now applies to **all** Resources subtypes (no EMX2 per-subtype override); accepted tradeoff, both SHACL profiles still pass.
- **DataServices `[NEW]`** (column-map §A/§H) was **not** built in Phase 1 (directory-facing) — Phase 2. New tables actually built: `Collection facts`, `Services`, `Quality info` (→ catalogue 27→30 tables).
- **C8 / R5 `source id`.** Only `source` (→Organisations) was added; **`Resources.id` doubles as the local id** (`source:id` uses existing `id`). Column-map §C/§D "(+ Resources.source id ✎)" is stale — no separate column.
- **Beacon coupling (new).** The C8 `source`/`publisher`→Organisations→ROR path made ROR beacon-reachable; `ROR.country` (nested ontology→Countries) broke beacon query-gen. Fixed by making beacon `QueryBuilder` type-skip nested ontology subcolumns — **`ROR.country` stays an ontology** (owner-confirmed 2026-07-05), model NOT degraded.
- **`Linkages.relationship type`** implemented as **string** for now (no ontology exists yet); the MIABIS-Dataset-Types ontology (2026-07-02) is a Phase-2 follow-up when directory Linkages populate it.

## Directory record mapping (essentials)

- **Biobank** → a `Biobanks` (extends `Organisations`) row; its datasets → `Collections` **held by** it.
- **National node** → an `Organisations` identity used as the record's `source`. (`source` is the record's owning/stewarding node — an Agent role, so it must ref an `Organisations` (`foaf:Agent`), not a `Networks` grouping.)
- A "biobank" that is really *just its one dataset* → drop the org, keep a `Collection` **held by** its parent organisation.
- **Study** → a typed `Collections` (a MIABIS *Research resource* — may span **multiple collections and multiple biobanks**) + `Linkages` (`wasDerivedFrom`, **many** — one per source collection, not one-to-one). See column map §G.
- **Nested sub-collections** → `CollectionFacts` (default) / `Collection events` / `Subpopulations` / a promoted `Collection` — **human-in-the-loop where ambiguous** (a key thing the PoC measures).
- Every resulting `Collection` gets ≥1 `held by` (R3).

## Migrating existing catalogue data

Each current per-resource `Organisations` row becomes an `Organisation roles` row **and** folds (dedup by name/ROR) into one minted top level `Organisations` identity. Automated (R2).

## Main risk (what the PoC validates)

The `Organisations → Organisation roles` rename + identity split touches **`Organisations` referenced by name across nine profiles** (DataCatalogueFlat, EMA, and 7 staging) plus every `publisher`/`creator` ref. The PoC proves: (1) the rename ripple, (2) the dedup migration of existing orgs, (3) that a directory slice loads with holders enforced.

## RDF serialisation (the agent story in DCAT/PROV)

Two coexisting paths, both resolving the agent to the `Organisations` **identity** (`foaf:Agent`) — which is what makes them RDF-clean:

- **Dedicated formal links** — `publisher` / `creator` / `held by` → *flat* DC/DCAT triples (`dcterms:publisher` / `dcterms:creator` / `dcterms:rightsHolder`) pointing straight at the identity.
- **`Organisation roles`** (the soft contribution roles left after the C6 prune) → *reified* **`prov:qualifiedAttribution`**: the row **is** a `prov:Attribution` node — `.organisation` = `prov:agent` (→ identity), `.role` = `dcat:hadRole`, on `.resource`.

This is the canonical DCAT-AP 3 / HealthDCAT-AP shape, and it only works because the split provides one stable Agent node for `prov:agent` (before, "organisation" was a per-resource `Agents` row or the ROR ontology — no clean agent to attribute to).

**Status:** the `PROV` namespace is already declared in `molgenis-emx2-rdf` (`DefaultNamespace.java`), but `qualifiedAttribution`/`hadRole` are emitted **zero** times today — the current generator flattens agents (copied-from-ROR `foaf:name`/`dcterms:identifier`; `Agents.role` has no semantics). So emitting `qualifiedAttribution` is a **generator enhancement the new model enables**, not a regression. Dependency: the pruned role-ontology terms need URIs (a `dcat:Role`/SKOS concept each) for `dcat:hadRole`.

## Deferred / open

- **`Biobanks` subtype** — **adopted** (2026-07-06, C1): a real `Biobanks extends Organisations` subtype so directory users recognise a "Biobanks" table. Reverses the earlier "not needed now" position.
- **Diamond / multiple inheritance** — later; not required here.
- **`creator` = the dataset's creator** (DCAT `dcat:Dataset`), *not* the record's — the record steward is `source` (R9). Keep distinct.
