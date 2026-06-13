# Real `subject` visible= axes — inspiration for the demo model + Phase H (2026-06-13)

Source: `data/patient_registry_demo/molgenis.csv` (1164 lines; the `subject` table fakes modular
column groups with ~1000 `visible=` JS expressions). Owner (2026-06-13): use this as INSPIRATION for
the feature-showcase demo model — make the demo realistic, not a toy; it doubles as a Phase H preview.

## Axis variables referenced in visible= expressions (by frequency)
| Axis var | refs | nature |
|----------|------|--------|
| `subgroups01` | 557 | PRIMARY modular axis — disease subgroups multi-select → maps to a MODULE_ARRAY |
| `diseaseGroup` | 227 | secondary orthogonal axis (disease group / coding system) → second MODULE_ARRAY |
| `biospecimenTypes` | 42 | conditional group |
| `knownGeneticTest` | 19 | conditional |
| `activeLesionsBSA`, `pregbool`, `growthknown`, `compGroup`, `clinicalbool`, `birthWeightKnown`, … | ≤12 each | long tail — small conditional groups, likely stay as visible= or minor modules |

## Distinct values (the modules per axis)
- **`subgroups01`** (disease subgroups): `cs` (Cockayne Syndrome), `xp` (Xeroderma Pigmentosum),
  `ttd` (Trichothiodystrophy), `hp`, `dna` (DNA-repair), `albinism`. Each value gates a column group
  → becomes a MODULE table; the column is a MODULE_ARRAY listing them.
- **`diseaseGroup`**: `eb` (Epidermolysis Bullosa) + coding-system values (`icd10`, `ordo`, `mendel`,
  `mosaic`, `other`, …). The disease-subtype ones (eb, …) → MODULE tables; the coding-system ones are
  more like ENUM choices than modules — datamodeler to judge per real column gating.

## Mapping rule (visible= → engine)
`subgroups01?.name=="cs"` on a column  ⇒  that column belongs to MODULE `CockayneSyndrome` (extends
Subject); `subgroups01` becomes a MODULE_ARRAY column on Subject whose `values` list the subgroup
MODULEs. A row with `subgroups01` containing `cs` activates the `CockayneSyndrome` module → its column
group shows (engine row-presence), replacing the per-column `visible=` predicate.

## For the DEMO (trim, don't port all 557 columns)
Pick ~2 axes (subgroups01 + diseaseGroup), 3–4 MODULEs each drawn from the REAL subgroup values
(cs/xp/ttd; eb/…), a handful of REAL columns per module (read the actual columns gated by each
predicate in molgenis.csv), plus: a diamond (ClinicalSubject+ResearchSubject→ClinicalResearchSubject),
ENUM/ENUM_ARRAY with `values`, module-extends-module, and demo data rows activating none/one/both.
Datamodeler MUST read `data/patient_registry_demo/molgenis.csv` to lift real column names/types per
subgroup so the demo feels authentic. Full port of all axes/columns = Phase H (separate, larger).
