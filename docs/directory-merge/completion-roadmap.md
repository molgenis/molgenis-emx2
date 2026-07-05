# Completion Roadmap — from PoC to Production

The proof-of-concept (model change + whole-dump migration + a loading demo) is done and validated. **None of the items below are model-validation gaps — the model is proven.** They are the reference-data, curation, and execution work to take this from PoC to production.

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

- Wire `Studies.also_known` (only **34 / 166** alt-ids resolved — ECRIN-MDR ids live on studies).
- Add a facts-anatomy tie-breaker so the `type`-varies rule stops over-promoting large body-part families.
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
