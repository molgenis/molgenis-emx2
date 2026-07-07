# Directory → Catalogue — Migration Guide

**What this is.** The data manager's reference for where every BBMRI-ERIC Directory table, row and column lands in the catalogue — and what is automatic vs. what needs curation afterward. The plan is **load-then-curate**: migrate everything automatically first (it loads clean), then curate the flagged judgement-calls inside the catalogue, per source.

Legend: **⎇** choose target (rule given) · **✎** new column · **⌫** drop (derived/system, no info loss) · **[NEW]** net-new table. Relations are EMX2 `ref` / `refback`: a `ref` / `ref_array` column sits on the *child* pointing at the parent; the parent sees it back as a `refback`. The **MIABIS v3** column in the tables below names the MIABIS (Minimum Information About BIobank data Sharing) Core v3 attribute each field preserves.

> **Already reconciled with the Phase-1 build:** identity **key = `id`** (not `name`); `held by` is soft-required and migrated from the old **custody role** (not `publisher`); there is **no separate `source id`** column (`id` is the local id); `organisations involved` → `Organisation roles`; `DataServices` is deferred; `Linkages.relationship type` is a string for now.

## Goal & requirements

**Goal.** A **one-shot** migration of the BBMRI-ERIC Directory (biobanks + collections) into the catalogue, after which the Directory app is **retired** — done so it is also the first step toward a **federated, multi-catalogue** future (many catalogues merging records into central servers, each still maintaining its own). Not an ongoing sync.

| # | Requirement                                                                                                                                                             |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 | Model change + one-shot migration; no ongoing directory sync.                                                                                                           |
| R2 | Existing catalogue data migrates automatically; directory records migrate automatically where deterministic, curation allowed afterwards for genuinely ambiguous cases. |
| R3 | Every Collection has ≥1 legal **holder** organisation (custody/controller).                                                                                             |
| R4 | Organisations are **global identities** — one row referenced by many resources, not re-declared per resource. Prerequisite for R3/R5/R6.                                |
| R5 | Federation-ready ids: every record carries a `source` + a stable local id; global id = `source:id`; a merging instance **never rewrites another's ids**.                |
| R6 | Provenance: every record carries `source` / `imported from` so merged records stay attributable to their origin.                                                        |
| R7 | Lossy only where safe: collapse empty/duplicate 1:1 biobank↔collection; re-express nested sub-collections losslessly; human-in-the-loop where automation can't.         |
| R8 | Directory app + code retired after migration.                                                                                                                           |
| R9 | End-users manage their own records (submit/edit); a record is stewarded by its `source`, distinct from the dataset's `creator`/`publisher`.                             |
| R10 | Target model is **MIABIS Core v3 feature-complete** (every v3 attribute has a home). Audit: §J.                                                                         |

## The mapping ledger

Every one of the 29,013 Directory records is mapped case-by-case in **`mapping_ledger.csv`**. Headline: **94% map automatically, 3% need curation, 3% are dropped** (orphan persons with no resource to attach to). Each row gives the source record (`directory_table` / `id` / `name`), where it lands (`catalogue_table` / `id`), the rule applied (`mapping_rule`), its `disposition` — `auto`, `needs_curation`, or `dropped` — and `flag_reason` (why a human is needed, e.g. the 1:1 "is-it-a-real-org?" triage). A separate, **orthogonal** `data_quality_error` flag (with `data_quality_reason`; ~1,084 rows: a person in `juridical_person`, placeholder text, malformed IDs, bad geo-coords) marks bad *values* — it fires **independently of `disposition`**, so many `auto` rows still carry it. Biobank-fold hints (`collections_fold_review` / `name_coherence` / `part_of_possible_facts_fold`) advise where a multi-collection biobank's collections are really facts slices.

**Two worklists, both needed:** (1) `disposition = needs_curation` — structural judgement-calls; (2) `data_quality_error = TRUE` — bad values to clean. To work only your own records, filter `source = <your national node>` (`from_UMCG` is UMCG-specific demo scaffolding). All fixes are made **in the catalogue after migration**, not in the retired Directory.

**Where the human effort concentrates.** Measured over the real dumps, ~60% of the hard structural decisions automate cleanly. Curation focuses on two things: (a) **organisation / legal-entity dedup** (fuzzy name-matching — the single biggest automated-migration correctness risk, the 871→319 existing-catalogue collapse plus the ~500 minted `juridical_person` entities), and (b) the **~118 of 206 `parent_collection` parents** whose sub-collections aren't a clean facts-fold. Everything else is deterministic.

## Curation — your worklist and how to action it

Curation happens **inside the catalogue after the load**, in the normal record-editing UI — each national node works only its own records (`source = your node`). There is no re-import from the (retired) Directory. The one task without a plain-UI path today is **merging duplicate `Organisations`** (dedup), which needs a merge tool that re-points references — planned, not yet built.

| Worklist | Count | Find it by | Action |
|---|---|---|---|
| Biobank "is it a real org?" (1:1) | ~475 | `flag_reason` = is-it-a-real-org? | confirm **collapse** to a `Collections` vs **mint** a `Biobanks` |
| Legal-entity dedup | ~31 fuzzy pairs (of ~500 minted + the 871→319 existing-org collapse) | near-duplicate `Organisations` names | **merge** duplicates (merge tool) |
| Ambiguous sub-collections | 220 rows, across ~118 of 206 parent groups | `curation status = unresolved-subcollection` | **fold** into `Collection facts` / `Subpopulations` / `Collection events` |
| Data-quality | ~1,084 | `data_quality_error = TRUE` | **clean** bad values (placeholder text, bad geo-coords, person in `juridical_person`) |

The "220 rows vs ~118 parents" are two views of the same work: 220 ambiguous *child* rows span ~118 of the 206 *parent* collections. Un-curated records stay loaded and visible; set `withdrawn` / `mg_draft` if a record must be hidden until fixed.

## A. Table-level map

### A.1 Complete directory dump → catalogue (all 22 dump tables)

Every table in the directory dump has a target; nothing is dropped silently.

| Directory table                                 | → Catalogue | Note |
|-------------------------------------------------|---|---|
| Biobanks                                        | `Biobanks` (extends `Organisations`) | subtype; 1:1 triage; `juridical_person` → `part of` legal-entity `Organisations` |
| Collections(*)                                  | `Collections` (+ `held by`) | `parent_collection` → facts/events/subpopulations/'promoted' |
| CollectionFacts                                 | `Collection facts` | dimensioned aggregate |
| Studies                                         | `Collections` (typed) + `Linkages` | MIABIS *Research resource*; `wasDerivedFrom` — many source collections/biobanks |
| Persons                                         | `Contacts` | |
| Networks                                        | `Networks` + coordinating (host) `Organisations` | |
| NationalNodes                                   | → `Organisations` (national-node identity) via `source` | node = an owning/stewarding org, not a grouping |
| ContactPersonsNationalNodes                     | `Contacts` | 0 rows |
| Organisations                                   | `Organisations` (identity) | |
| Publishers                                      | `Organisations` (publisher identity) | `Resources.publisher` → it |
| Address                                         | inline on `Organisations` (`address`) | no separate table |
| Services                                        | `Services` (extends `Resources`) | `provider` → `Organisations` |
| DataServices                                    | `DataServices` — DEFERRED (not built in Phase 1) | 0 rows |
| QualityInfoBiobanks                             | `Quality info` (`resource` → biobank) | **3 → 1 polymorphic** |
| QualityInfoCollections                          | `Quality info` (`resource` → collection) | |
| QualityInfoServices                             | `Quality info` (`resource` → service) | |
| Catalogs                                        | `Catalogues` | |
| Endpoints                                       | `Endpoint` | FDP (FAIR Data Point) |
| AlsoKnownIn                                     | `External identifiers` | alt-ids |
| molgenis / molgenis_members / molgenis_settings | — (schema / permissions / UI settings) | system metadata, dropped |

(*) **Collections are triaged** (see §D): **~83%** are auto-classified — a top-level `Collections`, or folded into `Collection facts` / `Subpopulations` / `Collection events`. The **~17%** ambiguous tail lands as a temporary `Collections`, flagged `curation status = unresolved-subcollection` and linked to its parent via `Linkages`, then folded during curation.

**`Quality info` applies to any `Resource` (`resource → Resources`), so the directory's three quality tables (Biobanks/Collections/Services) collapse into one — removing that duplication.**

### A.2 Touched catalogue tables — what changes

Only the tables below change; every other catalogue table is untouched. Each change is listed individually.

**Table changes**

- `Organisations` — **replaced**: now `extends Resources`; becomes the top-level legal-entity identity (`foaf:Agent`). Fed by directory Organisations, Publishers, Networks' host body, and `juridical_person`.
- `Biobanks` — **[NEW]** `extends Organisations`: biobank identity + capabilities. Fed by directory Biobanks.
- `Organisation roles` — **renamed** from the old `Organisations` table: per-resource attribution (`prov:qualifiedAttribution`).
- `ROR` (Research Organization Registry) — **renamed** from the old `Organisations` ontology: external-id reference.
- `Agents` — **discontinued**: split into `Contacts` (persons) + `Organisation roles` (orgs).
- `Collections` — **+ new columns** (below), incl. `held by`. Fed by directory Collections + Studies.
- `Collection facts` — **[NEW]**: dimensioned aggregate. Fed by CollectionFacts + folded sub-collections.
- `Quality info` — **[NEW]**: one polymorphic quality table (`resource → Resources`) replacing the directory's three (Biobanks/Collections/Services).
- `Services` — **[NEW]** `extends Resources`: biobank services (`provider → Organisations`).
- `DataServices` — **[NEW]** (deferred; 0 rows in Phase 1).
- `Linkages` — **+ typed**: gains `relationship type` + `source selection` (collection relationships, `wasDerivedFrom`).
- `Contacts` — reused, **+ `phone`**. Fed by directory Persons + head/contact.
- `Networks`, `Catalogues` — **unchanged** containers, now each managed by an `Organisations` (a Network is not itself an Organisation).
- `External identifiers`, `Endpoint` — reused (alt-ids / FDP endpoints).

**New columns** (each listed)

- `Collections`: `held by`, `sex`, `age low unit`, `age high unit`, `storage temperatures`, `body part`, `imaging modality`, `number of samples`, `sample source`, `sample collection setting`.
- `Organisations`: `part of`, `email`, `phone`, + capabilities (infrastructural / organisational / bioprocessing-&-analytical).
- `Networks`: `status`, `common collaboration topics`, `member organisations` (proposed — `ref_array → Organisations`, for biobank/org membership; see Open decisions #8).
- `Resources` (inherited by all subtypes): `source`, `imported from`, `location`, `latitude`, `longitude`, `last data refresh`, `withdrawn`. (`source` = original owner = the national node; `imported from` = immediate upstream = the Directory; both `ref → Organisations`.)
- `Linkages`: `relationship type`, `source selection`.

*(No new column needed for materials → existing `biospecimen collected`; data_categories → existing `areas of information`.)*

---

## B. Identity vs attribution — discontinue `Agents` → `Contacts` + `Organisation roles`

**The core model change:** separate an organisation's **identity** (one global `Organisations` row per real-world org) from its **attribution** (a per-resource role — "org X did role Y on resource Z"). The old per-resource `Organisations` table is renamed to **`Organisation roles`** (attribution); a **new `Organisations` identity table** is created — so 'attribution' is no longer confused with 'identity'. Everything else follows from that split. This rename touches `Organisations` referenced by name across **nine profiles** + every `publisher`/`creator` ref — the main migration risk the PoC validates.

`Agents` (one polymorphic per-resource person|org role table) is **discontinued**; nothing is lost:

| current `Agents` column | new home | note |
|---|---|---|
| `resource` | `Organisation roles.resource` (org) / `Contacts.resource` (person) — both `ref → Resources` | the per-resource link stays on the role side |
| `type` (person/org) | — | ⌫ the two typed tables *are* the discriminator |
| `name` (individual) | `Contacts.first/last/display name` | already exist |
| `organisation` (→ ROR ontology) | `Organisation roles.organisation` `ref →` **new `Organisations` identity** | identity carries `ror id` `ref → ROR` |
| `other organisation`, `department` | `Organisation roles.department`/org-unit (text) | informal units stay on the role, not the top level identity |
| `website`/`email`/`logo` | org → `Organisations`; person → `Contacts.homepage`/`email`/`photo` | already exist |
| `role` (→ Organisation roles ontology) | `Organisation roles.role` / `Contacts.role` (→ Contribution types) | separate role ontologies already exist |
| `organisation name`/`pid`/`website` (copied-from-ROR) | — | ⌫ derived; RDF reads from the identity row |

**EMX2 shape after the split:**
- **`Organisations`** (top level identity) `extends Resources`; primary key = the inherited `Resources.id`, with `name` a required secondary-unique (+`ror id` `ref → ROR`). Refbacks: `Resources.organisations involved` (its `Organisation roles`) and `Organisations.holds` (from `Collections.held by`); plus the forward self-`ref` `Organisations.part of` (legal entity).
- **`Organisation roles`**: `resource` (`ref → Resources`), `organisation` (`ref → Organisations`), `role` (ontology, pruned), `department`/org-unit (text).
- **Repoint** (were `→ Agents`): `Endpoint.publisher`, `Endpoint.contact`, and the identity refs `Resources.publisher`/`creator`/`organisations involved`, `Contacts.organisation` → new `Organisations`.

**Existing-data migration:** each per-resource `Organisations`/`Agents(type=org)` row → an `Organisation roles` row **+** folds (dedup by name/ROR) into one minted `Organisations` identity (871 → ~319; the fuzzy name/ROR match is the biggest automated-migration correctness risk — curator confirms near-duplicates); each `Agents(type=Individual)` → a `Contacts` row.

---

## C. Biobanks → `Biobanks` (extends `Organisations`)

> **A biobank is a `Biobanks extends Organisations` subtype row** (2026-07-06 decision) — a real "Biobanks" table directory users recognise. It shares the Resources PK; the *capability* columns live on `Biobanks`, the identity fields on the `Organisations` base; refs to `Organisations` polymorphically accept it. Below, "biobank-`Organisations`" means such a `Biobanks` row.

**Not every biobank becomes a `Biobanks` row** — triage by collection count:

| Directory biobank | → catalogue | why |
|---|---|---|
| 2+ collections (272) | a **`Biobanks`** row holding its N `Collections` (via `held by`) | umbrella biobank |
| 1 collection, has services/quality (part of the 475) | a **`Biobanks`** row holding its one `Collections` | a real biobank unit — flagged to confirm |
| 1 collection, attribute-poor (most of the 475) | **collapse** → just a `Collections`, held by its legal-entity `Organisations` — **no `Biobanks` row** | the record is really only its collection — the main judgement call, flagged |
| 0 collections (102) | an **`Organisations`** identity — **no `Biobanks` row** | bare registration, nothing to hold |
| every `juridical_person` value | a legal-entity **`Organisations`**, linked via `Organisations.part of` | mint one per distinct value; curator dedups |

**So: only 2+‑collection biobanks and attribute-rich 1:1 biobanks become `Biobanks`. 0‑collection biobanks → `Organisations`; attribute-poor 1:1 biobanks collapse into a `Collections`.**

**Columns:**

| directory `Biobanks` | → catalogue (EMX2) | MIABIS v3 | rule |
|---|---|---|---|
| id | `Organisations.id` | ID | key |
| pid | `Organisations.pid` | | |
| name / acronym / description | `Organisations.name` / `acronym` / `description` | Name / Acronym / Description | |
| url | `Organisations.website` | URL | |
| location / latitude / longitude | `Resources.location` ✎ / `latitude` ✎ / `longitude` ✎ | — (not MIABIS) | directory extras — MIABIS has *Country* only |
| country | `Resources.countries[]` | Country | ⎇ single → array-of-one |
| head / contact | `Contacts` (`Contacts.resource ref →` this org; `role`) | Contact information | ⎇ person → Contacts |
| **juridical_person** | `Organisations.part of` `ref →` a legal-entity `Organisations` ✎ | Juristic Person | **upgrade text → identity** (see note); mint ~500 legal entities |
| network | the biobank's `Collections` list under `data resources` (`dcat:dataset`); the **biobank itself** joins via a proposed `Networks.member organisations` `ref → Organisations` (an org *is* a `Resources`, but `data resources` is `dcat:dataset`-typed to `Collections`) | | see Open decisions #8 |
| collections | refback `Organisations.holds` (via `Collections.held by`) | | inverse |
| services | refback `Organisations.services` (via `Services.provider ref → Organisations`) [NEW] | Capabilities | |
| quality | refback `Resources.quality` (via `Quality info.resource ref → Resources`) [NEW] | Quality Management standard | |
| collaboration_commercial / _non_for_profit | `Collections.data use conditions` (DUO — Data Use Ontology) ⎇ or `Organisations.*` bool | Use & access conditions | ⎇ audit |
| also_known | `External identifiers` (`ref → Resources`) | | |
| national_node | `Organisations` identity (the node org) via `Resources.source` ✎ | | provenance (R6); §H |
| withdrawn / mg_draft | `Resources.withdrawn` ✎ / system | Status (v3) → `Collections.status` exists | |
| combined_network / biobank_label | — | | ⌫ derived |

> **`juridical_person` upgrade (key change).** Required free text (MIABIS *Juristic Person*), 500 distinct values across 849 biobanks, 85% distinct from the biobank name — clearly legal entities ("Tel-Aviv University", "Biobanque CHU de Toulouse"). Mint legal-entity `Organisations` identities; link each biobank via `Organisations.part of` (self-`ref`). Curator dedups near-duplicates. **The Directory has no `parent organisation` ref** — this replaces that idea.

> **Main-organisation list (ROR).** A `ror id` marks a **registered top-level organisation** ("UMCG"); informal subunits ("UMCG, dept of Genetics") are **not** separate `Organisations` — they live as `department`/org-unit text on `Organisation roles`. So the "main organisation list" = `Organisations` carrying a `ror id` (or with `part of` = empty, i.e. top-level legal entities); ROR's own parent/child hierarchy lets subunits roll up. This keeps departments off the top level identity table (dedicated links carry control; the role table only carries contribution).

---

## D. Collections → `Collections` (+ facts / events / subpops)

Each Directory `Collection` → a `Collections` row, **`held by`** its biobank (custody). A Directory **sub-collection** (a `parent_collection` child) is usually **not** its own resource — the catalogue expresses a collection's internal structure as `Collection facts` / `Subpopulations` / `Collection events`, not nested collections (*folding* a sub-collection = collapsing its rows into one of those aggregates of the parent, instead of keeping a separate nested collection). Each sub-collection is triaged automatically by **what varies across its sibling children** (measured over **1,319 sub-collections**):

| Child varies by | → catalogue | Count | Automatic? |
|---|---|---|---|
| facts-dimensions only (sample type / disease / sex / age / anatomy) | **`Collection facts`** | 934 (71%) | ✅ |
| institute only (site arm, e.g. `DIA-UMCG` / `DIA-AMC`) | **`Subpopulations`** | 92 | ✅ (abbreviation-aware) |
| timepoint / wave | **`Collection events`** | 38 | ✅ |
| distinct study (`type` varies) | promoted **`Collections`** + `Linkages` | 35 | ✅ |
| ambiguous / single-child / duplicate / mixed | **temporary `Collections`**, flagged `curation status = unresolved-subcollection` + parent `Linkages` | 220 | ✅ lands losslessly, folded in curation |

So ~83% resolve to their real target automatically; the ~17% ambiguous tail lands losslessly as a flagged sub-collection (a **default, not a guess**) and is folded during curation — never a permanent `Subcollection` type. During curation the 220 fold as: duplicates (38 → drop/merge), near-facts (34 → restore to facts), single-child (46 → 19 losslessly mergeable / 27 keep), facts×wave (83 → events + per-event facts), name-only (19 → manual).

> **Site-arm subpopulations are name-coded (corrected finding).** ~92 sub-collections (across 14 parents) are disease cohorts split per recruiting Dutch academic hospital (`DIA-AMC` / `DIA-UMCG` / `DIA-VUMC` …) → `Subpopulations`. A naive site regex (matching English words like `centre|hospital`) misses these institutional **abbreviations** and wrongly reported site-arms as absent — so site detection must be **abbreviation-aware**.

**Relations (EMX2):**
- **`Collections.held by`** (`ref_array → Organisations`, soft-required) — custody = `dcterms:rightsHolder`; refback `Organisations.holds`. From directory `Collections.biobank`. *Soft-required = enforced by migration + curation, not a DB constraint (an empty holder set still loads), so the curation pass must actively find holderless collections (R3).*
- **membership** — the `Networks` container lists the collection (existing container ref). From directory `Collections.network`. Distinct from custody.
- **facts** — `Collection facts.collection` (`ref → Collections`); refback `Collections.facts`.
- **studies** — see §G (refback from the study end).

**Columns:**

| directory `Collections` | → catalogue | MIABIS v3 | rule |
|---|---|---|---|
| id / name / acronym / description / url | `Collections.*` (url→website) | ID / Name / Acronym / Description / URL | |
| type | `Collections.type` (→ Resource types) | Collection type* | *MIABIS v3 renames type→*design*; our model keeps `type` and the existing `Collections.design` separate |
| data_categories | `Collections.areas of information` (existing) | Dataset type | value crosswalk (MIABIS *Dataset type* = *data categories*) |
| sex / diagnosis_available | `Collections.sex` ✎ (new) / `population disease` (existing) | Sex / Disease | detail → `Collection facts` |
| age_low / age_high / age_unit | `Collections.age min` / `age max` / unit | Age Low / Age High / **Age Unit** | ⎇ **`age_unit` mislabelled in the Directory** (tagged as *Description*; should be *Age Unit*) — fix on migration |
| materials | `Collections.biospecimen collected` (existing) ⎇ | Sample type | value crosswalk; per-slice detail → facts |
| storage_temperatures | `Collections.storage temperatures` ✎ (normal column) | Storage temperature | |
| body_part_examined | `Collections.body part` ✎ (normal column) | | summary here; detail → facts `anatomy` ✎ |
| imaging_modality | `Collections.imaging modality` ✎ (normal column) | | |
| country / location / lat / long | `Resources.countries[]` / `location`✎ / `lat`✎ / `long`✎ | Country | lat/long/location not MIABIS |
| head / contact | `Contacts` | Contact information | ⎇ person |
| data_use + collaboration_* + commercial_use | `Collections.data use conditions` (DUO) | Use & access conditions | ⎇ data_use subsumes; optional bool filters |
| access_* / sop | `Collections.*` | | mostly existing (rich RWE model) |
| license / publisher | `Resources.license` / `Resources.publisher` (→ Organisations) | | |
| quality | refback `Resources.quality` (`Quality info.resource`) [NEW] | | |
| size | `Collections.number of samples` ✎ (new) | Total number of subjects | ⎇ or facts measure |
| number_of_donors | `Collections.number of participants` (existing) | | ⎇ or facts measure |
| order_of_magnitude* / combined_* / biobank_label | — | | ⌫ derived |

Proposal: rename 'areas of information' to 'data categories'.

> **Confirmed against the model (MIABIS audit):** existing — `design`, `type`, `status`, `population disease`, `number of participants`, `inclusion criteria`, `data use conditions`, `biospecimen collected` (=materials), `areas of information` (=data_categories); **net-new normal columns** — `sex`, `age low/high unit`, `storage temperatures`, `sample source`, `sample collection setting`, `body part`, `imaging modality`, `number of samples`, `location`/`lat`/`long` (summary on `Collections`; per-slice detail in `Collection facts`).

---

## E. `Collection facts` [NEW] — definition (grounded in the export)

Profiled the real `CollectionFacts` export: **19,581 rows**, star schema, 4 **nullable** dimensions + 2 measures (`*`/empty = "aggregated over this dimension"). Root table, sibling of `Resource counts` / `Subpopulation counts`. Carries **no** MIABIS code (Directory-specific aggregation).

| column | type | maps from / notes |
|---|---|---|
| id | string, key | directory `id` — preserve for round-trip |
| collection | `ref → Collections`, required, key | directory `collection`; refback `Collections.facts` |
| sex | ontology → Sex (nullable) | FEMALE 9133 / MALE 8737 / …; `*`+empty → null. NB: the `Sex` ontology ships Male/Female only — BBMRI Unknown/Other need adding |
| age group | ontology → **Age groups** (nullable) | reuse the ontology `Resource counts` uses |
| sample type | ontology → Sample types (nullable) | DNA 11.9k / SERUM / PLASMA / … (19 distinct) |
| disease | ontology → Diseases (nullable) | 2,282 distinct ORPHA/ICD; empty+`*` → null |
| anatomy | ontology → body-part (nullable) ✎ | not in the export — receives body-part sub-collections (§D) |
| number of samples / number of donors | int | max 55,264 / 167,000 |
| last update | date | directory `last_update` |
| source | provenance | directory `national_node` → `Resources.source` pattern |

**Why new, not extend `Resource counts`:** that table is grain `(resource × age group)` with demographic measures (population size, proportion female, means) — a different aggregate. Folding four sample-inventory dims + sample/donor measures in would mix two concepts and force all-nullable dims.

---

## F. Persons → `Contacts` (reuse — no new Persons table)

`Contacts` already has: `resource` (`ref → Resources`), `organisation` (`ref → Organisations`), `role` (→ Contribution types) + role description, first/last/display name, prefix/initials/title, email, **orcid**, homepage, photo, expertise.

| directory `Persons` | → `Contacts` | MIABIS v3 | rule |
|---|---|---|---|
| full_name / first_name / last_name | `display name` / `first name` / `last name` | Contact: name | |
| title_before / title_after | `title` / `prefix` | | ⎇ partial |
| email / phone | `email` / `phone` ✎ | Contact: email / phone | |
| role | `role` (→ Contribution types) + `role description` | | ⎇ map free-text, else `Other` |
| biobanks / collections / networks | `Contacts.resource` (reverse) | | **orphan persons DROPPED** — `Contacts.resource` is required (it's the key), so a Contact must attach to a resource; unlinked persons cannot be stored (whole-dump migration drops ~824 such orphans) |
| address / zip / city / country | — | Contact: address | ⌫ per docx |
| national_node | `Resources.source` | | provenance |

---

## G. Studies (MIABIS *Research resources*) → `Collections` (typed) + `Linkages`

A directory `Studies` row is a **MIABIS *Research resource*** — *"a set of samples and/or data items used and/or analyzed in a common context in past or current research; may combine material from multiple collections and from multiple biobanks."* It is **not** a "sub-study" of one collection.

Directory `Studies` (135) → each a `Collections` row (`type` = a study type, `status` = Study status). Because a Research resource may draw on **several source collections across several biobanks**, its `wasDerivedFrom` relation is **many** (one `Linkages` row **per source collection**), authored **from the study end**:
- `Linkages.resource` (`ref → Resources`) = the study `Collections`,
- `Linkages.linked resource` (`ref → Resources`) = each source `Collections`,
- `Linkages.relationship type` = `wasDerivedFrom` ✎, `Linkages.source selection` (text) ✎ = which slice.

Each source collection sees the derived research resources via the **refback** `Resources.linked resources`. (Generalises `Linkages` from record-linkage-only to typed — orthogonal.) MIABIS: Study = **Research resource** entity (sparsely tagged in the Directory; the MIABIS token differs across source sheets — same entity).

---

## H. New / reused child tables (FK direction)

| directory | catalogue (EMX2) | note |
|---|---|---|
| QualityInfo{Biobanks,Collections,Services} | `Quality info` **[NEW]**: `resource` (`ref → Resources`); refback `Resources.quality` | **3 → 1 polymorphic**: `resource → Resources` lets one table serve biobank, collection and service quality — the directory's three separate quality tables collapse into one. **Quality is a refback on Resources, not an FK column on Organisations.** |
| Services | `Services` **[NEW]** `extends Resources`: `provider` (`ref → Organisations`); refback `Organisations.services` | a Service is a Resource subtype (inherits id/name/provenance); 32-type MIABIS enum + TRL / unit of access / device |
| DataServices | `DataServices` **[NEW]**: `catalogue` (`ref → Catalogues`); refback | |
| AlsoKnownIn | `External identifiers`: `resource` (`ref → Resources`) | name_system → type, pid, url, label |
| Endpoints | `Endpoint` (existing) | 1:1 |
| Publishers / directory-Organisations / Address | `Organisations` identity | `Resources.publisher ref → Organisations`; mbox → `Organisations.email` ✎; address inline on identity |
| **NationalNodes** / ContactPersons | **an `Organisations` identity** (the national-node coordinating body); migrated records' `source` `ref →` it | a national node **is an `Organisations`** used as the record's `source` — the federation *owner/steward* (an Agent), not a content-grouping (2026-07-06). Contact persons → `Contacts`. data_refresh → `Resources.last data refresh` ✎ |

*(Networks / Catalogs are unchanged containers — no column detail; the only change is that a container's **managing/coordinating body is an `Organisations`** (a Network is not itself an `Organisations`) and a catalogue's maintainer = its `publisher`.)*

---

## J. MIABIS Core v3 feature-completeness (R10)

**Goal (R10):** the target model has a home for **every** MIABIS Core v3 attribute (70 across the 4 entities), even where the Directory doesn't populate it — so the migration is a *superset-ready step toward MIABIS v3*, the key stakeholder-acceptance message. **Result — of 70 v3 attributes: 49 already HAVE a home, 2 via planned new tables, 19 need a net-new column (12 distinct).**

- **Already exist (no new column):** *sample type* → `biospecimen collected`, *dataset type* → `areas of information`, *disease* → `population disease`, *status* → `Collections.status`, *inclusion criteria*, *use & access* → `data use conditions`, *design* → `Collections.design`, *publications* → `Resources.publications`, network *type* & *members* → `network type` / `data resources`.
- **Planned new table:** *Quality Management standard* → `Quality info`. (*Contact information* → `Contacts`, which is **reused** — `phone` is the only net-new column.)
- **Net-new columns (the 12):** Organisations — infrastructural / organisational / bioprocessing-&-analytical *capabilities* (*juristic person* = the `part of` ref); Collections — *sample source*, *sex*, *age low/high unit*, *storage temperature*, *sample collection setting*; Networks — *status*, *common collaboration topics*. **All 12 are in scope.**

**Flags for stakeholders:** `age_unit` is **mislabelled** in the Directory (tagged *Description*, should be *Age Unit*); `location`/`latitude`/`longitude` are **not** MIABIS (Directory extras — MIABIS has *Country* only); the Study/Research-resource token differs across MIABIS source sheets (same entity).

---

## RDF serialisation (the agent story)

Both agent paths resolve to the `Organisations` **identity** (`foaf:Agent`) — which is what makes them RDF-clean:

- **Dedicated formal links** — `publisher` / `creator` / `held by` → *flat* DC/DCAT triples (`dcterms:publisher` / `dcterms:creator` / `dcterms:rightsHolder`) pointing straight at the identity.
- **`Organisation roles`** (the soft contribution roles) → *reified* **`prov:qualifiedAttribution`**: the row **is** a `prov:Attribution` — `.organisation` = `prov:agent` (→ identity), `.role` = `dcat:hadRole`, on `.resource`.

This is the canonical DCAT-AP 3 / HealthDCAT-AP shape, and it only works because the split gives one stable Agent node for `prov:agent`. **Status:** `qualifiedAttribution` / `hadRole` are **not emitted today** (deferred) — a generator enhancement the model enables, not a regression; it needs the pruned role-ontology terms to carry `dcat:Role` URIs.

---

## Migration-script notes (for implementers)

Load-time data-integrity points the mapping tables above don't surface (from the SQL review, 2026-07-08):

- **Org dedup — repoint *every* `ref → Organisations` before deleting a merged org**, or the delete is blocked: `publisher`, `creator`, `held by`, `part of`, `Organisation roles.organisation`, `source`, `imported from`, `Services.provider`, `Collection facts.source`, `Contacts.organisation`, `Endpoint.publisher`/`contact`. `held by` and `creator` are `ref_array` → array-replace **and** de-dup, then delete.
- **Composite keys.** `Organisation roles` PK = `(resource, organisation)` → fold multiple same-org rows (e.g. two departments) into **one** role row (`role` is an array; `department` is not in the key). `Linkages` PK = `(resource, linked resource)` → one linkage per pair (`relationship type` is not in the key).
- **`Collections.type` is required** → every migrated `Collections` (incl. Studies-as-Collections and the temporary sub-collections) must carry ≥1 `Resource types` term; supply a default for type-less directory collections.
- **Insert ordering.** Insert identities (national nodes, legal-entity parents, orgs/biobanks) before rows that reference them; the self/forward refs `part of` / `source` / `imported from` need ordering or deferred constraints.
- **`name` (and `pid`) are global-unique** across all `Resources` subtypes — see Open decisions #6.

---

## Open decisions

1. **`creator` — DECIDED: organisation-only** for now (`Resources.creator` = `ref_array → Organisations`). Person-creators (`dcterms:creator` may range over any Agent) can be added later as `creator → Contacts` or a Contact role=creator; not in this change. `creator` = the *dataset's* creator, distinct from the record steward (`source`, R9).
2. `juridical_person` → legal-entity identities: fuzzy-dedup of the 500 distinct values (curator).
3. `data_categories` target + MIABIS *Dataset type* value crosswalk (§D).
4. **Value-level ontology cross-walks (a workstream of its own).** e.g. Diseases ~8,015 distinct ICD/ORPHA codes; `Biospecimens` ships **no** term file today; `Sample types` has only 5 terms (BBMRI materials need ~14+ added); ~118 SNOMED anatomy codes; countries ISO→names. Until extended, unmapped values load null (~120k value-occurrences over the full dump).
5. Role-ontology terms need URIs (a `dcat:Role`/SKOS concept each) so `Organisation roles` serialises as `dcat:hadRole`.
6. **`name` uniqueness (a load prerequisite, not just open).** `Resources.name` is a **global required-unique** key across *all* subtypes (Collections, Organisations, Biobanks, Networks…), so collisions are cross-type — notably the 1:1 biobank+collection that keep both rows under the same name, the ~500 minted `juridical_person` orgs, and the 871→319 dedup. Disambiguation (id-suffix) must be computed **globally over the whole `Resources` table at load**, or inserts abort on the unique-key violation. (`pid` (KEY2) is likewise globally unique, but nullable.)
7. commercial/collaboration flags: `Organisations` bool vs `Collections` DUO — pick one home.
8. **Network membership of organisations/biobanks.** A biobank is an `Organisations` (a `Resources`), but `Networks.data resources` is `dcat:dataset`-typed to `Collections`, and an org isn't a dataset. Add a dedicated `Networks.member organisations` (`ref_array → Organisations`) — DCAT-clean — rather than broadening `data resources` to `→ Resources` (which would also admit networks/catalogues). **Recommend the dedicated ref.**
