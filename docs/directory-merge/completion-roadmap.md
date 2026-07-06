# Completion Roadmap — from PoC to Production

The proof-of-concept (model change + whole-dump migration + a loading demo) is done and validated. **None of the items below are model-validation gaps — the model is proven.** They are the reference-data, curation, and execution work to take this from PoC to production.

## Migration strategy — load-then-curate (2-step)

Curation can be sequenced two ways: **curate-then-load** (resolve the flagged cases outside the catalogue, load a clean result) or **load-then-curate** (load everything with deterministic defaults, then curators refine in-catalogue). The PoC proves the second is viable — the full auto-migrated output loads clean (31,877 rows, ~79 s, 0 structural errors) with a deterministic default applied to every flagged case. Load-then-curate fits R9 (stewards edit their own records), R5/R6 (curate per `source`, distributed — no central bottleneck), and unblocks Directory retirement (R8): Step 1 lands, curation continues in parallel.

**Step 1 — automatic bulk migration + load.** Every rule below is deterministic — it always yields a loadable record (a default, never a failure) and tags a *review reason* where the default is a judgment call:

| Decision | Automatic rule (default) | Flags for curation |
|---|---|---|
| Biobank shape | 0-coll → `Organisations` identity; 2+-coll → biobank-org holding N `Collections`; 1:1 → collapse to a `Collections` held by its legal entity if attribute-poor, else mint a biobank-org (has quality/services) | every 1:1 "is-it-a-real-org?" (475) |
| `juridical_person` | mint one legal-entity `Organisations` per normalized-exact distinct value; link via `part of` | fuzzy near-dup clusters — minted separately, flagged as candidate-merges (31); never auto-merged |
| `parent_collection` (1,319 sub-collections / 206 parents) | classify by varying dimension: facts-dims → `Collection facts` (default, **934 / 71%**); `type` varies → promoted `Collections` + `Linkages`; temporal → `Collection events`; site → `Subpopulations` | 385 (29%) reviewed: `collection_event` 38 / `subpopulation` 0 / `promoted_collection` 35 / `unclear_mixture` 312 |
| `held by` | custody signal: old "Data holder" role → `biobank` → publisher → lead-org | — (deterministic) |
| Ontology values | crosswalk code → catalogue term; unmappable → safe default (type → `Biobank`) or null the dimension | each unmapped code (feeds item 1) |
| `Resources.name` collision | append an id-suffix | names a curator may want to merge/rename (832) |
| `source` / ids | `source` = national node; global id = `source:localid`, never rewritten | — (deterministic) |
| Persons / Studies / Networks / QualityInfo / Services | deterministic structural map | — (mostly deterministic) |

**Step 2 — incremental in-catalogue curation.** Curators query the review-reason tag within their own `source` and accept-or-adjust each default via the catalogue's data-entry UI (confirm a 1:1 collapse vs promote to a real org, re-classify a sub-collection, merge fuzzy legal entities).

**Enablers this adds:** a review-flag surfaced in the UI (`mg_draft` or a dedicated `curation status`, plus a per-`source` saved filter "flagged records") and — for the fuzzy-dedup tail only — an in-catalogue **merge tool** (post-load merging must re-point `part of`/`held by`/`publisher` refs), or run entity-dedup pre-load and everything else load-then-curate (hybrid).

## 1. Ontology extension & crosswalks (the main data task)

The catalogue's fixed `CatalogueOntologies` are intentionally small (~5 sample types, ~9 resource types) and don't yet cover BBMRI vocabulary. The *model has the columns*; the *reference ontologies* need extending + a code→term crosswalk:

- **Diseases:** ~8015 distinct ICD/ORPHA codes (`urn:miriam:icd:*`, `ORPHA:*`) → disease ontology terms.
- **Biospecimens / sample types:** the `Biospecimens` ontology currently ships **zero** terms; BBMRI materials (Blood, DNA, Serum, …) need adding/crosswalking.
- **Body part / anatomy:** ~118 SNOMED body-part codes → anatomical-location terms.
- **Collection / resource types:** BBMRI types (Disease-specific, Rare-disease, …) → catalogue resource types (currently collapsed to `Biobank`).
- (Countries, age groups, sex already resolve.)

| Dimension | Distinct codes needing a term/crosswalk |
|---|---|
| Diseases (ICD/ORPHA) | **8015** |
| Anatomical location (SNOMED) | 118 |
| Countries (dump uses ISO alpha-2; ontology keys on full names) | 35 |
| Biospecimens (**ontology ships no term file today**) | 17 |
| Resource types | 16 |
| Sample types | 15 |
| Age groups | 10 |
| Storage conditions | 5 |

Measured over the full dump: **8233 distinct unresolvable values, ~120,290 value-occurrences** would be null until the ontologies are extended (largest: population-disease ~40.8k, facts sample-type ~18.5k, facts disease ~17.7k, facts age-group ~16.6k, biospecimen ~10.9k). The '8015 codes' figure is exactly the Diseases dimension.

## 2. Faithful directory demo as a separate profile

Replace the PoC's minimal augmentation of the cohort demo with a dedicated `directory` demo profile on the extended ontologies + identifier-light legal entities — so directory records show in the app with their real vocabulary/dimensions instead of placeholder scaffolding.

## 3. Curation of the flagged ambiguous cases

The human-in-the-loop work the PoC surfaced:

- **475** 1:1 biobanks — confirm collapse-vs-mint.
- **31** fuzzy legal-entity near-duplicate pairs.
- **114** `parent_collection` parents: 25 type-varies → promote, 28 multi-dimension, 46 single-child, 15 no-vary.

## 4. Migration-rule refinements

The per-sub-collection analysis (1,319 sub-collections across 206 parents) classifies **934 (71%) → `Collection facts`** automatically; **385 (29%)** need review — `collection_event` 38, `subpopulation` 0, `promoted_collection` 35, `unclear_mixture` 312. The refinements below lift the auto-rate and tame the review queue:

- **Robust `type`-detection (supersedes the old facts-anatomy tie-breaker — now addressed).** The promote-on-`type`-varies rule must handle BBMRI's **multi-valued `type` sets**: set-ordering, the `IMAGE` modality token, rare (<10%) outliers, and nested-subset diffs. Naive dimension-counting wrongly promoted the two mega-parents **FFPEslides (262 children)** and **CryoCollection (161)** — which are actually **facts** (disease / anatomy dimensions). Fixing this is what lifts the auto-rate.
- **`subpopulation` is unused (new finding).** The Directory does **not** encode site/centre arms as sub-collections — country/location almost never vary, and apparent site name-matches were anatomy/diagnosis/biobank-name false positives. So the `Subpopulations` target the design anticipated for sub-collections **isn't exercised by directory data** (Subpopulations remain in the model for other uses).
- **Never auto-trust temporal detection (low-confidence heuristic).** `data_categories`→temporal detection conflates omics modality and case/control with true timepoints — flag, don't auto-classify. The genuinely hard cases are the **~83 facts×wave mixtures** (timepoint AND facts-dims both vary → want `Collection events` + per-event facts).
- **`unclear_mixture` (312) review queue** breaks down as: ~38 duplicates + ~111 name-only slices (distinction only in the free-text name, no structured column) + ~83 facts×wave + ~34 near-facts (a spurious `data_categories` diff — a curator would likely restore to facts) + ~46 single-child parents (flatten into parent).
- Wire `Studies.also_known` (only **34 / 166** alt-ids resolved — ECRIN-MDR ids live on studies).
- Clean junk `juridical_person` placeholders ("No information", contact-id strings).

## 5. Model decision — global-unique `Resources.name` vs duplicate BBMRI names

The model keys `Resources.name` globally unique (`Resources_KEY3`; Organisations, Collections and Networks share the Resources table). Real BBMRI data has **832 duplicate names among 5,735 resources** (360 colliding groups — reused hospital/biobank names). The full-scale load reached a clean load by disambiguating with an id-suffix (per column-map §K.6), but at this magnitude the choice deserves an explicit decision: **(a) disambiguate on migration** (id-suffix — keeps the constraint, mutates display names) vs **(b) relax the `name` uniqueness key** (allow duplicate names; rely on `id`/`pid` for identity — `id` is globally unique, `pid` unique-or-empty). Not a blocker either way; the PoC used (a).

## 6. Full-scale load — done; semantic spot-check remaining

The full-scale in-database load is **done and passed**: the entire migrated output (**31,877 rows / 9 tables; 4746 collections, 19,924 facts**) loaded into a fresh catalogue schema in **~79 s with 0 structural errors**, survived a full export→import round-trip (~92 s, counts stable), and spot-checks resolve. What remains under this item: a **semantic** spot-check at scale + the production run on the real ontologies.

## 7. Cross-catalogue dedup

When merging with the live catalogue, dedup the existing **~871** per-resource org rows → **~319** identities against the minted directory orgs.

## 8. RDF `prov:qualifiedAttribution` (deferred generator enhancement)

Emit reified attribution (`Organisation roles` → `prov:Attribution`, `.role` → `dcat:hadRole`) + give the role-ontology terms `dcat:Role` URIs. The model already enables it.

## 9. Retire the Directory app + codebase

Once the production migration lands (the end goal).

## Sequencing

Item 1 (ontology) unblocks item 2 (faithful demo) and reduces the emptied fields; items 3–4 are curation/tuning and item 5 is a model decision; items 6–7 are execution; 8–9 are cleanup/decommission.
