# Plan: tailwind-components → Design System Documentation

Decided direction (focused). Full exploration archived in
`archive/design-system-docs-proposals.md`; rejections in `../decisions.md`;
behavioural contract in `../specs/design-system-docs.md`.

## Goal
Evolve `apps/tailwind-components` from a component playground into the EMX2
design-system documentation site — industry-standard IA, a design-tokens section,
purpose-grouped components, and auto-generated API tables — **without replacing
the homegrown `.story.vue` system.** Live editing is a Phase 2 follow-on.

## Information architecture (industry standard)
`Get started → Foundations → Components (by purpose) → Patterns`

| Owner's words | Nav section | Contains |
|---|---|---|
| Tokens | **Foundations** | Colors · Typography · Spacing · Radius & elevation · Icons |
| Smallest building blocks + organisms | **Components** (by purpose) | Actions · Inputs & selection · Forms · Feedback & status · Navigation · Layout & content · Data display · Visualization |
| Templates | **Patterns** | page-level layouts + `samples/` flows |

Atoms/organisms are NOT separate tiers — grouped by purpose under Components.

## Docs shell — DECIDED (owner, 2026-06-24)
Keep `.story.vue` for live component demos + add **`@nuxt/content` (MDC)** for the
narrative pages (Get started, token pages, Patterns) so non-frontend teammates can
author in Markdown + `vue-component-meta` for API tables.
- Rejected alt: pure homegrown Nuxt pages (no Nuxt Content) — lighter but clunkier
  prose authoring.

## Phases
- **Phase 1 (now):** grouped nav/IA, Foundations token pages (auto from theme),
  `vue-component-meta` API tables under each story, copyable source.
- **Phase 1.5 (owner review round 2, 2026-06-25):** three parallel work items —
  (A) DocsNav → single declarative ordered config + Ungrouped safety net
  (`docsNav.ts`, `DocsNav.vue`, `default.vue`); (B) ApiTable expand-on-hover type
  details + generator captures bounded expanded schema (`generateComponentMeta.mjs`,
  `componentMetaTypes.ts`, `componentMetaMap.json`, `ApiTable.vue`); (C) dogfooding
  reuse pass on Foundations pages + TokenLabel (`pages/foundations/*`,
  `TokenLabel.vue`). Files are non-overlapping → run A/B/C in parallel; dogfooding
  for nav/ApiTable folded into A/B. Final: consolidated format+lint+test-ci gate,
  restart dev server, re-verify.
- **Phase 2 (follow-on):** in-app **monaco** live-editing + prop "controls"
  (reuses the already-present `monaco-editor`).

## Tooling decisions
- API tables: **`vue-component-meta`** (Vue/Volar team, actively maintained).
- Tokens: read from `tailwind.config.js` + CSS-var theme files (not hand-kept).
- No Storybook / Histoire. Optional later: Style Dictionary for token export.

## Execution cadence (owner, 2026-06-24)
Run all of Phase 1 autonomously, then STOP for owner's visual review before
Phase 2. Hard stops within Phase 1: commit (stage only), genuine design forks,
test-vs-spec conflicts. Monitor reports progress; owner can interrupt anytime.

## Status / next
- [done] proposals + research, decisions log, spec.
- [done] owner confirmed docs shell (Nuxt Content/MDC) + 8-group taxonomy + cadence.
- [done] Phase 1 implemented + staged: grouped nav (DocsNav/docsNav.ts), Foundations
  token pages (colors/typography/spacing/radius-elevation/icons + designTokens.ts +
  TokenLabel), ApiTable + generateComponentMeta + componentMetaMap, Nuxt Content
  pages (get-started, patterns), + 4 spec test files.
- [done] verified 2026-06-25 (post-reboot): `pnpm format` exit 0, `pnpm lint`
  (typecheck) exit 0, `pnpm test-ci` 44 files / 222 tests pass, exit 0. Cold-start
  hook timeout fixed via `vitest.config.ts` `hookTimeout: 30000` (infra only).
- [done] owner-review round 1 (2026-06-25): fixed 5 dead Foundations routes
  (tailwind.config.js CJS→ESM) + made sidebar/content scroll independently
  (default.vue/Story.vue + scrollToTop). Playwright-verified: all 5 Foundations
  pages render w/ no client SyntaxError; content scrollTop moves while sidebar &
  window stay at 0; navbar fixed; scroll-to-top works. Staged.
- [done] Phase 1.5 implemented (A/B/C parallel) + consolidated gate green
  (2026-06-25): `pnpm format` (4 files), `pnpm lint` exit 0, `pnpm test-ci`
  45 files / 246 tests pass exit 0. 29 files staged; nuxt.config.ts excluded.
- [done] Phase 1.5 live-verified (Playwright): (A) nav renders exactly the 8
  taxonomy groups in deterministic order, no dead links, no Ungrouped (all stories
  assigned), no empty groups — confirmed against SSR HTML after an e2e mis-parse
  flagged phantom "UUID"/"EMX2" groups (those are story LINKS in Inputs/Data
  display, not groups); (B) ApiTable hover popover shows expanded type on complex
  props (`DatasetRow[]` → "element: DatasetRow"), absent on simple types; (C)
  Foundations Button-based TokenLabel copy fires aria-live "Copied", all 5 pages
  render.
- [done] review round 2 fixes (2026-06-25): tailwind ESM dead-links; independent
  scroll; viz API-table key doubling; ApiTable type hover; Foundations/TokenLabel
  dogfood; SourceCode path fix; Layout/Content split; live maps (runtime ?raw source
  + meta watcher module); sidebar rebuilt on real `NavigationMenuItems` menu
  component (Bebas Neue + flyouts) — all Playwright-verified.
- [note] sidebar reversal: 4-section IA flattened — purpose groups promoted to
  top-level flyouts, "Components" umbrella dropped (owner-chosen preview). Owner may
  switch to nested "Components ▸" on review.
- [done] sidebar = FormLegend TOC (active-state fixed: plain-boolean isActive +
  computed legendSections; only current page + its group highlight). Section overview
  pages (`/section/<slug>`, auto-listed component cards) added; cards reuse the
  `Button` component (outline tokens, no ad-hoc). "Inputs & selection" → "Inputs"
  (slug /section/inputs). Verified via curl/grep (per new cheapest-check policy).
- [done] CLAUDE.md: added verification-ladder + e2e-as-last-resort policy, and a
  "reuse components before ad-hoc UX" subagent rule + review-checklist item.
- [done] IA review v2 (2026-06-25): re-grouped to 10 component groups + sibling
  sections per research (GOV.UK/Material/Carbon/Polaris/Atlassian); ThemeSwitch→
  Foundations, Session→Navigation, Accordion→Layout, Legend→Forms; Feedback/Overlays
  split; Patterns→"Page templates"; "Data visualisation"; empty dirs dropped; NEW
  Prototypes section; Get started → code-first content page. 284 tests; verified via
  curl/grep (8 new /section routes 200, sidebar regrouped, no Ungrouped leakage,
  code-first content renders). TODO in get-started.md: replace relative import w/ a
  package alias if tailwind-components gets published.
- [done] IA v3 + EMX2 + nav model (2026-06-25): Switch→Actions, PageHeader/Footer→
  Layout, new Pages group, Content&media→Display, Data-visualisation→Visualisation,
  PatternsOverview/+/patterns page deleted; NEW EMX2 section (12 backend-coupled:
  Form/Field/Input/modals/Ref/RefBack/RefSelect/Ontology/table*/Session) via two-pass
  claiming; Forms dissolved (helpers→Feedback, Legend→Navigation). NAV MODEL: sidebar
  = section names only (buildDocsSidebar) + per-page SectionNavBar (Button-based,
  active=aria-current) via getSectionNavForRoute. 327 tests; verified curl/grep
  (section routes 200, EMX2 lists smart comps, Button.story bar shows Actions siblings
  +1 active, get-started no bar, no Ungrouped). Out of scope (owner): move component
  files into section folders.
- [todo] still owed: (1) live-maps WATCHER HMR-on-edit proof (static parts verified;
  edit-a-component→table-updates not yet demonstrated live); (2) Phase 1.6 TypeDoc
  Types reference (decided, queued); (3) final consolidated format+lint+test-ci gate
  before commit.
- [done] merge `origin/master` (2026-07-02): branch synced (0 behind). Only default.vue
  conflicted → kept docs-nav, DROPPED master's app-shell (Header/Nav/Footer/AccountMenu);
  merge commit `3d17214b3`; 885 tests pass. New master stories placed; then owner
  corrections: Skeleton→Containers, `filter`→own "Filter" group (11 groups now). 889
  tests pass, staged (docsNav.ts + spec). See decisions.md 2026-07-02.
- [OPEN] app-shell reconciliation (owner unsettled): docs-nav-only vs wrap master's
  header/footer/account chrome around the docs sidebar. Push pending (13 ahead of origin).
- [WAIT] owner visual review of Phase 1 + 1.5. Then decide on Phase 2 (monaco
  live-edit + controls panel).
- [note] componentMetaMap.json is generated by `generate-component-meta` and churns
  on every `pnpm dev` (e.g. trailing newline); staged copy = format-normalized.
