# Flat Catalogue Routes - Implementation Plan

## Summary

Flatten catalogue URLs so each resource gets ONE canonical URL at `/:resourceId`.
Catalogue context becomes a `?catalogue=` query parameter for breadcrumbs only.

See: `.plan/specs/flat-catalogue-routes.md` for full spec.

## Phase 1: Route Restructuring — COMPLETE

### 1.1 New page structure under `[resourceId]/`

Replaced `[catalogue]/` dynamic route tree with `[resourceId]/` tree.

**`[resourceId]/index.vue`** — type-detecting resource page:
- Fetches resource by id from GraphQL
- Checks `type.name === "Catalogue"` → CatalogueLandingView, else → CollectionDetailView
- Reads `?catalogue=` from query for breadcrumb context
- Handles reserved IDs (`about`, `all`, `tables`) via constants

**Sub-routes:**
- `[resourceId]/about.vue` — resource about page
- `[resourceId]/collections/index.vue` — sub-collections
- `[resourceId]/variables/index.vue` — variables overview
- `[resourceId]/datasets/[datasetId]/[variableId].vue` — variable detail
- `[resourceId]/collection-events/[collectionevent].vue`
- `[resourceId]/subpopulations/[subpopulation].vue`

### 1.2 Composable: `useCatalogueContext()`

Centralizes catalogue context logic:
- Reads `?catalogue=` from route.query
- Provides: catalogueId, resourceId, and link-building helpers
- Builds breadcrumbs with optional catalogue prefix
- Generates links with `?catalogue=` preserved

### 1.3 Legacy redirect middleware

`app/middleware/legacy-redirect.global.ts`:
- Detects old URL patterns: `/:a/collections/:b`, `/:a/networks/:b`
- 301 redirects to `/:b?catalogue=:a`
- Detects `/:a/variables/:b` → redirects to `/:b/variables?catalogue=:a`

### 1.4 Internal links updated

All components that generate URLs updated:
- `ResourceCard.vue` — flat URL links
- `NetworkCard.vue` — flat URL links
- `VariableCard.vue` — flat URL links
- `CollectionEventDisplay.vue` — flat URL links
- `SubpopulationDisplay.vue` — flat URL links
- `harmonisation/HarmonisationTable.vue` — flat URL links
- `layouts/DetailPage.vue`, `LandingPage.vue`, `SearchPage.vue` — minor fixes
- `header/Catalogue.vue` — builds nav from resourceId + ?catalogue param
- `composables/useHeaderData.ts` — uses new route structure

Pattern: `/${catalogue}/${type}/${id}` → `/${id}?catalogue=${catalogue}`

### 1.5 Breadcrumb logic

All pages read `?catalogue=` for breadcrumb context:
- With `?catalogue=LifeCycle`: Home → LifeCycle → Resource
- Without: Home → Resource
- Nested: Home → LifeCycle → Resource → SubPage
- "Subpopulations" and "Collection events" breadcrumb segments are non-clickable

### 1.6 `/all` landing page

Created `app/pages/all/index.vue` as standalone page (not reusing shared component).

### 1.7 Extracted view components

Moved page logic into reusable components:
- `[catalogue]/index.vue` → `components/CatalogueLandingView.vue`
- `[catalogue]/[resourceType]/[resource]/index.vue` → `components/CollectionDetailView.vue`

### Files created
- `app/pages/[resourceId]/index.vue`
- `app/pages/all/index.vue`
- `app/composables/useCatalogueContext.ts`
- `app/middleware/legacy-redirect.global.ts`
- `app/utils/constants.ts`

### Files renamed (page → component)
- `[catalogue]/index.vue` → `components/CatalogueLandingView.vue`
- `[catalogue]/[resourceType]/[resource]/index.vue` → `components/CollectionDetailView.vue`

### Files renamed (route restructure)
- `[catalogue]/about.vue` → `[resourceId]/about.vue`
- `[catalogue]/[resourceType]/index.vue` → `[resourceId]/collections/index.vue`
- `[catalogue]/variables/index.vue` → `[resourceId]/variables/index.vue`
- `[catalogue]/.../collection-events/[collectionevent].vue` → `[resourceId]/collection-events/[collectionevent].vue`
- `[catalogue]/.../subpopulations/[subpopulation].vue` → `[resourceId]/subpopulations/[subpopulation].vue`
- `[catalogue]/.../variables/[variable].vue` → `[resourceId]/datasets/[datasetId]/[variableId].vue`

### Files deleted
- `[catalogue]/variables/[variable].vue` (replaced by datasets route)
- Entire `[catalogue]/` directory tree

### Files modified
- `header/Catalogue.vue` — builds nav from resourceId + ?catalogue param
- `ResourceCard.vue`, `NetworkCard.vue`, `VariableCard.vue` — flat URL links
- `CollectionEventDisplay.vue`, `SubpopulationDisplay.vue` — flat URL links
- `harmonisation/HarmonisationTable.vue` — flat URL links
- `layouts/DetailPage.vue`, `LandingPage.vue`, `SearchPage.vue` — minor fixes
- `composables/useHeaderData.ts` — uses new route structure
- `nuxt.config.ts` — updated for new route structure

### Deviations from original spec
1. Query param: `?catalogue=` (not `?cat=`)
2. Variable detail route: `/:resourceId/datasets/:datasetId/:variableId` (not `/:resourceId/:datasetId/:variableId`)
3. No `/networks` route — networks shown inline on catalogue landing pages
4. `/all` landing page: standalone `all/index.vue` (not reusing shared component)
5. Type detection: `type.name === "Catalogue"` (not `type.tags`)

### Known pre-existing issues (NOT from this spike)
- ContentReadMore hydration mismatch (SSR vs client)
- 500 errors on invalid subpopulation/event/dataset IDs (missing null checks)
- Empty subpopulation/event tables on some collection detail pages (data issue)

### Testing
- [x] Routes resolve correctly
- [x] Type detection renders right layout
- [x] Breadcrumbs with and without `?catalogue=`
- [x] Legacy redirects (301)
- [x] `/all` landing page links
- [x] Manual testing via Playwright

---

## SEO Audit Summary (5-agent review)

Audited by: Technical SEO, Structured Data, Content SEO, Duplicate Content, Academic Discovery specialists.

### What's good
- SSR works — all pages server-rendered with `useFetch()`, crawlers get full HTML
- 301 redirects correct for collections/networks old URLs
- Proper 404s via `createError({ statusCode: 404 })`
- Flat routes eliminate primary duplicate content problem
- Backend has mature DCAT-AP/RDF infrastructure (Turtle, JSON-LD, RDF/XML, SHACL validation)
- PID field exists in data model (`dcterms:identifier`)
- Rich metadata available in GraphQL (countries, keywords, startYear, endYear, etc.)

### What's missing (by priority)

**CRITICAL — duplicate content risk:**
- No `<link rel="canonical">` anywhere (same content at `/GECKO` and `/GECKO?catalogue=LifeCycle`)
- Legacy redirect middleware incomplete: old subpopulation, collection-event, and deep variable URLs → 404

**CRITICAL — invisible to search engines:**
- No JSON-LD structured data (blocks Google Dataset Search entirely)
- No schema.org/Dataset markup for collections
- No schema.org/DataCatalog markup for catalogues

**HIGH — social sharing & click-through:**
- No Open Graph meta tags (og:title, og:description, og:image, og:url)
- No Twitter Card meta tags
- Meta descriptions minimal — don't include participant counts, keywords, etc.

**HIGH — crawlability:**
- SideNavigation links wrapped in `<ClientOnly>` — hidden from crawlers
- No BreadcrumbList JSON-LD (no rich snippet breadcrumbs in search results)
- No rel="next/prev" for paginated lists

**MEDIUM — academic discovery:**
- PID field not exposed in structured data or displayed prominently
- No content negotiation (Accept: application/ld+json doesn't serve RDF)
- No signposting Link headers (describedby, license, canonical)
- No robots.txt from frontend

---

## Phase 2: SEO & Structured Data

### 2.1 Canonical URLs + duplicate content fixes
**Priority: CRITICAL | Effort: Small**

- Add `<link rel="canonical">` to every page via `useHead()`
- Always path-only (no `?catalogue=`), absolute URL
- Composable: `useCanonicalUrl()` — strips query params, builds absolute URL
- Apply globally in `app.vue` or per-layout

### 2.2 Complete legacy redirects
**Priority: CRITICAL | Effort: Small**

Extend `legacy-redirect.global.ts` to handle ALL old URL patterns:
- `/:cat/:type/:resource/subpopulations/:sub` → `/:resource/subpopulations/:sub?catalogue=:cat`
- `/:cat/:type/:resource/collection-events/:evt` → `/:resource/collection-events/:evt?catalogue=:cat`
- `/:cat/:type/:resource/variables/:var` → `/:resource/datasets/:var?catalogue=:cat` (best effort)
- `/:cat/variables/:var` → `/:var/variables?catalogue=:cat` (if not already handled)

### 2.3 JSON-LD: schema.org/Dataset for collections
**Priority: CRITICAL | Effort: Medium**

In `CollectionDetailView.vue`, emit JSON-LD via `useHead()`:
```
@type: Dataset
Fields: name, description, url, identifier (PID), creator, datePublished,
        license, spatialCoverage, temporalCoverage, keywords,
        includedInDataCatalog, numberOfParticipants (via variableMeasured)
```
All fields already available in existing GraphQL query.
Reference: `apps/directory/src/functions/bioschemasMapper.js` has prior art.

### 2.4 JSON-LD: schema.org/DataCatalog for catalogues
**Priority: CRITICAL | Effort: Small**

In `CatalogueLandingView.vue`, emit JSON-LD:
```
@type: DataCatalog
Fields: name, description, url, keywords, creator, dataset[] (list child collections)
```

### 2.5 JSON-LD: BreadcrumbList
**Priority: HIGH | Effort: Small**

Emit BreadcrumbList JSON-LD on all pages with breadcrumbs.
Can be built from existing breadcrumb data in layout components.

### 2.6 Open Graph + Twitter Card
**Priority: HIGH | Effort: Small**

Add to `app.vue` or per-page via `useHead()`:
- `og:title`, `og:description`, `og:url`, `og:image` (resource logo), `og:type`
- `twitter:card` (summary_large_image), `twitter:title`, `twitter:description`

### 2.7 Fix SideNavigation crawlability
**Priority: HIGH | Effort: Tiny**

Remove `<ClientOnly>` wrapper from `SideNavigation.vue` links.
These are standard `<a>` navigation — no reason to be client-only.

### 2.8 Improve meta descriptions
**Priority: MEDIUM | Effort: Small**

Enrich descriptions with specifics:
- Collections: include type, participant count, countries
- Catalogues: include collection count, variable count
- Variables: include dataset name, resource name

### 2.9 Pagination SEO
**Priority: MEDIUM | Effort: Small**

Add `rel="next"` / `rel="prev"` link tags on paginated search pages
(collections list, variables list).

### 2.10 Heading hierarchy
**Priority: MEDIUM | Effort: Small**

Verify each page has exactly one `<h1>`. Check `PageHeader` component renders semantic heading.

---

## Phase 3: Academic Discovery & Backend

### 3.1 Update CatalogueSiteMap.java
**Priority: HIGH | Effort: Medium**

- Generate `/:resourceId` URLs instead of `/all/collections/:id`
- Add `<lastmod>`, `<priority>`, `<changefreq>`
- Catalogues: priority 1.0, weekly
- Collections: priority 0.8, monthly

### 3.2 Content negotiation
**Priority: MEDIUM | Effort: Medium**

Add Nuxt server middleware to detect `Accept: application/ld+json` or `text/turtle` on `/:resourceId` routes and proxy to backend RDF API (`/api/rdf/Resources?id=...`).

### 3.3 Signposting (Link headers)
**Priority: MEDIUM | Effort: Small**

Emit HTTP Link headers on resource pages:
- `rel="canonical"` → `/:resourceId`
- `rel="describedby"` → `/api/rdf/Resources?id=...` (type: application/ld+json)
- `rel="license"` → license URI (if available)

### 3.4 PID display & validation
**Priority: MEDIUM | Effort: Small**

Display PID prominently on resource detail pages (hero section).
Include PID in JSON-LD as `schema:identifier`.

### 3.5 robots.txt
**Priority: LOW | Effort: Tiny**

Create `public/robots.txt`:
- Allow all
- Point to sitemap
- No need to disallow `?catalogue=` if canonical URLs are in place

### 3.6 DCAT-AP link tags
**Priority: LOW | Effort: Tiny**

Add `<link rel="alternate">` pointing to RDF endpoints:
- `type="application/ld+json"` → `/api/jsonld/...`
- `type="text/turtle"` → `/api/ttl/...`

---

## Open Questions

- Should `?catalogue=` variants return `noindex` meta tag, or is canonical sufficient?
- Should variable-level pages get JSON-LD too, or only collection/catalogue level?
- Is the `bioschemasMapper.js` in directory app maintained? Can we reuse/share it?
- Content negotiation: proxy in Nuxt middleware vs nginx rewrite rule?
