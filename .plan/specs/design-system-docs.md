# Spec: Design System Documentation (tailwind-components)

Direction: industry-standard IA, no framework replacement; live editing deferred
to Phase 2. Rationale in `../plans/design-system-docs.md`; rejections in
`../decisions.md`. Rows are not "done" until `Test` links a test that fails on
drift. `Visual` = "visual check" marks items with no automated test.

## Information architecture (industry standard)

```
Get started   — intro, install/usage, how to read these docs, theme switcher
Foundations    — design tokens: Colors · Typography · Spacing · Radius & elevation · Icons
Components     — reusable components, grouped BY PURPOSE (see taxonomy)     (stories)
Patterns       — page-level templates / multi-component flows (samples/, page layouts)
```

Owner mapping: Tokens→Foundations · "smallest building blocks" + organisms→
Components (grouped by purpose, NOT split into atomic tiers) · Templates→Patterns.

## Information architecture — owner-approved 2026-06-25 (IA review v2)

Sibling top-level sections (Carbon model), validated vs GOV.UK/Material3/Carbon/Polaris/Atlassian:

| Section | Holds |
|---|---|
| Get started | content page — EMX2 as a **code-first** design system: install/import + working snippet, prototype-vs-production tracks (design tools secondary) |
| Foundations | Colors · Typography · Spacing · Radius & elevation · Icons · Theme styles · **Theme switch** (moved from Layout) |
| Page templates | page-level templates + samples (was "Patterns") |
| Data visualisation | viz/* (ChartLegend, ColumnChart, PieChart, ProgressMeter) — British spelling |
| Prototypes (NEW) | in-development showcases before they become components/app features |

Component groups (9):

| Group | Maps from (real stories) |
|---|---|
| Actions | Button, ButtonBar, ButtonDropdown |
| Inputs | input/* (incl. **Switch**), FilterSearch |
| Forms | Form, Field, form/* (AddModal, EditModal, Error, RequiredInfoSection), label/Draft, **Legend** (moved from Content) |
| Navigation | Navigation, NavigationNested, BreadCrumbs, Pagination, Header, PageHeader, FooterComponent, NavigationGroups, **Session** (moved from Layout) |
| Feedback | Banner, Message, notification/* |
| Overlays | Modal, SideModal, CustomTooltip |
| Data display | DisplayList, table/*, value/*, text/Hyperlink |
| Content & media | Heading, Paragraph, Image, Logo, LogoMobile, Icons |
| Layout | **Accordion** (moved from Feedback), ShowMore, SlideUp/transition |

> Empty speculative dirs (button/ filter/ field/ breadcrumb/ tabs/ pagination/ menu/
> notification/ content/ display/ value/) dropped — they held no stories. Ungrouped
> safety net still catches anything unmapped.

## Behaviours — Phase 1

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Sidebar nav renders 4 sections (Get started / Foundations / Components / Patterns), not a flat alpha list | `default.vue` (or extracted `DocsNav`) | (to add) unit: nav builder returns the 4-section grouped structure | visual check |
| Each Component story is assigned to exactly one purpose group; unassigned stories surface in a fallback "Ungrouped" bucket (no silent drop) | nav data builder | (to add) unit: every discovered story id appears in output exactly once | visual check |
| Grouping source = `dir`→group lookup + override map; adding a `.story.vue` in a mapped dir needs zero menu edits | nav data builder | (to add) unit: a new dir-less story falls to override/Ungrouped | — |
| Foundations → Colors renders palette + semantic swatches per theme, each with copy-to-clipboard token name | Foundations Colors page + `ColorTile` | (to add) unit: copy handler writes token name to clipboard | visual check (5 themes) |
| Foundations → Typography shows a rendered type scale (heading-* / body-*) with token name + size | Foundations Typography page | (to add) unit: scale list derived from theme font sizes | visual check |
| Foundations → Spacing / Radius & elevation render visual specimens with token names | Foundations pages | (to add) unit: specimen list derived from theme | visual check |
| Token pages are generated from `tailwind.config.js` + CSS-var theme files (not hand-maintained) | token-source reader (build/util) | (to add) unit: reader parses config → token records | — |
| Each story page shows an auto API table: props (name/type/default/required/description), events, slots | new `ApiTable` + `vue-component-meta` build step | (to add) unit: ApiTable renders rows from a meta JSON fixture | visual check |
| API metadata is build-generated from component TS types (no manual tables; regenerates on change) | `vue-component-meta` integration | (to add) build/test: meta JSON for a sample component matches its props | — |
| Story source still shown + copyable, syntax-highlighted | `SourceCode` | (to add) unit: copy button copies source | visual check |
| All themes still switch correctly across new pages | theme switch | (existing) | visual check (Light/Dark/Molgenis/UMCG/AUMC) |
| Every Foundations route renders on client navigation (HTTP 200, no module-load SyntaxError) — token source `tailwind.config.js` must be ESM-importable in the client bundle, not just SSR/vitest | `tailwind.config.js` (ESM) + `designTokens.ts` import | e2e/route smoke (opt-in) — unit env masks client module-format failures | visual check (all 5 pages) |
| Sidebar menu and main content scroll independently; top navbar stays fixed; "scroll to top" scrolls the content region | `default.vue` shell + `Story.vue` content | (to add) — hard to unit-test scroll; covered by visual/e2e | visual check (desktop/tablet) |

## Behaviours — Phase 1.5 (owner review round 2)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Nav is built from ONE declarative ordered config (sections→groups→ordered members; member = explicit route OR dir pattern); group + link order is deterministic, not glob order | `docsNav.ts` config + builder | (to add) unit: builder output order is stable + matches config order | visual check |
| Every discovered story appears exactly once; any story not covered by the config surfaces in a visible "Ungrouped" group (no silent drop) | `docsNav.ts` builder | (to add) unit: a story with no config match lands in Ungrouped; counts conserved | — |
| ApiTable shows expanded type details on hover for complex prop types (union options / object members / array element); the flat type string stays visible | `ApiTable.vue` + floating-vue/CustomTooltip | (to add) unit: ApiTable renders popover content from a fixture prop with an expanded schema | visual check (hover) |
| Generator captures a bounded (~1-level) expanded type schema into the meta map for complex types | `generateComponentMeta.mjs` | (to add) unit: schema extraction on a sample union/object type; depth capped | — |
| Docs chrome (DocsNav, ApiTable, layout, Foundations pages, TokenLabel) reuses design-system components + tokens where applicable; no gratuitous raw HTML | all docs-shell components | (review) reuse audit vs available components | visual check |
| Sidebar is rendered by the real `FormLegend` (form TOC) component — a persistent vertical, sectioned table-of-contents with active-section highlighting — from a `LegendSection[]` builder. NOT hand-rolled, NOT the flyout menu. SUPERSEDES the Phase-1 "4 sections" row: purpose groups + top sections → SECTIONs, links → HEADINGs (Components umbrella stays flattened); conservation + Ungrouped preserved; `goToSection`→router nav; `isActive`=current route; errorCount 0 (no badges) | `docsNav.ts` `buildDocsLegend` + `FormLegend` in `default.vue` | (to add) unit: builder emits LegendSection tree; every story once; Ungrouped on no-match | visual check (matches Legend look; active accent on current page) |

| Each container section (Foundations, the 9 purpose groups, Patterns) has an auto-generated overview page: human title + short description (config) + auto-listed grid of its component cards (links); clicking the section label navigates there; the section shows active when on its overview | section overview dynamic page + `buildDocsLegend` (container ids = overview routes) + section config | (to add) unit: section-overview helper returns title/description/items for a slug; builder container ids = overview routes | visual check |

## Behaviours — Live maps (DX, owner review round 2)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| Story source resolves at RUNTIME via `import.meta.glob(?raw)` (no generated sourceCodeMap.json); editing a `.story.vue` hot-updates the shown source with no restart | SourceCode/Story wrapper | `tests/vitest/utils/sourceCode.spec.ts` — `globKeyToRouteKey` flat + nested | visual check (edit → HMR) |
| `generate-source-map` step + `generateSourceMap.js` removed from build/dev | package.json | (n/a — absence verified) | — |
| componentMetaMap is consumed via direct JSON import (not runtimeConfig) so Vite HMR reloads it on regen | Story.vue | (existing ApiTable/meta tests still green ✓) | — |
| Editing a component regenerates only its meta entry (checker kept alive) and the API table updates without a manual restart | dev-only Nuxt local module `modules/componentMetaWatch.ts` (`builder:watch`, 200 ms debounce) | visual check (unit not feasible: Nuxt module lifecycle) | visual check (edit component → table updates) |
| nuxt.config.ts is NOT edited for any of this (never-committed); dead runtimeConfig map entries left as harmless no-ops | — | confirmed: not staged ✓ | — |

## Behaviours — Phase 2 (follow-on, not in Phase 1 scope)

| Behavior | Component | Test | Visual |
|---|---|---|---|
| A story snippet is editable in monaco and re-renders live in-page | live-edit harness (monaco + Vue runtime compiler) | (to add) | visual check |
| Interactive prop "controls" panel generated from `vue-component-meta` schema drives the live demo | controls panel | (to add) | visual check |

## Non-goals / constraints

- No replacement of the homegrown `.story.vue` system (no Storybook / Histoire).
- No new colors/spacing/radius hardcoded — reuse theme tokens (per
  `frontend-conventions`).
- Don't put app-specific logic in tailwind-components.
- Atoms/Molecules/Organisms NOT used as nav labels.

## Verification

- Review agent checks nav builder + token reader + ApiTable against these rows.
- e2e/visual is opt-in (owner-requested): 5-theme × 3-size matrix on new pages.
