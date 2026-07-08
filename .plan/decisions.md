# Decisions Log — Design System Docs

Append-only. Each entry: WHAT was decided/changed + WHY + owner + date. The
*current* WHAT lives in `specs/design-system-docs.md`; this log captures
rejections and direction changes the spec can't show.

## 2026-06-24 — IA uses industry-standard terms (owner)
DECIDED: nav spine = `Get started → Foundations → Components (grouped by purpose)
→ Patterns`; tokens live under Foundations. Owner first sketched a 4-tier atomic
model (tokens / smallest building blocks / organisms / templates) but chose to
"follow industry standards" to prevent confusion later.
- "Smallest building blocks" + "organisms" are NOT separate nav tiers — both live
  under **Components**, grouped by purpose (simple→complex via ordering).
- "Templates (how pages look)" → **Patterns**.
WHY: every surveyed system (Material 3, Carbon, Polaris, Atlassian, GOV.UK) groups
components by function; none navigate by atom/molecule/organism. Avoids the
"is a button an atom or molecule?" ambiguity.

## 2026-06-24 — Rejected: literal Atoms/Molecules/Organisms nav labels (owner)
WHY: no production design system uses them as navigation; Frost himself: "the
labels were never the point." The composition gradient is kept conceptually via
the Foundations→Components→Patterns tiers.

## 2026-06-24 — Rejected: Proposal A (hand-written prop tables)
A = restructure + manual `:spec` prop tables. Superseded by B: manual tables rot;
`vue-component-meta` yields type-accurate tables for ~the same effort. A retained
only as fallback if component-meta wiring proves impractical.

## 2026-06-24 — Rejected: Storybook / Histoire as the platform (owner)
Owner dislikes Storybook (too heavy); a Storybook migration (old "C2") is a
framework change, against the "don't replace the framework" constraint. Histoire
considered — closest to our `.story.vue` ergonomics and the likely referent of
"lighthouse" — but rejected as the SOLE platform: perpetual beta
(`1.0.0-beta.1`, Jan 2026; last stable `0.17.x`, Apr 2024), single maintainer,
flat story-tree fights the 4-tier IA.

## 2026-06-24 — "Lighthouse" is not a design-system framework
The name owner heard does not exist as a component-docs tool; it resolves to
Google Lighthouse (perf/SEO/accessibility auditing). Closest real Vue tool =
Histoire (rejected above).

## 2026-06-24 — Docs shell = homegrown .story.vue + Nuxt Content (MDC) (owner)
Component demos stay in `.story.vue`; narrative pages (Get started, token pages,
Patterns) authored in Markdown via `@nuxt/content` (MDC) so non-frontend
teammates can contribute. API tables via `vue-component-meta`. WHY over pure
homegrown .vue: prose authoring is far cheaper in Markdown; Nuxt Content is an
additive official module, not a framework swap.

## 2026-06-24 — Component taxonomy approved (owner)
8 purpose groups locked: Actions · Inputs & selection · Forms · Feedback & status
· Navigation · Layout & content · Data display · Visualization. Ambiguous items
(Accordion, Modal) settled during build.

## 2026-06-25 — Fix: Foundations pages 500'd; tailwind.config.js → ESM (owner-requested fix)
Owner review found all 5 Foundations routes dead (500). Cause: `designTokens.ts`
default-imports `tailwind.config.js`, which was CommonJS (`module.exports`). Vite's
SSR/vitest transform interops CJS→ESM (so the 25 designTokens unit tests passed),
but the CLIENT bundle gets no `default` export → SyntaxError → 500. The config is
pure data (no internal `require()`), so converting `module.exports = {…}` →
`export default {…}` is the minimal correct fix and keeps a single token source.
WHY not a separate token module: would duplicate the source of truth. Test-gap
noted: unit tests run in the SSR-transformed env that masks client module-format
failures — Foundations-render is now guarded by an opt-in e2e/route smoke + visual.

## 2026-06-25 — Independent menu/content scroll (owner)
Owner wants the sidebar menu and main content to scroll independently (navbar fixed
at top). Layout shell (`default.vue`) capped to viewport height below the 54px
navbar; sidebar + content each own `overflow-y-auto`. `scrollToTop` retargeted from
`window` to the content scroll container.

## 2026-06-25 — Phase 1.5 (owner review round 2): nav config, type hover, dogfooding
Owner found: API tables missing for 4 viz components (fixed, see ProgressMeter
entry below), the DocsNav grouping felt "indirect/random", complex TS prop types
shown poorly, and the docs chrome barely reuses the design system. Three decisions:
- **DocsNav → single declarative ordered config + Ungrouped safety net.** Replace
  the 3 lookup maps (DIR_TO_GROUP / STORY_NAME_TO_GROUP / PAGES_SUBDIR_TO_GROUP)
  with ONE ordered config (sections→groups→ordered members; members = explicit
  route OR dir pattern), deterministic order. Auto-discovery kept ONLY as a safety
  net: any story not covered surfaces in a visible "Ungrouped" (never silently
  dropped). WHY: readable/predictable structure without losing zero-menu-edit adds.
  Rejected full hand-authored JSON: reintroduces drift (dead/missing links).
- **ApiTable → expand-on-hover type details.** Generator captures vue-component-meta
  expanded schema (~1 level: union options / object members / array element type);
  ApiTable shows it in a hover popover reusing existing floating-vue/CustomTooltip;
  flat type string stays visible. Cap expansion depth to avoid meta-map bloat.
  WHY: flat strings hide complex shapes (e.g. DatasetRow[], unions).
- **Dogfood the docs chrome.** Reuse design-system components + tokens in DocsNav,
  ApiTable, layout, Foundations pages, TokenLabel where they fit. Exception: the
  data-bound EMX2 Table components don't suit a static props table, so ApiTable may
  stay a token-styled raw table. WHY: a design system should use its own components.

## 2026-06-25 — Phase 1.6: complex TS types → Types reference page + hyperlinks (owner)
Owner: the 1-level hover doesn't show object structure. Chose "type reference +
links (+ keep hover)" over deeper inline hover. KEY FINDING: the viz types owner
saw (DatasetRow, PieChartData, ColorPalette) are `Record<string, …>` ALIASES with
no named fields — deeper structural hover yields nothing for them. A reference can
document aliases (show the RHS) AND deep/circular interfaces via LINKS (no meta-map
bloat). TypeDoc-style standard. Build details:
- Registry built via **TypeDoc `--json`** (industry standard; vue-component-meta
  resolves types structurally and loses alias name + JSDoc). Considered + rejected:
  hand-rolled TS compiler API (no new dep but we own every edge case — generics,
  intersections, JSDoc) and API Extractor / Rush stack (enterprise-grade but
  overkill for an internal site). Owner chose TypeDoc 2026-06-25. A build step runs
  TypeDoc on `types/**/*.ts`, then MAPS the verbose reflection JSON to a lean
  `typeRegistry.json` (name, kind alias|interface|enum, definition/RHS, fields
  [name/type/optional/description], extends bases, JSDoc, source). If the set is
  very large, filter to types referenced (transitively) by component meta; log
  inclusions. NOTE: most types currently lack TSDoc comments, so the reference
  shows signatures/structure now and gets richer as `/** */` is added at source.
- Consumed via DIRECT JSON import (not runtimeConfig) so we don't edit the
  never-committed `nuxt.config.ts`.
- New `/reference/types` page (single page, per-type `#Name` anchors); new nav
  "Reference" section. API table type cells hyperlink known type names to the anchor;
  hover keeps 1-level detail + a "→ full definition" link.
- Rejected (this round): deeper inline hover-only — does nothing for Record aliases,
  risks huge/circular tooltips (EMX2 table types), grows meta map.

## 2026-06-25 — Fix: "Show source code" empty everywhere (path mismatch)
`generateSourceMap.js` writes `app/sourceCodeMap.json` (its `__dirname` = `app/`
post Nuxt-4 srcDir migration), but `nuxt.config.ts` reads `resolve("./sourceCodeMap.json")`
= app ROOT (where `componentMetaMap.json` correctly lives). Read missed → runtimeConfig
fell back to `{none:"none"}` → SourceCode showed nothing on every page (API tables
unaffected — that map is at root). FIX: align generator output to the app root
(do NOT touch the never-committed `nuxt.config.ts`). Pre-existing migration bug,
surfaced during owner review. NOTE: SourceCode also renders on non-story pages
(Foundations) where no `.story.vue` source exists → those legitimately show "No
source code found".

## 2026-06-25 — Taxonomy: split "Layout & content" into "Layout" + "Content" (owner)
Owner: they should be separate nav groups. Now 9 purpose groups. Proposed split
(owner to confirm membership): **Layout** (structure/disclosure/motion/chrome) =
`transition/*`, ShowMore, SlideUp, ThemeSwitch, Session; **Content** (page content)
= `content/*`, pages Banner/Heading/Paragraph, Image, Logo, LogoMobile, Legend,
Icons. Group order: …Navigation, Layout, Content, Data display, Visualization.

## 2026-06-25 — Live maps: runtime source (?raw) + dev watch-regen for meta (owner)
Owner wants maps to auto-update on component change without manual regen/restart.
Chosen approach (over "source-only quick win", a full virtual-module plugin, or
defer):
- **Source**: drop generated `sourceCodeMap.json` entirely; resolve story source at
  RUNTIME via Vite `import.meta.glob("../pages/**/*.vue", { query: "?raw", import:
  "default" })` inside the SourceCode/Story wrapper → full HMR, no generate step,
  removes the path-mismatch bug class. Remove `generate-source-map` from
  package.json dev/build + delete `generateSourceMap.js`.
- **Meta (types)**: vue-component-meta is a Node/tsc tool — CANNOT run in browser
  (runtime `component.props` loses TS types). So keep build-time generation but make
  it live: Story reads `componentMetaMap.json` via DIRECT import (not runtimeConfig)
  so Vite HMR reloads it; a dev-only Nuxt LOCAL module (`modules/`, auto-registered
  — no nuxt.config edit) watches `app/components/**/*.vue` (`builder:watch`) and
  regenerates on change. Keep the checker instance alive + patch only the changed
  component's entry (avoid slow full re-gen). Production `nuxt generate` still bakes
  the JSON at build time.
- Constraint that shaped this: `nuxt.config.ts` is never committed (local API-base
  override), so we must avoid editing it — hence local `modules/` + direct import;
  the now-dead `runtimeConfig.public.sourceCodeMap/componentMetaMap` entries are left
  in the uncommitted nuxt.config as harmless no-ops.

## 2026-06-25 — Sidebar adopts the real menu component (flyouts) (owner)
Owner was baffled the docs sidebar didn't reuse the app menu: `DocsNav.vue` was
hand-rolled with generic type (`text-2xl font-bold`, `text-sm uppercase`,
`text-title`) while the real Molgenis menu (`NavigationMenuItems.vue`) uses
`font-display` (Bebas Neue) + `text-heading-xl` + `text-menu`/`text-sub-menu`.
DECISION: adopt `NavigationMenuItems` (the vertical list + right-flyout submenus)
for the docs sidebar — true dogfooding. Build a `MenuItem[]` from the docs nav
config and render via NavigationMenuItems/NavigationMenuLink; retire the hand-rolled
`DocsNav.vue`.
- REVERSAL of the Phase-1 "sidebar renders 4 sections" behavior: per the owner's
  chosen preview, the 8 purpose GROUPS are promoted to TOP-LEVEL flyout items
  (Get started, Foundations▸, Actions▸…Visualization▸, Patterns▸), 1-level flyouts.
  The "Components" umbrella label is dropped from the nav. Owner to confirm vs a
  nested "Components▸" (2-level) on review.
- `Navigation.vue` (horizontal top-bar w/ overflow) is NOT used — wrong layout.
- Conservation + Ungrouped safety net carry over to the MenuItem builder.

## 2026-06-25 — Sidebar: use the Legend (form TOC) component, NOT the flyout menu (owner)
Owner clarified "I meant the legend component not this one" — the docs sidebar
should be built from the `form/Legend.vue` pattern (a persistent vertical, sectioned
table-of-contents with active-section highlighting), NOT the `NavigationMenuItems`
flyout menu (my prior rebuild was a misread of "did you use our menu sidebar").
REPLACES the NavigationMenuItems flyout sidebar.
- Reuse `FormLegend` AS-IS (no edits to the shared form component / its sub-pieces /
  the metadata-utils `LegendSection`/`LegendHeading` types). Map docs nav →
  `LegendSection[]`: purpose groups + top sections → SECTIONs, their links →
  HEADINGs. `goToSection(id)` handler → router navigation (id = route); `isActive`
  = current route (drives the accent bar); `errorCount: computed(()=>0)` so no error
  badges render; `isVisible: computed(()=>true)`.
- Caveats (surface on review): group SECTION headers are non-navigating containers
  but FormLegend renders every row as a clickable link → clicking a group is a
  no-op; CSS `capitalize` slightly alters label casing ("Get started"→"Get Started");
  FormLegend brings its own `bg-form-legend` background. If the no-op group rows are
  unwanted, we'd GENERALIZE Legend to support links/containers (touches shared form
  component) — deferred.
- 4-section IA stays flattened (groups at top level), consistent with the prior
  flyout decision. buildDocsMenu/NavigationMenuItems usage removed.

## 2026-06-25 — Section overview pages, auto-generated (owner)
Owner: clicking a section label (Foundations, the 9 purpose groups, Patterns) does
nothing — wants each section to have a page explaining what it contains. Chosen
(over hand-authored Markdown or Foundations-only): AUTO-GENERATED overview pages.
One dynamic route renders each section's overview = human title + a short
description (from a small config) + an auto-listed grid of the components it
contains (cards linking to each). Container-section ids in `buildDocsLegend` are
wired to these routes so the label navigates; the section highlights active on its
overview. Covers Foundations + 9 groups + Patterns. Get started / Data fetching
already navigate (unchanged). Markdown-override of the prose can come later.
- Open detail for the agent: keep section overview titles HUMAN-readable despite the
  Story wrapper's route-derived auto-title (special-case `/section/` in default.vue's
  storyName, or otherwise) and avoid a spurious empty API table on overview pages.

## 2026-06-25 — IA review v2: re-grouped taxonomy + code-first Get started + Prototypes (owner)
After owner review + research of GOV.UK / Material 3 / Carbon / Polaris / Atlassian
(see `.plan/notes` not kept; summary: "Data display" is the standard name not bare
"Display"; Layout & Content stay SEPARATE but de-grab-bagged; toggles live in
Inputs; Patterns/Data-viz/experimental are SIBLING sections (Carbon model);
code-first intro = GOV.UK/Carbon install-first model). Inventory finding: many
configured dirs were EMPTY (button/filter/field/breadcrumb/tabs/pagination/menu/
notification/content/display/value) — dropped.
APPROVED IA:
- Sibling top sections: Get started (code-first content page) · Foundations (+ Theme
  switch moved in) · Page templates (was "Patterns") · Data visualisation (British
  spelling) · **Prototypes** (NEW — in-dev showcases before they become components/
  app features; owner picked "Prototypes" over Labs/Experimental).
- 9 component groups: Actions · Inputs (incl. Switch) · Forms (+ Legend) · Navigation
  (+ Session) · Feedback (Banner, Message) · Overlays (Modal, SideModal, CustomTooltip)
  [owner: split Feedback & Overlays into TWO] · Data display · Content & media · Layout
  (Accordion, ShowMore, SlideUp).
- Re-assignments fixing grab-bags: ThemeSwitch Layout→Foundations; Session
  Layout→Navigation; Accordion Feedback→Layout; Legend Content→Forms.
- Get started: rewrite as a content page stating EMX2 is a "code-first design system"
  — install/import + working snippet, prototype-vs-production tracks, design tools
  secondary (GOV.UK/Carbon model).
- SUPERSEDES the earlier "Layout & content split" + flyout/menu decisions' taxonomy.

## 2026-06-25 — IA v3 tweaks + nav model: sidebar=sections, in-page section button bar (owner)
IA tweaks:
- **Switch** (input/Switch) → **Actions** (owner override; was Inputs).
- **PageHeader**, **FooterComponent** → **Layout** (were Navigation).
- NEW **"Pages"** group = pages/Heading, pages/Paragraph (+ pages/Banner — INTERPRETED;
  owner said "header, paragraph", I read "header"=Heading and folded the 3rd pages/*
  block Banner in too — flag for correction).
- **"Content & media" → renamed "Display"** (slug display) = Image, Logo, LogoMobile,
  Icons (Heading/Paragraph/Banner moved to Pages).
- **"Data visualisation" → "Visualisation"** (slug visualisation).
- **PatternsOverview deleted**: remove the "Patterns overview" link from Page templates;
  delete the orphaned /patterns content page (content/patterns/index.md + pages/patterns)
  — superseded by the /section/page-templates auto-overview.
NAV MODEL CHANGE (owner proposal):
- **Sidebar shows ONLY section names** (no nested component links). Each section label
  links to its overview (/section/<slug>, /get-started, etc.); the section highlights
  active when you're on any page within it.
- **Every page gets a top "section nav bar"** — a Button-outline list (like the
  Foundations overview grid) of the items relevant to the CURRENT section, with the
  current page's button active. Driven by a reverse-lookup `getSectionNavForRoute`.
  Pages with no section (Get started, Data fetching) show no bar.
- OUT OF SCOPE (owner, later): moving component files into folders matching their
  section.

## 2026-06-25 — NEW "EMX2" section for backend-coupled (smart) components (owner)
Scan classified 12 backend-coupled components (fetch metadata/GraphQL or take
schema/table/ontology props) vs ~55 pure-presentational. NEW top-level **EMX2**
group/section holds the smart ones; dumb components stay in their purpose groups
(so they could ship standalone).
- **EMX2** = Form, Field, Input (dispatcher), form/EditModal, form/DeleteModal,
  input/Ref, input/RefBack, input/RefSelect, input/Ontology, table/TableEMX2,
  table/CellEMX2, **Session** (owner: backend/auth-coupled → EMX2, not Navigation).
- **"Forms" group dissolved**: smart bits (Form, Field, modals) → EMX2; dumb helpers
  **Error, RequiredInfoSection, label/Draft → Feedback** (owner; they're
  status/validation, not media-"Display"). **Legend** (dumb, props-driven form-section
  TOC) → **Navigation** (it's a sectioned nav). 
- Inputs now = dumb inputs only (Ref/RefBack/RefSelect/Ontology/Input removed →
  EMX2). Data display loses TableEMX2/CellEMX2 → EMX2. Navigation loses Session → EMX2.
- IMPLEMENTATION: EMX2 members are route-based (nested stories). buildDocsLegend must
  claim ALL explicit (route/storyName) members across groups BEFORE dir-members
  (two-pass), so EMX2's route-members win over Inputs' {dir:"input"} regardless of
  display order. EMX2 displayed as last component group (adjustable).

## 2026-06-25 — Nav model REVERSAL: accordion sidebar, not the section button-bar (owner)
Owner disliked the "sidebar=sections only + top-of-page SectionNavBar" model. NEW:
**accordion sidebar** — sidebar lists all section labels; only the ACTIVE section is
expanded to show its sub-items (e.g. click Foundations → see Colors/Typography… in
the legend). Implementation: `buildDocsSidebar` keeps headers ONLY for the active
section (section.isActive), empties the rest; FormLegend renders all labels +
expanded active section. Clicking a section navigates to its overview AND expands it
(navigate-to-expand, chosen over a pure no-nav toggle for simplicity — flag).
REMOVES SectionNavBar (the per-page top button-bar) + its spec. Supersedes the
2026-06-25 "sidebar=sections + in-page bar" decision.

## 2026-06-25/26 — Editable per-demo source; source = demo block; @nuxt/content via module
- **Per-demo source + editable live demo** (`Demo.vue` + `compileTemplate.ts` via
  `@vue/compiler-dom`, `demoSource.ts`): each demo block has a subtle text toggle,
  a copy button (`useClipboard`), and a Monaco editor; editing live-recompiles and
  re-renders the demo above (template-only demos; graceful inline error otherwise).
  Runtime-compiled demos resolve REAL styled components via a components-map (Nuxt
  auto-imports aren't globally registered at runtime). Monaco is lazy
  (`defineAsyncComponent`) so it doesn't poison the vitest env.
- **Source belongs to the demo block, not page chrome** (owner): the page-level
  read-only SourceCode is being replaced — `Story.vue` treats each page as one demo
  block by default (editable source from the `<template>` body); pages opt into
  multiple `<Demo>` blocks to split (Button has 8). Every page must show a source
  link. [all-pages rollout in progress]
- **@nuxt/content via a committed local module** (`modules/content.ts`
  `installModule`) so the branch builds on a clean checkout WITHOUT committing
  `nuxt.config.ts` (per the never-commit-nuxt.config rule). Removed @nuxt/content
  from local nuxt.config to avoid double-registration.
- **Pages group dissolved into Display**; `table/EMX2`→"Table",
  `table/modal/Ref`→"Table ref"; non-Foundations sections alphabetised.
- KNOWN DEBT (out of scope): 23 pre-existing lint (typecheck) errors in untouched
  infra files (`fetch*`, `useSession`, `cms`, `fetchSetting`, a few old stories) —
  loose `unknown`-typed fetch responses; our design-system work is lint-clean.
- Commits: `ad93b7708` (IA overhaul) + `a65aad14c` (editable demo + content module
  + Display merge + nav fixes).

## 2026-06-26 — 3-level accordion sidebar (extend FormLegend) + IA reshape (owner)
Sidebar restructured to a 3-level tree, top-level = **Foundations** / **Components** /
**Examples & prototypes** (Get started REMOVED — nav + page/content). Components →
groups → components. New/renamed groups: **Containers** (was "Layout": Accordion,
ShowMore, SlideUp) and **Page layouts** (Header, PageHeader, FooterComponent, "Page
banner"). Examples & prototypes = sample flows (Row edit, Edit modal) + prototypes.
- **Approach: extend FormLegend to 3 levels** (chosen over custom nav / catalogue
  TreeNode after investigation). Added optional `LegendHeading.children` +
  `collapsible` + `expandAll` props; BACKWARD-COMPATIBLE (forms use collapsible=false
  → unchanged, verified by form regression tests).
- **Interaction (owner):** in collapsible mode a NODE (has children) row click
  folds/unfolds and does NOT navigate (overview pages are empty in practice); only
  LEAVES navigate. Active route auto-expands its path. Search force-expands the
  filtered tree so matches are revealed (e.g. "button" → Components→Actions→Button).
- `buildDocsTree(paths, currentPath, query)` builds the 3-level tree (isActive on
  leaves; query prunes to matching branches keeping ancestors).
- ORPHANED (flagged for cleanup): the `/section/<slug>` overview pages + getSectionOverview
  are now unreachable from the nav (nodes don't link to them). SourceCode.vue also
  unused (per-demo/page source replaced it).
- Resource note: kill stray `nuxt dev` processes — accumulation OOM-killed the dev
  server (exit 144) during verification.
- Commit: `c2f93cde1` (all-pages source) → this 3-level-nav commit.

## 2026-07-02 — Merge master in: app-shell dropped for docs-nav; Filter group; new-story placement (owner)
Synced `origin/master` into the branch (`git merge`, per convention not rebase).
`.gitignore`/`package.json`/`tailwind.config.js` auto-merged; only `default.vue`
conflicted and master's other-app commits merged clean (merge commit `3d17214b3`).
- **App-shell DROPPED (default.vue conflict resolution).** Master (`e133688b2` era)
  had replaced tailwind-components' layout with a full app-shell — `Header`,
  `Navigation`, `FooterComponent`, `AccountMenu`, auth via `useLayoutState`/
  `isSignedIn`. The merge KEEPS our 3-level docs-nav layout and drops master's
  app-shell (kept master's additive `BackgroundGradient`). WHY: two incompatible
  layouts can't coexist in one `default.vue`; the branch's whole purpose is the
  docs-nav. OPEN (owner, unsettled): optionally reconcile by wrapping master's
  Header/Footer/AccountMenu chrome AROUND the docs sidebar (common docs-site shape)
  vs staying docs-nav-only. IMPLICATION: a future squash-PR to master would overwrite
  master's app-shell — a team discussion, flagged.
- **New master stories placed.** Master added `Well`, `Skeleton`, `filter/FilterSystem`
  after divergence → they hit Ungrouped (5 test failures). Placed: `Well`→Display,
  `Skeleton`→Feedback initially, `filter/*`→EMX2 initially. Fixture counts 72→75.
- **Owner corrections (2026-07-02):** `Skeleton` → **Containers** (not Feedback);
  **`filter` becomes its OWN group** ("Filter") — `FilterSearch` pulled out of Inputs,
  `filter/FilterSystem` pulled out of EMX2. Components order now 11 groups: Actions ·
  Inputs · **Filter** · Feedback · Overlays · Navigation · Display · Containers · Page
  layouts · Visualisation · EMX2. Tests: 889 pass, exit 0.
- Housekeeping: `componentMetaMap.json` re-flagged as a generated artifact that churns
  every `pnpm dev` — should be **gitignored** (queued cleanup); the `Demo.vue`/
  `demoSource.spec.ts` prettier churn committed as `6e55bdbf9`.

## 2026-06-24 — Dead / SaaS tools eliminated
Pattern Lab (archived May 2026), Backlight (shut Jun 2025), Specify (shut Nov
2024), Ladle (React-only). SaaS hubs (zeroheight / Supernova / Knapsack)
rejected: don't render our live Vue components; add cost/lock-in.
KEEP as complementary: `vue-component-meta` (API tables); Style Dictionary
(optional, only if multi-platform token export is needed later).
