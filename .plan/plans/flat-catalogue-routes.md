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

## Phase 2: SEO Implementation (FUTURE)

### 2.1 Canonical URLs
- Add `<link rel="canonical">` to every page via useHead()
- Always absolute URL, never includes `?catalogue=`

### 2.2 Structured Data (JSON-LD)
- Collections → `schema.org/Dataset` with `includedInDataCatalog`
- Catalogues → `schema.org/DataCatalog` with `dataset` list
- `BreadcrumbList` structured data on all pages

### 2.3 Open Graph + Twitter Card
- `og:title`, `og:description`, `og:type`, `og:url`, `og:image`
- `twitter:card`, `twitter:title`, `twitter:description`

### 2.4 Meta tag improvements
- Page titles: signal resource type
- Unique descriptions per page

---

## Phase 3: Backend Sitemap (FUTURE)

### 3.1 Update CatalogueSiteMap.java
- Generate `/:resourceId` URLs instead of `/all/collections/:id`
- Add `<lastmod>`, `<priority>`, `<changefreq>`

### 3.2 robots.txt
- Point to sitemap
- No need to block old URLs (redirects handle it)
