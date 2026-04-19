# MOLGENIS EMX2 - Project Conventions

### CRITICAL: Always Use Subagents for Implementation
**Main thread = lead agent.** Never implement directly in main thread.
**Lead agent MUST NOT**: edit/write files, run gradle/pnpm/build/format/lint/test commands, or use Bash for implementation tasks. Always delegate to backend/frontend/review/e2e subagents.
**Tests define expected behavior. Test first. Changing tests = changing requirements.**

# Communication
- Be extremely concise, sacrifice grammar for brevity
- Don't show diffs - just report done, user reviews themselves
- After code changes, run tests immediately; only show code if tests fail and feedback needed

# Git Workflow (ALL projects)
- Never commit to 'master'
- **CRITICAL: Stage only, never commit without user permission**
- **Never commit `nuxt.config.ts`** — contains local dev overrides (e.g. API base URL)
- **CRITICAL: Always `git add` changed files before reporting done**
- **CRITICAL: NEVER add `Co-Authored-By` lines to commit messages**
- **Always run `pnpm format` and `pnpm lint`** on any touched app before committing. Fix pre-existing errors too.
- User reviews staged changes with `git diff --cached`
- **Before reporting done**: run `git diff master...HEAD --name-only` and `git diff --cached --name-only` to verify no unrelated changes crept in. Flag anything off-topic.

Without product owner confirmation, NEVER:
- Remove or rename test files/describe blocks
- Change test assertions to match new (broken) behavior
- Weaken expectations (e.g., `.toBe(true)` → `.toBeTruthy()`)
- Remove assertions from existing tests

**If tests fail after code changes**: Fix the CODE, not existin tests. If tests are wrong, ask first.

**Red-green testing**: When fixing bugs, write a failing test FIRST (red), verify it fails, then fix the code and verify the test passes (green). This ensures the bug is reproducible and the fix is verified.

**Preserve existing comments**: Never delete comments already in a file unless the associated code is also changed/deleted. This includes rename/replace-all refactors — verify comments survive.

**Prefer testable pure functions**: Extract logic into pure functions/composables for easy unit testing. Enables red-green test approach.

# Plans & Specs
- Use `.plan/plans/` and `.plan/specs/` for branch-specific plans; `.claude/plans/` is for plan mode only (system-assigned filenames, not configurable).
- `.claude/` is symlinked to master — no branch-specific files there.
- After plan mode exits: copy final plan to `.plan/plans/`, then **wait for explicit instruction** before writing any code.
- **Spec files are living guardrails** — table format: `| Behavior | Component | Test | Visual |`
    - `Test` links to the validating test; `Visual` marks manual-check items (use "visual check" if no automated test exists).
    - When closing a task: move acceptance criteria into the spec; keep plan task entries brief.
- **Visual verification**: After implementation, lead spawns an e2e agent to check visual spec items via Playwright; review agent checks template/CSS logic against spec.


# System Constraints
- Stay within 16Gb memory when running tools
- Don't leave tools running when done
- **CRITICAL: NEVER read/write files outside `/Users/m.a.swertz/git/` folder (exception: `~/.claude/`)**

# Agents
Use subagents: lead, ci, backend, frontend, e2e, review, datamanager, monitor, scout

| Agent | Role |
|-------|------|
| lead | Coordination, user interface. NEVER runs commands, reads code, or edits files directly — delegates ALL work to other agents. Uses Explore agent for research/codebase investigation, then updates plans based on findings |
| backend | Java implementation, JUnit tests |
| frontend | Vue/Nuxt implementation, vitest, story files |
| review | Code review, quality check, pattern compliance |
| e2e | Playwright test engineer, smoke tests |
| scout | Codebase exploration, read-only research (haiku) |
| ci | CircleCI status checker, failure logs (haiku) |
| datamanager | Data model review, docs, examples (haiku) |
| datamodeler | Data models, demo data, python scripts (sonnet) |
| monitor | Polls background tasks, reports progress (haiku) |

Pattern for implementation tasks:
```
1. Lead: spawn Explore agent to research codebase, read plan/spec files, investigate current state
2. Lead: based on Explore findings, write plan and spec, ask questions to user
3. Lead: only when user agrees, spawn worker agents (run_in_background: true)
4. Lead: spawn monitor agent (run_in_background: true, model: haiku)
5. Lead: respond immediately to user - "Started X, monitoring..."
6. Lead: keep track of monitor, feedback to user
7. User can interrupt/redirect anytime
8. Monitor reports completion
9. Lead: spawn review agents, test agents, e2e agents
10. Lead: update plan/spec files with results
11. Lead: stage changes, report done
```

**CRITICAL: Lead MUST update `.plan/plans/*.md` and `.plan/specs/*.md` after every analysis, review, or implementation cycle. Update BEFORE reporting to user.**

**Lead must not skim past aside-warnings.** When a subagent reports "pass" alongside unexpected warnings, rejections, or errors, investigate or re-delegate before marking done. "Pass with N warnings" is not green.

## Subagent Rules
Subagents MUST be told in Task prompt:
- Stage all changes with `git add` before completing
- Follow existing patterns in codebase
- Don't add new comments in code or config files (build.gradle, package.json, etc.)
- Code should be self-explanatory
- Prefer better naming over comments
- Avoid single-character variable names (except `i`, `j`, `k` in iterators)
- Read relevant `.plan/specs/*.md` before starting — these are the behavioral contracts
- After changes: report which spec behaviors were affected and any new behaviors to add

## Review Agent Checklist
After implementation changes, review agent should check:
- Dead code (unused methods, fields, imports)
- Stale terminology (old names after renames)
- Orphaned references (strings/comments with old names)

## Background Tasks
Use for commands expected to take >15 seconds.
```
1. Bash(command, run_in_background: true)
2. Task(monitor, model: haiku, run_in_background: true)
3. Respond immediately - stay responsive
```

## Critical Feedback
- Push back on user suggestions when drawbacks exist
- Ask clarifying questions before breaking modularity

## Frontend (pnpm)

### Code Organization
**Step-down rule (Clean Code, R.C. Martin):** Organize code top-to-bottom by abstraction level. Public API and high-level functions at the top, called functions below them, helpers below those — readers encounter functions before their implementation details.
- **Don't extract to global utils until there are 2+ consumers.** Keep helpers local to the component/composable until reuse is proven. Don't add slot-based APIs for a single consumer. A computed wrapping one function call adds indirection without value.
- **Separate data from UI.** Components should not fetch data, parse URLs, or manage complex state directly. Move those concerns into composables. Components receive data via props and composable return values.
- **Name functions for intent, not mechanics.** `preserveExternalQueryParams` over `getNonFilterParams`, `resetLocalState` over `syncFromProps`. Names should explain WHY the function exists.

### Reuse Before Reinventing
- Check `types/types.ts` for existing interfaces (e.g., `ITreeNode`, `INode`) before defining new ones
- `fetchMetadata`/`fetchTableMetadata` cache per session — don't add manual caches on top
- `getColumnIds(schemaId, tableId, expandLevel)` in `fetchTableData.ts` generates GraphQL field selections, handles composite keys recursively
- `fetchGraphql(schemaId, query, variables)` supports GraphQL variables (prefer over string interpolation)
- Don't fetch entire tables — use filtered queries to get only what you need
- **Check for existing components before using raw HTML.** Before writing a raw `<button>`, `<input>`, `<span>` with inline classes, search `app/components/` for an existing component (Button, InputSearch, Well, Skeleton, etc.). Raw HTML is only appropriate when no existing component fits AND creating one isn't justified.

### Backend GraphQL Ontology Operators
Not obvious — discover via `__type` introspection on ontology filter types:
- `_match_any_including_children: ["termName"]` — filter records matching term OR any descendant
- `_match_any_including_parents: ["termName"]` — query ontology table for terms + full ancestor chain to root
- Available on all ONTOLOGY/ONTOLOGY_ARRAY filter input types

### Story Files
Use `Story` component with markdown spec:
```vue
<template>
  <Story title="ComponentName" :spec="spec">
    <!-- demo content -->
  </Story>
</template>

<script setup>
const spec = `
## Features
- Feature list

## Props
| Prop | Type | Default |
|------|------|---------|

## Test Checklist
- [ ] Test item
`;
</script>
```

### Theme Text Color Classes
Test all 5 themes: Light, Dark, Molgenis, UMCG, AUMC

| Class | Use Case |
|-------|----------|
| `text-title` | Gradient/colored backgrounds |
| `text-title-contrast` | Content areas (`bg-content`) |
| `text-record-heading` | Section headings |
| `text-record-label` | Definition list labels |
| `text-record-value` | Definition list values |
| `text-link` | Hyperlinks |

Pitfall: `text-title` on `bg-content` = invisible in Molgenis/AUMC themes.

### Theme-Aware Styling
- **Never hardcode colors, border-radius, or spacing that varies by theme.** Check `tailwind.config.js` and `app/assets/css/` for existing theme tokens before using arbitrary Tailwind values like `bg-gray-200` or `rounded-[3px]`. If a token exists (e.g. `bg-disabled`, `rounded-3px`), use it. If no token exists and the value should be theme-aware, add a CSS variable.

### Visual Testing
Test across themes AND screen sizes:
- Themes: Light, Dark, Molgenis, UMCG, AUMC
- Sizes: Desktop 1280px+, Tablet 768px, Mobile 375px

### Error Handling (Vue/Nuxt)
- Use `try/catch` in async composables, return `{ data, error }` objects
- Display errors via toast notifications or inline error states
- Never swallow errors silently - always log or display
- Use `onErrorCaptured` for component error boundaries

### Frontend Verification Workflow
When verifying frontend changes visually:
0. Clean database (optional): `./gradlew cleandb` (resets database to fresh state)
1. Start backend: `./gradlew dev` (run in background, wait ~30s for startup)
2. Start target app (depends on app type):
   - **Nuxt apps** (ui, directory, catalogue, etc.): `cd apps/<app> && NUXT_PUBLIC_API_BASE=http://localhost:8080/ pnpm dev`
     - CRITICAL: Without `NUXT_PUBLIC_API_BASE`, Nuxt apps default to `https://emx2.dev.molgenis.org/` as API base (set in tailwind-components/nuxt.config.ts runtimeConfig).
   - **Vite apps** (schema, molgenis-components, etc.): `cd apps/<app> && MOLGENIS_APPS_HOST=http://localhost:8080 pnpm dev`
     - Vite apps use `apps/dev-proxy.config.js` which reads `MOLGENIS_APPS_HOST` (default: `https://emx2.dev.molgenis.org`) and `MOLGENIS_APPS_SCHEMA` (default: `pet store`) to proxy API calls.
     - Without `MOLGENIS_APPS_HOST`, API calls go to the remote server, not your local backend.
3. Use `playwright-cli` skill to navigate, take snapshots, verify visual behavior
4. Kill dev servers when done — don't leave them running

Backend runs on http://localhost:8080. Frontend apps typically on http://localhost:3000 (nuxt) or http://localhost:5173 (vite).

### E2E Testing with Playwright
- Some apps have Playwright e2e tests (ui, catalogue, directory, schema)
- Config: `apps/<app>/playwright.config.ts`, tests: `apps/<app>/tests/e2e/`
- For Vite apps, start dev server with `MOLGENIS_APPS_HOST=http://localhost:8080` before running tests
- API setup (create schemas, sign in) should use direct HTTP requests to `http://localhost:8080`
- Install playwright browsers: `npx playwright install chromium`

## Backend (gradle)
```
1. make changes
2. create JUnit tests
3. run './gradlew test' to verify
4. iterate
```

### Core Architecture
```
CSV metadata → SchemaMetadata/TableMetadata/Column → PostgreSQL DDL
                     ↓
              Dynamic API generation (GraphQL, CSV, RDF, custom)
```

### Key Packages
| Package | Purpose |
|---------|---------|
| `org.molgenis.emx2` | Core metadata model (Schema, Table, Column, Row) |
| `org.molgenis.emx2.sql` | PostgreSQL implementation, JOOQ integration |
| `org.molgenis.emx2.graphql` | Dynamic GraphQL schema generation |
| `org.molgenis.emx2.io` | CSV/Excel import/export, EMX2 format |
| `org.molgenis.emx2.rdf` | RDF/Turtle/JSON-LD generation |
| `org.molgenis.emx2.beaconv2` | Custom Beacon API implementation |
| `org.molgenis.emx2.webapi` | Javalin REST endpoints |

### Metadata Hierarchy
- **SchemaMetadata** → container for tables
- **TableMetadata** → columns, inheritance, semantics
- **Column** → type, refs, validation, computed, semantics
- **ColumnType** → 50+ types (STRING, REF, ONTOLOGY, FILE, etc.)

### Data Models Location
- Shared models: `/data/_models/shared/`
- Specific models: `/data/_models/specific/`
- Profiles: `/data/_profiles/*.yaml`
- Ontologies: `/data/_ontologies/`

## Testing
- Every utility/composable → test file
- Every .vue with template → story file
- Run tests before marking done
- Integration tests use embedded PostgreSQL
- **Use `pnpm run test-ci` (matches CI), not `pnpm test` (watch mode).** Watch mode never exits and "all green" output can be misread. Verify process exit code is 0 — unhandled rejections fail CI even when all assertions pass.
- **Don't launder "pre-existing" warnings.** If you find unhandled rejections, warnings, or errors labeled "known quirk" while working, verify they don't affect CI exit code before forwarding that label. A label saying "this is fine" is not evidence it is fine.
- **Never swallow errors silently.** Every `catch` block must at minimum `console.error` with a descriptive message. `catch { return fallback }` without logging hides bugs.

### Code Review Checklist
- [ ] Follows existing patterns
- [ ] Tests pass
- [ ] No lint errors
- [ ] TypeScript types correct

### junit testing
- Using ./gradlew test
- Test class naming: suffix `Test`, not prefix — e.g. `TableTypeTest.java`, not `TestTableType.java`

### Playwright / Browser Testing
- Prefer `browser_snapshot` over `browser_take_screenshot` (faster, less tokens)
- Use `browser_evaluate` for quick DOM checks instead of full snapshots
- Use `browser_fill_form` (batch) over individual `browser_type` calls
- Close browser when done (`browser_close`) to free resources
- Set reasonable viewport with `browser_resize` before testing

### Git Worktree Setup
```
git worktree add /Users/m.a.swertz/git/molgenis-emx2/<category>/<name> -b mswertz/<category>/<name> master
ln -s /Users/m.a.swertz/git/molgenis-emx2/master/.claude /Users/m.a.swertz/git/molgenis-emx2/<category>/<name>/.claude
```
- Folder structure mirrors branch name: `feat/lakehouse`, `fix/something`, etc.
- Always symlink `.claude` from master to the new worktree
- To sync with master: use `git merge master`, never rebase (PRs are squash-merged, rebase causes issues)
