# Demo: Resource Detail Page with Nested Tables — Plan

## Goal
Prove Emx2RecordView can render a Resource detail page with nested CollectionEvents and Subpopulations, configured entirely through data model metadata (HEADING columns, display, summary, role settings). Find the **minimum configuration** needed.

## Scope
- Resource detail page rendered by Emx2RecordView
- Nested CollectionEvents + Subpopulations shown as tables (Emx2ListView)
- Organisations shown as cards (ListCard)
- Section grouping via HEADING columns
- Sidebar navigation auto-generated from HEADINGs
- NOT: nested URL routing, singular record ID, catalogue app changes

## What Needs Configuration

### 1. HEADING columns in Resources model
Without HEADINGs, everything renders as a flat list. Need to add HEADING rows to `data/_models/shared/Resources.csv` to group fields into sections.

Proposed sections (matching current catalogue layout):
- General Information (description, design fields)
- Population (countries, participants, age groups, medical conditions)
- Organisations (organisationsInvolved)
- Contributors (peopleInvolved)
- Datasets (datasets)
- Collection Events (collectionEvents)
- Subpopulations (subpopulations)
- Networks (partOfNetworks)
- Publications (publications)
- Access Conditions (dataAccessConditions, dataUseConditions)

### 2. Display settings on REF_ARRAY/REFBACK columns
Already partially done in Resources.csv. Verify:
- `organisationsInvolved`: display=CARDS
- `peopleInvolved`: display=CARDS
- `datasets`: display=TABLE (default)
- `collectionEvents`: display=TABLE (default)
- `subpopulations`: display=TABLE (default)
- `partOfNetworks`: display=CARDS
- `publications`: display=TABLE (default, LIST later)

### 3. Summary fields on referenced tables
For card display, mark which fields appear on cards:
- Contacts: firstName, lastName, role → summary=true
- Organisations: name, acronym, country → summary=true
- Networks: name, acronym, description → summary=true

### 4. Column roles on referenced tables
For card heading/subtitle:
- Contacts: role for title fields (TITLE, DETAIL, etc.)
- Organisations: role for name/acronym
- Networks: role for name/acronym

### 5. TableRole on nested tables
Already done:
- CollectionEvents: role=DETAIL (hidden from landing)
- Subpopulations: role=DETAIL
- Datasets: role=DETAIL

## Demo Approach

### Option A: Story file (no backend needed)
Create a story with mock metadata + mock data showing the full Resource detail view. Proves the component composition works.

**Pro:** No backend, fast to iterate
**Con:** Doesn't prove real data model config works end-to-end

### Option B: Data model CSV updates + live test
Update the shared Resources/Contacts/Organisations CSVs with HEADING columns and settings, then test in apps/ui with real data.

**Pro:** Proves end-to-end, real validation
**Con:** Requires running backend, data model changes affect all users of shared models

### Recommended: Both
1. Story first (fast feedback loop)
2. CSV updates (proves it works for real)

## Minimum Configuration Checklist
- [ ] HEADING rows in Resources.csv (group fields into sections)
- [ ] Verify display settings on REF_ARRAY/REFBACK columns
- [ ] Summary fields on Contacts, Organisations, Networks
- [ ] Column roles on key tables for card heading resolution
- [ ] Story file: RecordView with mock Resource data + nested tables
- [ ] Test with real data in apps/ui

## Files to Touch
- `data/_models/shared/Resources.csv` — add HEADING rows
- `data/_models/shared/Contacts.csv` — summary fields
- `data/_models/shared/Organisations.csv` — summary fields
- `data/_models/shared/Networks.csv` — summary fields (if exists)
- `apps/tailwind-components/app/components/display/Emx2RecordView.story.vue` — demo story

## Catalogue App (future, not this branch)
For catalogue specifically, a **thin wrapper** around the generic routes would work:
- Catalogue routes map `/{catalogue}/{resourceType}/{resource}` → generic `Emx2RecordView`
- Wrapper provides catalogue-specific context (schema, breadcrumbs, theme)
- No complex nested routing needed — just route params → Emx2RecordView props
- Breadcrumbs built from route params, not from record path structure
