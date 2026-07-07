# Merging the Directory — Column Mapping

> Section three of four. Requirements: `requirements.md`. Model changes: `proposed-changes.md`. Data & risk: `analysis.md`.
> Targets **verified against the live `data/_models`** (2026-07-05). **Audience: data manager** — relations are given in EMX2 **`ref` / `refback`** terms (a `ref`/`ref_array` column lives on the child pointing at the parent; the parent sees a `refback`).
> The **MIABIS v3** column = the MIABIS Core v3 attribute a field preserves (human-readable names) — so Directory stakeholders can see MIABIS compatibility is kept.
> Legend: **⎇** choose target (rule given) · **✎** new column · **⌫** drop (derived/system, no info loss) · **[NEW]** net-new table.

---

> **Phase-1 reconciliation (2026-07-05) — read before using this map for Phase 2/3.** Several targets below were refined by implementation; the canonical delta list is in `proposed-changes.md` → "Phase-1 implementation reconciliation". Highlights: identity **key = `id`** (inherited from Resources), not `name`; `held by` is **soft-required** (Option C — R3 by data), and its migration source is the old **custody role, not `publisher`**; **no separate `Resources.source id`** column (`id` doubles as the local id — the "(+ Resources.source id ✎)" in §C/§D rows is stale); `organisations involved` → **`Organisation roles`** refback, not the identity; **`DataServices [NEW]`** deferred to Phase 2; `Linkages.relationship type` is **string** for now (ontology → Phase 2).

## A. Table-level map

### A.1 Complete directory dump → catalogue (all 22 dump tables)

Every table in the directory dump has a target; nothing is dropped silently.

| Directory table | → Catalogue | Note |
|---|---|---|
| Biobanks | `Biobanks` (extends `Organisations`) | subtype; 1:1 triage; `juridical_person` → `part of` legal-entity `Organisations` |
| Collections | `Collections` (+ `held by`) | `parent_collection` → facts/events/subpopulations/promoted |
| CollectionFacts | `Collection facts` | dimensioned aggregate |
| Studies | `Collections` (typed study) + `Linkages` | `wasDerivedFrom` |
| Persons | `Contacts` | |
| Networks | `Networks` + coordinating (host) `Organisations` | |
| NationalNodes | → `Organisations` (national-node identity) via `source` | node = an owning/stewarding org, not a grouping |
| ContactPersonsNationalNodes | `Contacts` | 0 rows |
| Organisations | `Organisations` (identity) | |
| Publishers | `Organisations` (publisher identity) | `Resources.publisher` → it |
| Address | inline on `Organisations` (`address`) | no separate table |
| Services | `Services` (extends `Resources`) | `provider` → `Organisations` |
| DataServices | `DataServices` — DEFERRED (not built in Phase 1) | 0 rows |
| QualityInfoBiobanks | `Quality info` (`resource` → biobank) | **3 → 1 polymorphic** |
| QualityInfoCollections | `Quality info` (`resource` → collection) | |
| QualityInfoServices | `Quality info` (`resource` → service) | |
| Catalogs | `Catalogues` | |
| Endpoints | `Endpoint` | FDP |
| AlsoKnownIn | `External identifiers` | alt-ids |
| molgenis / molgenis_members / molgenis_settings | — (schema / permissions / UI settings) | system metadata, dropped |

**`Quality info` applies to any `Resource` (`resource → Resources`), so the directory's three quality tables (Biobanks/Collections/Services) collapse into one — removing that duplication.**

### A.2 Touched catalogue tables — what changes

Only the tables below are touched; every other catalogue table is unchanged and not mentioned here.

| catalogue table | status | role (DCAT/PROV) | fed by (directory) |
|---|---|---|---|
| `Organisations` | **replaced** → now `extends Resources` | **top level** legal-entity identity (`foaf:Agent`) | Networks (managing/coordinating body), Publishers, directory-Organisations, `juridical_person` |
| `Biobanks` | **[NEW]** `extends Organisations` | biobank identity + capabilities (`foaf:Agent` subtype) | Biobanks |
| `Organisation roles` | **renamed** ← old `Organisations` table | per-resource **attribution** (`prov:qualifiedAttribution`) | org role-links on any resource |
| `ROR` ontology | **renamed** ← `Organisations` ontology | external-id reference | — |
| `Agents` | **discontinued** | → split to `Contacts` + `Organisation roles` | — |
| `Collections` | +`held by` ✎ + normal-column adds ✎ | dataset (`dcat:Dataset`) | Collections, Studies |
| `Collection facts` | **[NEW]** | aggregate breakdown | CollectionFacts, sub-collections |
| `Contacts` | reused | person-in-a-role (`foaf:Person`) | Persons, head/contact |
| `Quality info` | **[NEW]** | quality/certification | QualityInfo{Biobanks,Collections,Services} |
| `Services` | **[NEW]** `extends Resources` | biobank services | Services |
| `DataServices` | **[NEW]** | data services | DataServices |
| `Linkages` | +typed ✎ | collection relationships | Studies↔Collections, derived |
| `Networks`, `Catalogues` | **unchanged** containers (managed by an `Organisations`) | grouping / published catalog | Networks, Catalogs |
| `External identifiers`, `Endpoint` | reused | alt-ids / FDP endpoints | AlsoKnownIn, Endpoints |

**Net-new columns:** `Collections.held by` / `sex` / `age low+high unit` / `storage temperatures` / `body part` / `imaging modality` / `number of samples` / `sample source` / `sample collection setting`; `Organisations.part of` + *capabilities* (infra/org/bioprocessing); `Networks.status` + `common collaboration topics`; `Resources.source`/`imported from`/`location`/`latitude`/`longitude`/`last data refresh`/`withdrawn` (**`source`** = original owner = the national node; **`imported from`** = immediate upstream = the BBMRI-ERIC Directory for migrated records; both `ref → Organisations`); `Collection facts.anatomy`; `Linkages.relationship type`+`source selection`; `Contacts.phone`; `Organisations.email`+`phone`. *(materials → existing `biospecimen collected`; data_categories → existing `areas of information` — not new.)*

---

## Row-mapping rules (read first)

*How each directory **row** becomes catalogue rows. Column-level detail is in §B–§K below.*

> **`mapping_ledger.csv`** — the **case-by-case mapping of every directory record** (29,013 rows: one per source record + minted legal entities). Columns: `directory_table`, `directory_id`, `directory_name`, `catalogue_table`, `catalogue_id`, `mapping_rule`, `disposition` (`auto` / `needs_curation` / `dropped`), `flag_reason`, `from_UMCG`. Headline **94% auto / 3.2% needs-curation / 2.9% dropped**. Filter `disposition = needs_curation` for the curation worklist, or `from_UMCG = TRUE` for fix-at-source. Summary in `mapping_ledger_summary.md`.

### 1) Biobanks — which directory Biobank rows become what (triage by collection count)

| directory Biobank | → catalogue | note |
|---|---|---|
| 0 collections | a `Biobanks` row that holds nothing | bare registration |
| 2+ collections | a `Biobanks` row holding N `Collections` (via `held by`) | umbrella org |
| 1 collection (1:1), **attribute-poor** (no services/quality) | **collapse** → ONE `Collections` held by its legal-entity `Organisations` (**no** `Biobanks` minted) | the main judgement call — flagged for review |
| 1 collection (1:1), **real org-unit** (has services/quality) | a `Biobanks` row holding its one `Collections` | flagged for review |
| `juridical_person` | a legal-entity `Organisations`, linked via `Biobanks.part of` | mint + curator dedup |

### 2) Collections & Studies

| directory row | → catalogue |
|---|---|
| Collection | a `Collections` row, **`held by`** its biobank (custody) |
| Study | a typed `Collections` **+** a `Linkages` (`wasDerivedFrom`) to its source collection |

### 3) Sub-collections (`parent_collection` children) — re-expression rules

Classified by what varies across sibling children (measured over **1,319 sub-collections**):

| Child varies by | → catalogue | Count | Auto? |
|---|---|---|---|
| facts-dimensions only (sample type / disease / sex / age / anatomy) | **`Collection facts`** | 934 (71%) | ✅ automatic |
| institute only (site arm, e.g. `DIA-UMCG` / `DIA-AMC`) | **`Subpopulations`** | 92 | needs abbreviation-aware detection |
| timepoint / wave | **`Collection events`** | 38 | |
| distinct study (`type` varies) | promoted **`Collections`** + `Linkages` | 35 | |
| ambiguous / single-child / duplicate / mixed | **review (human)** | 220 | |

**The 220 review cases:** duplicates (38 → drop/merge), near-facts (34 → restore to facts), single-child (46 → 19 losslessly mergeable / 27 keep), facts×wave (83 → events + per-event facts), name-only (19 → manual).

---

## B. `Agents` → `Contacts` + `Organisation roles` (the identity/attribution split)

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
- **`Organisations`** (top level identity) `extends Resources`; key = `name` (+`ror id` `ref → ROR`). Refback `Resources.organisation roles`, `Organisations.holds` (from `Collections.held by`), `Organisations.part of` (self-`ref`, legal entity).
- **`Organisation roles`**: `resource` (`ref → Resources`), `organisation` (`ref → Organisations`), `role` (ontology, pruned), `department`/org-unit (text).
- **Repoint** (were `→ Agents`): `Endpoint.publisher`, `Endpoint.contact`, and the identity refs `Resources.publisher`/`creator`/`organisations involved`, `Contacts.organisation` → new `Organisations`.

**Existing-data migration:** each per-resource `Organisations`/`Agents(type=org)` row → an `Organisation roles` row **+** folds (dedup by name/ROR) into one minted `Organisations` identity (871 → ~319, see analysis §3); each `Agents(type=Individual)` → a `Contacts` row.

---

## C. Biobanks → `Biobanks` (extends `Organisations`)

> **A biobank is a `Biobanks extends Organisations` subtype row** (2026-07-06 decision) — a real "Biobanks" table directory users recognise. It shares the Resources PK; the *capability* columns live on `Biobanks`, the identity fields on the `Organisations` base; refs to `Organisations` polymorphically accept it. Below, "biobank-`Organisations`" means such a `Biobanks` row.

**Scenarios** (counts from analysis §1):

| shape | count | mapping |
|---|---|---|
| 0 collections | 102 | one `Organisations` identity; holds no `Collections` |
| 1 collection | 475 | **triage** — usually one `Collections` **held by** the legal entity; a biobank-`Organisations` only if it is a real org unit |
| 2+ collections | 272 | a biobank-`Organisations` holding N `Collections` (via `held by` refback) |

**Columns:**

| directory `Biobanks` | → catalogue (EMX2) | MIABIS v3 | rule |
|---|---|---|---|
| id | `Organisations.id` (+ `Resources.source id` ✎) | ID | key |
| pid | `Organisations.pid` | | |
| name / acronym / description | `Organisations.name` / `acronym` / `description` | Name / Acronym / Description | |
| url | `Organisations.website` | URL | |
| location / latitude / longitude | `Resources.location` ✎ / `latitude` ✎ / `longitude` ✎ | — (not MIABIS) | directory extras — MIABIS has *Country* only |
| country | `Resources.countries[]` | Country | ⎇ single → array-of-one |
| head / contact | `Contacts` (`Contacts.resource ref →` this org; `role`) | Contact information | ⎇ person → Contacts |
| **juridical_person** | `Organisations.part of` `ref →` a legal-entity `Organisations` ✎ | Juristic Person | **upgrade text → identity** (§C note); mint ~500 legal entities |
| network | membership — the `Networks` container lists it | | biobank ∈ ≥1 network |
| collections | refback `Organisations.holds` (via `Collections.held by`) | | inverse |
| services | refback `Organisations.services` (via `Services.provider ref → Organisations`) [NEW] | Capabilities | |
| quality | refback `Resources.quality` (via `Quality info.resource ref → Resources`) [NEW] | Quality Management standard | |
| collaboration_commercial / _non_for_profit | `Collections.data use conditions` (DUO) ⎇ or `Organisations.*` bool | Use & access conditions | ⎇ audit |
| also_known | `External identifiers` (`ref → Resources`) | | |
| national_node | `Organisations` identity (the node org) via `Resources.source` ✎ | | provenance (R6); §H |
| withdrawn / mg_draft | `Resources.withdrawn` ✎ / system | Status (v3) → `Collections.status` exists | |
| combined_network / biobank_label | — | | ⌫ derived |

> **`juridical_person` upgrade (key change).** Required free text (MIABIS *Juristic Person*), 500 distinct values across 849 biobanks, 85% distinct from the biobank name (analysis §2) — clearly legal entities ("Tel-Aviv University", "Biobanque CHU de Toulouse"). Mint legal-entity `Organisations` identities; link each biobank via `Organisations.part of` (self-`ref`). Curator dedups near-duplicates. **The Directory has no `parent organisation` ref** — this replaces that idea.

> **Main-organisation list (ROR).** A `ror id` marks a **registered top-level organisation** ("UMCG"); informal subunits ("UMCG, dept of Genetics") are **not** separate `Organisations` — they live as `department`/org-unit text on `Organisation roles`. So the "main organisation list" = `Organisations` carrying a `ror id` (or with `part of` = empty, i.e. top-level legal entities); ROR's own parent/child hierarchy lets subunits roll up. This is why departments are kept off the top level identity table (C2/C6).

---

## D. Collections → `Collections` (+ facts / events / subpops)

**`parent_collection` tree** — re-express by *why* the child was nested (measured: 206 parents / analysis §4):

| child differs by | share of multi-child | target |
|---|---|---|
| one dimension (material / diagnosis / body-part) | 90 (56%) | `Collection facts` **[NEW]** — clean, automatable |
| 2+ dimensions | 57 (36%) | still `Collection facts` (star schema absorbs multiple dims) **unless** a varying dim is `type` → promote to a separate `Collections`, or `data_categories`/temporal → modality dim / `Collection events` |
| timepoint / wave | few | `Collection events` |
| distinct study (`type` varies) | few | promoted `Collections` + `Linkages` (`wasDerivedFrom`) |
| single-child (46) / no-vary (15) | — | review (artifacts/duplicates) |

> **Site-arm subpopulations are name-coded (corrected finding).** ~92 sub-collections (across 14 parents) are disease cohorts split per recruiting Dutch academic hospital (`DIA-AMC` / `DIA-UMCG` / `DIA-VUMC` …) → `Subpopulations`. A naive site regex (matching English words like `centre|hospital`) misses these institutional **abbreviations** and wrongly reported site-arms as absent — so site detection must be **abbreviation-aware**. This supersedes the earlier "subpopulation unused" reading.

**Relations (EMX2):**
- **`Collections.held by`** (`ref_array → Organisations`, **required**) — custody = `dcterms:rightsHolder`; refback `Organisations.holds`. From directory `Collections.biobank`.
- **membership** — the `Networks` container lists the collection (existing container ref). From directory `Collections.network`. Distinct from custody.
- **facts** — `Collection facts.collection` (`ref → Collections`); refback `Collections.facts`.
- **studies** — see §G (refback from the study end).

**Columns:**

| directory `Collections` | → catalogue | MIABIS v3 | rule |
|---|---|---|---|
| id / name / acronym / description / url | `Collections.*` (url→website) | ID / Name / Acronym / Description / URL | (+`source id`) |
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
| sex | ontology → Genders (nullable) | FEMALE 9133 / MALE 8737 / …; `*`+empty → null |
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

## G. Studies → `Collections` (typed study) + `Linkages` (refback from the study end)

Directory `Studies` (365) → each a `Collections` row (`type` = a study type, `status` = Study status). The `Studies.collections` relation is authored **once, from the study end**, as a `Linkages` row:
- `Linkages.resource` (`ref → Resources`) = the study `Collections`,
- `Linkages.linked resource` (`ref → Resources`) = the source `Collections`,
- `Linkages.relationship type` = `wasDerivedFrom` ✎, `Linkages.source selection` (text) ✎ = which slice.

The source collection sees derived studies via the **refback** `Resources.linked resources`. (Generalises `Linkages` from record-linkage-only to typed — orthogonal.) MIABIS: Study = Research resource entity (sparsely tagged in the Directory; the MIABIS token differs across source sheets — same entity).

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
- **Planned new tables:** *Quality Management standard* → `Quality info`; *Contact information* → `Contacts`.
- **Net-new columns (the 12):** Organisations — infrastructural / organisational / bioprocessing-&-analytical *capabilities* (*juristic person* = the `part of` ref); Collections — *sample source*, *sex*, *age low/high unit*, *storage temperature*, *sample collection setting*; Networks — *status*, *common collaboration topics*. **All 12 are in scope (proposed-changes C13).**

**Flags for stakeholders:** `age_unit` is **mislabelled** in the Directory (tagged *Description*, should be *Age Unit*); `location`/`latitude`/`longitude` are **not** MIABIS (Directory extras — MIABIS has *Country* only); the Study/Research-resource token differs across MIABIS source sheets (same entity).

---

## K. Open items (for the PoC)
1. **`creator` — DECIDED: organisation-only** for now (`Resources.creator` = `ref_array → Organisations`). Person-creators (`dcterms:creator` may range over any Agent) can be added later as `creator → Contacts` or a Contact role=creator; not in this change.
2. `juridical_person` → legal-entity identities: fuzzy-dedup of the 500 distinct values (curator).
3. `data_categories` target + MIABIS *Dataset type* value crosswalk (§D).
4. Value-level ontology cross-walks (types, materials→sample types, diseases, age groups).
5. Role-ontology terms need URIs (a `dcat:Role`/SKOS concept each) so `Organisation roles` serialises as `dcat:hadRole`.
6. `name` uniqueness — BBMRI names aren't unique; append id-suffix on collision.
7. commercial/collaboration flags: `Organisations` bool vs `Collections` DUO — pick one home.
