# Plan: Patient Registry Demo Using EXTENSION Inheritance

**Status**: DRAFT — iterate on naming, descriptions, and limitations

**Goal**: Create `data/templates/patient_registry_demo_using_extensions/` as a working demo of multiple inheritance + EXTENSION columns, replacing the 1,010-column `subject` table that currently uses `visible:` expressions to simulate groups.

**Motivation**:
- `patient_registry_demo` is the perfect real-world test case — a 6,407-line single table with two-tier discrimination (`diseaseGroup` → `subgroups01`)
- Current design duplicates fields with `_DNA`/`_1` suffixes to work around single-table limits
- Demonstrates the value of EXTENSION columns and multiple inheritance visually
- Surfaces limitations of the current extension system

---

## 1. Proposed Inheritance Hierarchy

### Tier 1: Root — `subject`

Root table with EXTENSION column `diseaseGroup` driving tier 1 branching.

**Always-visible fields** (the identity core):
- `ID_SPIDER` (key)
- `ID_Patient`
- `skinGroup` (section heading — may become obsolete under extension)
- `diseaseGroup` — **EXTENSION column** (was ontology, becomes discriminator)

### Tier 2: Disease Group Extensions

All extend `subject`. These match the 10 ontology entries in `diseaseGroup.csv`:

| Table name | EXTENSION value | Label (from ontology) | Fields |
|---|---|---|---|
| `subjectMendel` | `mendel` | Mendelian connective tissue disorders (Mendel) | ~37 |
| `subjectEB` | `eb` | Epidermolysis Bullosa (EB) | ~22 + common |
| `subjectAllocate` | `allocate` | Acquired immunological Low prevalence and Complex Adult diseases of the Skin (ALLOCATE) | ~13 + common |
| `subjectIPPK` | `ippk` | Ichthyosis and Palmoplantar Keratoderma (IPPK) | ~8 + common |
| `subjectMosaic` | `mosaic` | Cutaneous Mosaic Disorders, Naevi & Naevoid skin disorders, complex vascular malformations and vascular tumors (Mosaic) | ~2 + cascading |
| `subjectDNA` | `dna` | Cutaneous diseases related to DNA repair disorders (DNA) | ~53 + tier-3 extension |
| `subjectOther` | `other` | Other Genodermatosis | ~1 (subgroupGenodermatosis ref) |
| `subjectAIBD` | `aibd` | Autoimmune Bullous diseases (AIBD) | — (no current fields) |
| `subjectToxiTEN` | `toxiten` | Severe cutaneous drug reactions (ToxiTEN) | — (no current fields) |
| `subjectED` | `ed` | Ectodermal Dysplasias including Incontinentia Pigmenti and P63-associated disorders (ED) | — (no current fields) |

**Note**: The current CSV only has fields for mendel, eb, allocate, ippk, mosaic, dna, other. The 3 remaining groups (aibd, toxiten, ed) exist in the ontology but have no fields — they could be stubs or omitted.

### Intermediate INTERNAL tables for shared fields

To avoid duplicating the 27 common clinical fields (`dateBirth`, `biologicalSex`, `country`, `patientStatus`, etc.) across 6 non-DNA groups, introduce an **internal** intermediate table:

- `subjectCommonClinical` (INTERNAL, extends `subject`)
  - 27 shared fields: dateBirth, biologicalSex, country, patientStatus, dateFirstContact, centre, centreContact, ageOfOnset, consent fields, disability ICF, images
  - Not selectable in UI

All non-DNA disease group extensions would then extend `subjectCommonClinical` instead of `subject` directly. This is **multiple inheritance** in a diamond shape:
```
subject → subjectCommonClinical → subjectMendel, subjectEB, subjectAllocate, ...
       ↘ subjectDNA (direct, has its own _DNA variants)
```

Alternatively, for the parallel diagnosis fields shared between mendel and non-mendel-non-dna groups:
- `subjectWithDiagnosis` (INTERNAL, extends `subjectCommonClinical`)
  - 26 diagnosis fields: codesystemType, hpo, hgnc, chromsomalTest, geneticTest, histology, immunology, etc.
  - Currently duplicated between mendel (`_1` suffix) and others (no suffix)

**Open question**: Do we want `subjectMendel` to extend `subjectWithDiagnosis` too? That would eliminate the `_1` suffix duplication. Or keep mendel separate because its fields have subtly different semantics?

### Tier 3: DNA Subgroups (extends `subjectDNA`)

`subjectDNA` has its OWN EXTENSION column `subgroups01`. Subgroups match `subgroupsDNA.csv`:

| Table name | EXTENSION value | Label (from ontology) | Fields |
|---|---|---|---|
| `subjectDNAXP` | `xp` | Xeroderma Pigmentosum | ~150 |
| `subjectDNATTD` | `ttd` | TTD (Trichothiodystrophy) | ~106 |
| `subjectDNACS` | `cs` | Cockayne Syndrome | ~157 |
| `subjectDNAHP` | `hp` | Hereditary Poikiloderma | ~83 |
| `subjectDNAAlbinism` | `albinism` | Albinism | ~61 |
| `subjectDNABloom` | `bloom` | Bloom syndrome | — (in ontology but no fields) |
| `subjectDNAWerner` | `werner` | Werner syndrome | — (in ontology but no fields) |

**CRITICAL LIMITATION QUESTION**: Does the current system support EXTENSION columns on **non-root tables**? The `subjectDNA` table is itself an extension of `subject`, but it needs its own EXTENSION column for the subgroup selection. This is nested/multi-level inheritance. Needs verification before proceeding.

---

## 2. Naming Convention — ITERATE

Current proposal:
- Root: `subject` (lowercase, matching current CSV)
- Extensions: `subjectMendel`, `subjectEB`, `subjectDNA` (camelCase, "subject" prefix)
- Internal: `subjectCommonClinical`, `subjectWithDiagnosis`
- DNA subgroups: `subjectDNAXP`, `subjectDNACS` (long but clear)

**Alternative**: Drop the `subject` prefix for the extensions since they all live under `subject` anyway:
- `Mendel`, `EB`, `Allocate`, `IPPK`, `Mosaic`, `DNA`, `Other`
- `DNA.XP`, `DNA.CS`, `DNA.Albinism` — but dots aren't valid in table names

**Alternative 2**: Use full human-readable names:
- `MendelianConnectiveTissue`, `EpidermolysisBullosa`, `DNARepairDisorders`

**Decision needed**: Which naming feels right? The user mentioned wanting to iterate on this.

---

## 3. Labels and Descriptions

For each extension table, we need:
- `label` — user-facing display name (from ontology where available)
- `description` — longer explanation

Draft for each (pulled from diseaseGroup.csv ontology labels):

**subject (root)**
- label: "Subject"
- description: "Patient enrolled in the ERN-SKIN disease registry. The diseaseGroup column determines which extension table applies."

**subjectMendel**
- label: "Mendel — Mendelian connective tissue disorders"
- description: "Patients with Mendelian inheritance rare skin disorders. Includes diagnosis, HPO phenotyping, genetic testing (chromosomal, DNA analysis), histology, immunology, and etiology data."

**subjectEB**
- label: "EB — Epidermolysis Bullosa"
- description: "Patients with Epidermolysis Bullosa. Clinical dataset includes EB type, active lesions with BSA (Body Surface Area) measurements, deformities, mucosal involvement, pain/pruritus scores, biology markers (hemoglobin, CRP, albumin), severity scoring."

**subjectAllocate**
- label: "ALLOCATE — Hidradenitis Suppurativa & related"
- description: "Patients with Acquired immunological Low prevalence and Complex Adult diseases of the Skin. Includes comorbidity history (acne, psoriasis, IBD, arthritis), family history, and physical examination findings."

**subjectIPPK**
- label: "IPPK — Ichthyosis and Palmoplantar Keratoderma"
- description: "Patients with Ichthyosis or Palmoplantar Keratoderma. Includes sample/culture presence, mosaicism, clinical notes, and treatment response."

**subjectMosaic**
- label: "Mosaic — Cutaneous Mosaic Disorders"
- description: "Patients with cutaneous mosaic disorders, naevi, complex vascular malformations, and vascular tumors. Cascading sub-forms per sample type (skin, blood, saliva, other) with variant, protein, VAF, and ACMG classification."

**subjectDNA**
- label: "DNA — Cutaneous diseases related to DNA repair disorders"
- description: "Patients with photosensitive DNA repair disorders. The subgroups01 column selects the specific condition (XP, TTD, CS, HP, Albinism, Bloom, Werner)."

**subjectOther**
- label: "Other Genodermatosis"
- description: "Other rare genodermatoses (Hailey-Hailey, Darier)."

### DNA subgroups

**subjectDNAXP** — Xeroderma Pigmentosum
- description: "XP: autosomal recessive DNA repair disorder causing extreme UV sensitivity. Includes complementation groups (XP-A through XP-G, XP variant), UDS testing, growth assessment, dermatological manifestations, neurological involvement, ocular involvement, skin cancers."

**subjectDNATTD** — Trichothiodystrophy
- description: "TTD: brittle hair and nails with ichthyosis, intellectual disability, photosensitivity. Shares complementation groups with XP. Includes growth, neurological, dermatological manifestations."

**subjectDNACS** — Cockayne Syndrome
- description: "CS: premature aging syndrome with growth failure, neurological regression, and photosensitivity. Complementation groups CS-A, CS-B. Includes growth curves, neurological assessment, ocular involvement."

**subjectDNAHP** — Hereditary Poikiloderma
- description: "HP: Rothmund-Thomson, Bloom-like, poikilodermatous conditions. Includes skin manifestations, growth assessment, cancer surveillance."

**subjectDNAAlbinism** — Albinism
- description: "Oculocutaneous albinism. Includes skin/eye phototype assessment, photoprotection behavior, skin cancer surveillance, visual assessment."

**Decision needed**: Are these descriptions accurate? They are derived from the field contents in subject.yaml but should be reviewed by domain experts.

---

## 4. Limitations to Document

### Backend limitations (need verification)

1. **Nested EXTENSION columns**: Can a non-root table (like `subjectDNA` which extends `subject`) have its own EXTENSION column? The current Phase 1-5 implementation was built for single-level extensions.
   - Test: Create a 3-level hierarchy (root → ext1 with EXTENSION → ext2) and verify insert/query works
   - If not supported: flatten to single level (all subgroups extend `subject` directly with composite EXTENSION column)

2. **Multiple inheritance with shared INTERNAL parents**: `subjectCommonClinical` is INTERNAL and should not appear in the dropdown. Verify that `TableType.INTERNAL` properly hides it from the extension selector.

3. **EXTENSION value domain**: Currently EXTENSION extends STRING. The `diseaseGroup` column was an ontology reference. Converting to EXTENSION loses the ontology link. Options:
   - Keep the ontology reference for docs/semantics, store the extension value separately
   - Use EXTENSION values that match ontology codes (`mendel`, `eb`, `dna` — already the case)
   - Accept the loss of ontology metadata on the discriminator

4. **Within-group visible conditions stay**: Cascading conditions like `foetus===yes → expecteddob`, `activeLesions.some(blisters) → blisterBSA`, `typeSample.some(skin) → skin fields` are legitimate per-field conditionals, not group selection. These should remain as visible expressions. Document this as "visible expressions are still needed for intra-extension conditionals."

### Frontend limitations

5. **Extension selector UX**: Currently a simple dropdown. With 7-10 disease groups (some with subgroups), a flat dropdown becomes unwieldy. Options:
   - **Tree selector**: `subject → DNA → XP` as a drill-down (supports nested extensions)
   - **Grouped dropdown**: group headers for "Disease Group" and "DNA Subgroup"
   - **Two-step**: select diseaseGroup first, then show a second dropdown for DNA subgroups if `dna` was selected
   - **Breadcrumb picker**: current selection shown as `DNA > Xeroderma Pigmentosum`

6. **Form rendering with many extensions**: `subjectDNACS` has 157 columns. Current form rendering may not scale. Options:
   - Collapsible sections per tier (parent fields collapsed by default)
   - Tab view: "Common" | "DNA common" | "CS-specific"
   - Wizard-style multi-page form

7. **Query/filtering across extensions**: When querying `subject`, does the UI know which extensions are populated? Currently needs LEFT JOINs across all children. Filter UI might need to show "disease group" facet first.

8. **Demo data migration**: The existing `data/patient_registry_demo/data/subject.csv` has flat rows with all columns. Under extensions, data must be split by disease group. Conversion script needed.

### What we CANNOT replace with extensions

- **Cascading within a group**: `foetus===yes → expecteddob`, `antigen===yes → resultAntigen` — these are per-patient conditional fields, not group selection
- **Multi-value conditions**: `activeLesions.some(blisters)` where a single patient has multiple active lesion types — each lesion's data is conditional on that specific lesion being selected
- **Cross-group shared conditions**: `patientStatus===dead → ageAtDeath` applies to all groups
- **Sample-type cascades**: `typeSample.some(skin)` creates conditional sub-forms within a single patient record

These must remain as `visible:` expressions even after extension refactoring.

---

## 5. Implementation Steps

### Phase A: Verify backend capabilities (spike)
1. Write a small test: create a 3-level extension hierarchy. Verify:
   - Insert into leaf table creates rows in all ancestors
   - Query root auto-joins all leaves
   - EXTENSION column on non-root works or doesn't
2. Verify INTERNAL table behavior in frontend extension dropdown
3. Document findings in this plan

### Phase B: Create `patient_registry_demo_using_extensions`
1. Copy `data/patient_registry_demo/` structure (ontologies, demodata)
2. Create `tables/` YAML with the new inheritance hierarchy
3. Start with 2-3 groups (mendel, eb, dna+xp) to validate
4. Convert demodata rows: split `subject.csv` by `diseaseGroup` column, write to per-extension CSV files
5. Verify model loads, data imports, queries work

### Phase C: Add to CsvToYamlConverterTest
Add a conversion test that either:
- Converts the new YAML back to CSV (for backup)
- OR verifies the new model against demodata roundtrip

### Phase D: Frontend adjustments
Based on Phase A findings, implement any required UX changes:
- Tree selector for nested extensions (if supported)
- Collapsible form sections
- Extension-aware filter facets

### Phase E: Documentation
- Document the migration pattern: "visible-expression groups → EXTENSION inheritance"
- Guide for when to use extensions vs. visible expressions
- Limitations and workarounds

---

## 6. Questions for Review

1. **Naming**: `subjectMendel` vs `Mendel` vs `MendelianConnectiveTissue`?
2. **INTERNAL intermediate tables**: Include `subjectCommonClinical` and `subjectWithDiagnosis`, or duplicate fields for simplicity?
3. **DNA subgroups**: Flat (all extend `subject`) or nested (extend `subjectDNA`)? Depends on Phase A spike result.
4. **Empty disease groups** (aibd, toxiten, ed): Create empty extension tables as stubs, or omit until fields exist?
5. **Column name harmonization**: Drop `_DNA` and `_1` suffixes? (Makes inheritance clean but breaks any existing query that references those names.)
6. **Demo scope**: Convert the full 6,407-line model, or a subset (mendel + dna+xp) as proof of concept?
7. **UI redesign scope**: In this phase, or as a separate frontend phase?

---

## 7. Open Ontology Research Needed

- Verify `subgroupsDNA` labels against published disease taxonomies (HPO, ORPHA)
- Check if `subgroupGenodermatosis` should be expanded (current list is tiny: Hailey-Hailey, Darier)
- Confirm `diseaseGroup` values match ERN-SKIN working groups (9 ERN-SKIN groups expected; this has 10)
- Verify the 3 currently-empty groups (aibd, toxiten, ed) are intentional or missing data
