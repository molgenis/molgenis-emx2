# Detail View Like Catalogue — Plan

## Goal
Replace handcrafted catalogue detail pages (~1,950 lines across 5 pages) with a config-driven system using generic building blocks. Same visual quality, 90% less page-specific code.

## Completed

### Phase 1-2: Foundation + RecordView — DONE
RecordView/RecordSection/RecordColumn dumb components. Emx2RecordView smart wrapper. SideNav scrollspy. Catalogue styling. Empty section hiding.

### Phase 3: ListView + Emx2ListView — DONE
Table/cards/list layout modes. Paginated search. REFBACK via Emx2ListView. RecordTable clickable title column. Ontology tooltips. Hide empty columns.

### Phase 4: REF_ARRAY Display + Row Navigation — DONE
REF_ARRAY as Emx2ListView. REF/SELECT/RADIO auto-link. Row click to detail.

### Phase 5: Ontology Tree Display — DONE
OntologyTreeDisplay auto-detects flat vs hierarchical. buildOntologyTree with 14 tests.

### Phase 6: Column Display Properties — DONE
Backend: ColumnRole enum (TITLE/SUBTITLE/DESCRIPTION/LOGO/DETAIL/INTERNAL), DisplayType enum (TABLE/CARDS/LIST), migration32, GraphQL + CSV import/export. Frontend: ListCard with role annotations, displayUtils (16+ tests). Data: HEADING columns + role/display annotations in Resources, Collection Events, Subpopulations.

### Phase 7: Table Role — DONE
TableRole enum (MAIN/DETAIL). DETAIL tables hidden from landing page.

### Phase 9: Polish & Small Fixes — DONE
Logo on cards + sidebar. Comma spacing. List alignment. Double scrollbar fix. ref_array items as links. Title-column-only click in tables. Date/DateTime display. ValueFile as download link. Sidebar [object object] fix. Delete button removed. Subpopulations creator=cards. Datasets variables refback.

## Open Items (this branch)

### Visual Testing (manual)
- [ ] Verify cards, logo, dates, links, variables refback with running app
- [ ] Theme testing (Light, Dark, Molgenis, UMCG, AUMC)
- [ ] Responsive testing (desktop, tablet, mobile)

### Minor Gaps
- [ ] Backend JUnit test for column role/display persistence
- [ ] HEADING columns needed in more catalogue data models (Datasets, Variables, Organisations, Contacts, Networks)

## Deferred (separate PRs)

### DATA_NESTED table type
Currently using TableRole=DETAIL (lighter touch, same UX). DATA_NESTED enum only needed if DETAIL tables require different DB/API behavior.

### Catalogue Detail Pages Demo
Wire Emx2RecordView for catalogue's 5 detail pages. Requires HEADING columns in all catalogue data model CSVs.

### Generic List/Search Page Framework
Emx2SearchPage with auto-generated filters, filter sidebar, URL-based filter state, result cards.

### Out of Scope
Landing pages, variable harmonisation, shopping cart, contact modals.
