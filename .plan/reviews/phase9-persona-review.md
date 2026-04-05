# Phase 9 — Persona review (rounds 1 + 2)

> **Post-review decisions applied (2026-04-05)**
> The following changes were made to data, docs, spec, and plan based on the decisions in the "Actionable items" section below:
> - Renamed `profiles:` → `subsets:` (column/table-level key) and restructured `molgenis.yaml` from nested `profiles: { internal:, templates: }` to two top-level keys `subsets:` + `templates:` across all 39 `shared2/` table files, `shared2/molgenis.yaml`, `yaml_format.md`, `new_naming.md`, and `phase9-profiles-vs-templates.md`.
> - Dropped `subsets: [-x]` exclusion syntax from the design — no occurrences were found in data files; all mentions removed from docs and specs.
> - Added a DDL worked example to `yaml_format.md` showing YAML → `CREATE TABLE` output.
> - Reconciled the prefix list in `yaml_format.md` — removed the shorter in-place list and replaced with a link to `semantics.md`.
> - Clarified hidden-table DDL: tables with `subsets:` are physically created in PostgreSQL.
> - Added "See also" cross-links section to `yaml_format.md`.
> Round-1 and round-2 review content below is preserved verbatim as a historical record.

**Dates**: 2026-04-05 (round 1), 2026-04-05 (round 2)
**Reviewers** (6 personas, no EMX2 background, parallel):
1. Senior relational data modeler (PostgreSQL/Oracle DBA)
2. FAIR data bioinformatician (DCAT, SHACL, FDP)
3. Python data modeler (Pydantic, SQLAlchemy, dbt, LinkML-adjacent)
4. FHIR / openEHR standards expert
5. LinkML expert (closest peer system)
6. Epidemiological researcher (REDCap/OpenClinica background, non-developer)

**Round-1 scope**: `docs/molgenis/yaml_format.md` alone + `shared2/` examples
**Round-2 scope**: same + full `docs/molgenis/` directory for surrounding context, after authors added a "what this produces" mental model + `semantics:` docs + column-types summary + advanced-features callout + TBD naming note.

---

## Verdict shifts (round 1 → round 2)

| Reviewer | Round 1 | Round 2 | Key driver of shift |
|---|---|---|---|
| Relational DBA | Red | **Yellow-leaning-green** | "What this produces" section answered the core physical-layer question |
| FAIR bioinformatician | Red | **Conditional green** | `dev_rdf.md` + `semantics.md` reveal a genuine RDF/SHACL story; `semantics:` now documented |
| Python modeler | "Reach for LinkML" | **Interested** | Reframed from "schema DSL competitor" to "deployment/runtime system with a schema attached" |
| FHIR/openEHR | Red (poisonous collision) | **Conditional yes** | "Ship with rename and I bring the team" |
| LinkML (closest peer) | Credible complement | **Credible complement with one genuinely novel capability** | Profiles-as-runtime-flags is a position no peer system occupies |
| Researcher | Anxious | **Cautiously optimistic** | Would pilot a cohort study; mental-model section did the heavy lifting |

---

## The single biggest finding — **terminology collision**

Both rounds: all 6 reviewers flagged `profile` as colliding severely across communities:

| Term | Their meaning | Our meaning | Severity |
|---|---|---|---|
| profile | FHIR: `StructureDefinition` that *constrains* a resource | feature flag on columns | severe |
| profile | DCAT-AP / FDP: application profile with SHACL + `conformsTo` URI | feature flag on columns | severe |
| profile | LinkML: closest is `subsets` (flat tag) | feature flag on columns | moderate (wrong word) |
| template | openEHR: composes archetypes, fills slots | preset shown in admin picker | mild |
| template | Python/dbt: Jinja text substitution | deployment preset | mild |

**Round 2 convergence on the rename**: 4 of 6 reviewers (DBA, FAIR, FHIR, LinkML) independently recommended **`subsets:`** for the internal concept — LinkML-native, zero collision with FHIR/DCAT-AP/SQL. The FHIR reviewer's strongest line: *"Ship `profiles`/`templates` and I walk; ship `subsets`/`presets` and I bring the team."*

**Universal rejections** (each hit a blocking collision):
- `variants` — genomic variants, catastrophic for a Beacon v2 platform (FHIR reviewer: "fatal")
- `views` — SQL/GraphQL views
- `featureFlags` — deployment-tooling jargon bleeding into data modeling
- `tags` — too generic, loses the "named grouping with includes" specificity

**DECISION**: rename `profiles` → `subsets` (internal concept). Keep `templates` as-is for the user-facing concept — the openEHR collision is mild and `templates` is already load-bearing in the admin UI. Rename applied to data + docs + spec + plan before Java parser ships. Also: **drop the `profiles: [-x]` exclusion syntax** entirely — simplifies mental model, no reviewer defended it, and it complicated the round-1 decision guide.

---

## Other convergent findings

### What landed well (praised across rounds)

- **Zero-config bundle** (`name: foo` + `description`) — universally praised as a great on-ramp
- **Directory layout diagram** — landed for everyone
- **Decision guide table** — "actionable" (Python), "excellent" (DBA)
- **Snake_case consistency** — zero complaints
- **`Datasets.yaml` reading naturally** on a real 20+ column production table
- **"What this produces" section added in round 2** — single biggest doc improvement. DBA: *"moves me from red to yellow on its own"*. Researcher: *"the sentence I needed in round 1"*. FHIR: *"turns the feature from mystery DSL into feature flags over one physical schema"*
- **TBD naming note** — honest and appreciated as a placeholder signal; reassured the researcher, defused the FHIR objection temporarily
- **Advanced-features callout** (`computed:`/`visible:`/`refLabel:`) — defuses DBA red flag by quarantining JS-in-YAML behind "ignore for new bundles"

### What's still missing (consolidated round-2 findings)

**Blockers for wide release**:
1. ~~Rename `profiles:` → `subsets:` before parser ships~~ (decided — doing it)
2. **Rename or gate `fair_data_point` template on `fdp-v1.2` SHACL validation** — FAIR reviewer: *"having a template literally named `fair_data_point` without a conformance statement invites the assumption that activating it produces a compliant FDP. A support-ticket factory."*
3. **Clarify hidden-table DDL** — a table with `subsets:` is still physically CREATEd in PostgreSQL, just hidden in the API. One sentence closes this.
4. **Reconcile prefix list** in `yaml_format.md` with the fuller list in `semantics.md` — they currently disagree.

**Worth adding before Java (high value, low cost)**:
5. **DDL worked example**: show a small YAML table + the resulting `CREATE TABLE` statement (DBA: *"would eliminate 90% of remaining uncertainty"*).
6. **Cross-links from `yaml_format.md`** to `semantics.md`, `dev_rdf.md`, `dev_beaconv2.md`.
7. **Per-bundle `prefixes:` in `molgenis.yaml`** — let bundles declare their own CURIE prefix map instead of relying on server defaults. Additive and backward-compatible. **Deferred to future work** per user decision.
8. **Typed `semantics:` mappings** — support `exact_mappings:`/`close_mappings:`/`narrow_mappings:` for LinkML round-trip and DCAT-AP compliance. LinkML's #1 ask; *"costs a day, payoff is full LinkML round-trip"*. Additive, non-breaking. **Deferred.**
9. **Clarify `extension` / `extension_array` column type** with a parenthetical that it's unrelated to FHIR Extension (FHIR reviewer).
10. **Migration semantics paragraph**: what happens when you edit `tables/Foo.yaml` on a live deployment? (DBA)

**Follow-up (Phase 10+)**:
11. Admin UI screenshots showing template picker (researcher)
12. JSON Schema for `molgenis.yaml` itself for external tool validation (Python)
13. LinkML ↔ EMX2 lossy converter sketch (LinkML)
14. Versioning + canonical URLs for bundles (FHIR/FAIR)
15. `molgenis-codegen` → Pydantic target (Python)
16. SPARQL endpoint + VoID for full FAIR federation story (FAIR)

---

## Persona-specific key insights (kept for posterity)

### Relational DBA
- **Mental model**: templates ≈ database roles + column-level GRANTs; subsets ≈ named column tags unioned into a role's visible set; `includes` ≈ role inheritance
- **Round-2 line**: *"The 'What this produces' section moves me from red to yellow on its own. What keeps me from green is purely operational: no DDL example, no answer on migration semantics, no word on indexes for `ref_array`/`refback`."*
- **Residual asks**: DDL example, migration semantics, `refback` cost model, `ref_array` index story, transactional boundary of bundle import
- **Verdict**: yellow-leaning-green

### FAIR bioinformatician
- **Round-1 strongest negative**: template literally named `fair_data_point` will confuse DCAT-AP/FDP audiences
- **Round-2 upgrade driver**: `dev_rdf.md` reveals real SHACL validation (`?validate=fdp-v1.2`), content-negotiated RDF in 7 serializations, curated 30+ prefix map including SNOMED/LOINC/HPO/ORDO, `ontologyTermURI` passthrough, Beacon v2
- **Quote**: *"Credible FAIR publisher, oversold FAIR federator. That's more than most 'FAIR-compliant' biomedical platforms ship."*
- **Gaps still**: no SPARQL endpoint, no federation/VoID, no dataset versioning, no PROV/PID minting story, `fair_data_point` template without conformance gate
- **Verdict**: conditional green — rename + `fair_data_point` conformance check are the two remaining blockers

### Python modeler
- **Closest peers**: LinkML, dbt `schema.yml`, Frictionless Table Schema — NOT Pydantic/JSON Schema
- **Round-2 reframing**: *"This is a deployment/runtime system with a schema DSL attached, not a schema DSL competing with LinkML. Reframing alone moved it from skeptical to interested."*
- **Remaining asks**: JSON Schema for `molgenis.yaml`, `molgenis-codegen --pydantic`, documented JS eval context (sandbox? GraalJS? timeouts?)
- **Naming preference**: `variants` (user-facing) + `tags` (column-level). `subsets` acceptable but LinkML-loaded. `views` avoid (SQL collision). `featureFlags` avoid (LaunchDarkly-flavored).
- **Note**: Python reviewer's `variants` preference is **overridden** by the FHIR/FAIR reviewers' "fatal genomic-variants collision" veto.

### FHIR / openEHR
- **Most severe terminology verdict** across both rounds: *"Terminology collision alone will poison every cross-team conversation."*
- **Round-2 upgrade driver**: "What this produces" section translates cleanly to the FHIR mental model; Beacon v2 + RDF docs show this platform earns a seat at the clinical-research table (not primary EHR-of-record, but research/registry/federated-discovery).
- **Profile mechanism**: *"Closer to GraphQL `@include(if:)` or LaunchDarkly feature flag than to `StructureDefinition`."*
- **Naming ranking** (top 3): `subsets` > `tags` > `views`. Avoid `variants` (genomics), `featureFlags` (tone), `columnGroup` (clunky).
- **Deployment-preset naming**: `presets` > `bundles` (already taken) > `deployments`. Keep `templates` is defensible but loses precision.
- **Residual asks**: `extension`/`extension_array` parenthetical clarifying unrelated to FHIR Extension; no FHIR IG import path; exclusion syntax `[-x]` should stay out of first release (✓ decided to drop entirely)
- **Verdict**: *"Ship with rename, I bring the team."*

### LinkML (closest peer)
- **Round-2 novel-capability finding**: *"Profiles-as-runtime-flags backed by one physical schema is a genuinely novel position. LinkML subsets are documentation, SHACL shapes are validation, DCAT-AP profiles are conformance, FHIR profiles are constraint. EMX2 profiles are API-level feature flags. That's a real capability LinkML doesn't have and can't easily express."*
- **Concept mapping** (refined in round 2):
  - table ↔ class, column ↔ inline slot, type ↔ range, refTable ↔ range:ClassName
  - `profiles:` ↔ `in_subset:` + synthesized `subsets:` block
  - `semantics:` ↔ `slot_uri`/`class_uri` (lossy — EMX2 flat list vs LinkML's 5 mapping kinds)
  - `type: ontology` ↔ `enum` with `reachable_from` (ontology tables as first-class data)
  - No EMX2 equivalent for: `tree_root`, `default_range`, `default_prefix`, `mixins`, `is_a`, `pattern`, `unit`, multiple `unique_keys`
- **Round-trip converter**: EMX2→LinkML ~80% mechanical, ~20% annotations-wrapped. LinkML→EMX2 ~90% mechanical but drops `tree_root`/multiple `unique_keys`/`rules`/mapping strengths.
- **Hardest to translate**: JS expressions in `computed:`/`visible:`/`validation:` (biggest source of loss); mapping strength; template deployment metadata (no LinkML equivalent, preserve as `annotations:` under `molgenis:` namespace).
- **The ONE improvement**: *"Rename `profiles:` → `subsets:` AND expand `semantics:` with typed mappings (`exact_mappings:`/`close_mappings:`/`narrow_mappings:`). If strictly one: the typed mappings — additive, backward-compatible, payoff is full LinkML round-trip."*

### Epidemiological researcher
- **Round-1 verdict**: *"Anxious. I'd want IT to set it up and give me a web form."*
- **Round-2 shift**: reading the `use_*.md` docs showed MOLGENIS is REDCap-like at the data-entry layer (screenshots, upload Excel, point-and-click). *"I'd pilot a small cohort study on MOLGENIS now instead of defaulting to REDCap."*
- **Who authors `molgenis.yaml`?** — schema (tables, columns): yes, via Excel upload; bundle/subsets YAML: still no, that's a deployment artifact for a data manager.
- **Three things understood after round 2**:
  1. Subsets don't change the database, they're visibility toggles
  2. A template is just a named preset of which subsets to turn on
  3. Zero-config bundles are two lines — complexity is opt-in
- **TBD naming note**: *"Honestly reassuring. Tells me the team is aware 'profile' is overloaded. A confident 'we know, we'll fix it' reads as maturity, not flux."*
- **One remaining ask**: screenshot/screencast of the admin schema picker showing the template dropdown end-to-end
- **Verdict**: anxious → cautiously optimistic

---

## Actionable items (decided)

### Doing now (before Java parser work)
- [x] Rename `profiles:` → `subsets:` across data, docs, spec, plan, examples
- [x] Drop `subsets: [-x]` exclusion syntax entirely from the design
- [x] Add DDL worked example to `yaml_format.md`
- [x] Reconcile the prefix list with `semantics.md` (or remove the shorter list and link out)
- [x] Clarify hidden-table DDL: "tables with `subsets:` are physically CREATEd; only hidden in the API"
- [x] Keep `templates:` as the user-facing term — openEHR collision is mild, admin UI already uses it, FHIR reviewer accepts it

### Deferred (Phase 10 or later)
- Per-bundle `prefixes:` parameter in `molgenis.yaml`
- Typed `semantics:` mappings (`exact_mappings:`/`close_mappings:`/...)
- `fair_data_point` template conformance gate against `fdp-v1.2` SHACL
- Admin UI screenshots
- JSON Schema for `molgenis.yaml` itself
- LinkML ↔ EMX2 converter
- Versioning + canonical URLs for bundles
- Migration semantics paragraph in docs
- `molgenis-codegen` Pydantic target
- SPARQL endpoint + VoID
- Full FAIR capability matrix (findable/accessible/interoperable/reusable × what ships × what doesn't)
