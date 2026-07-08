> SUPERSEDED 2026-06-24 — exploration record only. Live plan:
> `../design-system-docs.md`; decisions: `../../decisions.md`.

# Plan: tailwind-components → Design System Documentation

Goal: evolve `apps/tailwind-components` from a component playground into the EMX2
design-system documentation site — structured IA, a design-tokens section,
atomic-design-*inspired* grouping, and (dream) live code / cheap API-contract
visibility — **without replacing the homegrown story framework.**

## Current state (recon — grounded)

- **Story system**: homegrown. `.story.vue` files auto-discovered via
  `import.meta.glob` (`default.vue:10`, `index.vue`). Each file is a bare demo
  template; the layout wraps it in `<Story :title>` and appends a `<SourceCode>`
  block. ~87 stories, ~253 components. Nuxt 4 / Vue 3.5 / Tailwind 3.4 / TS 5.
- **Menu**: hand-coded sections in `default.vue:80-113` ("Theme Styles", "Sample
  pages", "Components", "Other") + a **flat alphabetical** auto-list of all
  stories (`default.vue:98-105`). The per-story `dir` is computed
  (`default.vue:19-21`) but **not used for grouping** — low-hanging fruit.
- **Tokens**: `Styles.other.vue` already showcases palette + semantic colors via
  `<ColorTile>`. `tailwind.config.js` (v3, JS config) defines palette + ~150
  semantic tokens, fonts, sizes, radius, shadows — all pointing at CSS variables
  in `app/assets/css/main.css` + per-brand theme files (`data-theme` switch:
  light/dark/molgenis/umcg/aumc/hdsu).
- **API contract**: **none automated.** `<SourceCode>` shows the *story* source,
  not the component's props/events/slots. The `Story` component takes only
  `title`/`description` (`Story.vue:2-5`). NB: the `frontend-conventions` skill
  documents a richer `<Story :spec>` markdown block (Props table + Test
  Checklist) that **was never built** — an existing intent we can fulfil.
- **Live editing latent capability**: `monaco-editor` + `nuxt-monaco-editor` are
  **already dependencies** → in-app live editing is far cheaper to reach than a
  greenfield design system would be.

## Field best practices (research — cited)

- **Common IA spine** across Polaris/Carbon/Atlassian/Spectrum/Primer/Material:
  `Get started → Foundations (incl. Tokens) → Components (grouped by purpose) →
  Patterns`. Tokens almost always live *under* Foundations.
- **Atomic design verdict**: use it *conceptually* (primitives → components →
  patterns composition gradient), but **do NOT use Atoms/Molecules/Organisms as
  menu labels.** No surveyed production system navigates by those words; all
  group by *function* (Actions, Inputs, Feedback, Navigation, Data display…).
  Frost himself: "the labels have never been the point." The "is a button an
  atom or molecule?" ambiguity is the classic failure mode.
- **Token docs**: layer primitive → semantic (→ component). Color = swatch grid,
  type = rendered scale, spacing/radius/elevation = visual specimens; **copy-to-
  clipboard the token NAME on everything**; auto-generate from the theme source.
- **API contract, cheapest high-value**: `vue-component-meta` (Volar/Vue team) —
  static, TS-type-derived props/events/slots/exposed as JSON → render our own
  tables. No framework switch, no rot.
- **Live examples**: "rendered demo + copyable source" covers ~90% (we're nearly
  there). True editable = Vue SFC Playground / `vitepress-plugin-vue-repl`, or —
  given monaco is already present — an in-app monaco + Vue runtime compiler.
- **Don't deepen Histoire** (last release Apr 2024, momentum stalled). Keep the
  homegrown system; Storybook 8 is the only "switch" worth considering, and only
  if we later want its addon/controls ecosystem.

## Shared decisions (apply to all proposals)

- IA = `Get started → Foundations → Components (by purpose) → Patterns`.
- Component groups by **purpose** (e.g. Actions, Inputs & selection, Form,
  Feedback & status, Navigation, Layout & content, Data display, Visualization),
  NOT by atomic tier. The atomic gradient is expressed by the
  Foundations/Components/Patterns tiers themselves.
- Tokens page lives under Foundations, generated from the theme where possible.

---

## Proposal A — "Restructure in place" (lowest cost)

Reorganise the existing system; add structure + a real tokens section; no new
build tooling.

- **Menu**: replace the flat alphabetical list with a small declarative
  **manifest** (story → tier + purpose-group + order) or frontmatter, rendered as
  collapsible grouped nav. Add `Get started`, `Foundations`, `Patterns` sections.
- **Foundations**: promote `Styles.other.vue` into a proper Foundations area —
  split into Colors / Typography / Spacing / Radius & shadow / Icons pages, each
  with copy-to-clipboard token names. Mostly hand-curated.
- **API contract**: implement the long-intended `<Story :spec>` markdown block —
  hand-written Props/Events/Slots tables per story. Cheap, but **rots** (manual).
- **Live code**: keep `<SourceCode>` (rendered demo + copyable source), tidy it
  (syntax highlight, copy button).
- **Effort**: S. **Risk**: low. **Downside**: prop tables are manual and drift.

## Proposal B — "Auto-documented design system" (recommended sweet spot)

Everything in A, but the **API contract is automated and rot-proof**.

- **Menu / IA**: same manifest-driven grouped nav as A.
- **Foundations / Tokens**: **auto-generate** the tokens pages by reading
  `tailwind.config.js` + the CSS-variable theme files at build time → swatch
  grids / type scale / spacing specimens, with live theme switching. No manual
  token tables to maintain.
- **API contract**: wire **`vue-component-meta`** into a Vite/build step to emit
  per-component props/events/slots/exposed JSON (from real TS types + JSDoc) →
  render as tables beneath each story. Accurate, zero-maintenance.
- **Live code**: rendered demo + copyable, syntax-highlighted source (as A).
- **Effort**: M. **Risk**: moderate (component-meta build wiring; theme parsing).
  **Highest value-for-effort** — matches the field's recommended path.

## Proposal C — "Live playground / docs platform" (the dream)

Everything in B, plus **true editable live examples**, exploiting monaco already
being present.

- Two routes (pick one):
  - **C1 (in-app, lean)**: monaco editor + Vue runtime compiler so a story's
    snippet is **editable and re-renders live** in-page. Reuses existing
    `monaco-editor`/`nuxt-monaco-editor`; no new docs platform. Add interactive
    **prop controls** generated from the `vue-component-meta` schema (B) — a
    "knobs" panel like Storybook controls.
  - **C2 (adopt Storybook 8)**: switch the docs container to Storybook 8 with
    autodocs (`docgen: 'vue-component-meta'`) + Controls + addon ecosystem.
    Biggest capability jump, but **is** a framework change — contradicts the
    "don't replace the framework" constraint; listed for completeness.
- **Effort**: L. **Risk**: higher (runtime compiler + bundle size for C1; full
  migration for C2). **Payoff**: real interactive API exploration.

---

## Recommendation

**Phase it: ship B, keep C1 as a follow-on.** A is a strict subset of B and the
only thing A saves is the component-meta wiring — but that wiring is exactly what
stops the API tables from rotting, which is the user's stated "API contract"
dream. C1 then layers live editing on top cheaply *because monaco is already
here*. Sequence: A's restructure → B's automation → optional C1 live layer.

Avoid C2 (Storybook migration) unless the team later wants its addon ecosystem —
it violates the "no framework change" constraint.

## DECIDED (product owner, 2026-06-24)

1. **Direction: B then C1.** Phase 1 = Proposal B (grouped IA, auto tokens,
   `vue-component-meta` API tables). Phase 2 = C1 (in-app monaco live editing +
   prop controls) as a documented follow-on. C2 (Storybook) rejected — framework
   change, against constraint.
2. **Purpose-based nav labels** — NOT Atoms/Molecules/Organisms. Atomic gradient
   expressed via Foundations → Components → Patterns tiers.

## Still-open sub-decisions (carry into spec review)

3. Menu grouping source: **recommend** `dir`→group lookup + small override map
   for loose root-level stories (minimal new metadata) vs per-file frontmatter.
4. Final per-group component taxonomy (draft in spec for owner sign-off).

## Next steps

- [done] Write `.plan/specs/design-system-docs.md` with behaviour rows.
- [WAIT] Owner reviews spec → explicit go-ahead → delegate to frontend agent(s);
  review agent verifies against spec.
