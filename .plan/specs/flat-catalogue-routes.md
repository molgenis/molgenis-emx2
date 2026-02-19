# Flat Catalogue Routes - SEO Improvement

## Problem

1. **Duplicate content**: Collections in multiple catalogues appear at multiple URLs
   - `/LifeCycle/collections/GECKO` and `/EUCAN/collections/GECKO` → same content, different URLs
   - Search engines penalize duplicate content

2. **No canonical homepage for collections**: COHORT1 cannot use `/COHORT1` as its homepage

3. **Catalogue baked into URL path**: Makes it impossible to have a single canonical URL per resource

## Goals

1. Each resource (catalogue, collection) gets ONE canonical URL: `/:resourceId`
2. Collections are no longer nested under catalogues → eliminates duplicate content
3. Resources can use `/:resourceId` as their homepage (e.g., `/COHORT1`)
4. Catalogue context preserved via `?catalogue=` query parameter for breadcrumbs/navigation
5. Rendering adapts based on resource type (catalogue vs collection)

## URL Structure

```
UNCHANGED:
/                                              → catalogue picker
/about                                         → global about (RESERVED)
/tables                                        → metadata browser (RESERVED)

GLOBAL ("ALL") ROUTES:
/all                                           → all-catalogues landing page
/all/collections?catalogue=all                 → all collections
/all/variables?catalogue=all                   → all variables

FLAT RESOURCE ROUTES:
/:resourceId                                   → type-detected (Catalogue landing OR Collection detail)
/:resourceId/about?catalogue=X                 → about page
/:resourceId/collections?catalogue=X           → sub-collections
/:resourceId/variables?catalogue=X             → variables overview
/:resourceId/datasets/:datasetId/:variableId?catalogue=X  → variable detail
/:resourceId/collection-events/:event?catalogue=X         → collection event detail
/:resourceId/subpopulations/:subpop?catalogue=X           → subpopulation detail
```

### Reserved Route IDs

`about`, `all`, `tables` are reserved (defined in `app/utils/constants.ts`).

### Catalogue Context

`?catalogue=LifeCycle` query parameter:
- Passed through internal links when browsing within a catalogue
- Used for breadcrumbs: Home → LifeCycle → GECKO
- NOT included in canonical URL, sitemap, or redirect targets

### Type Detection at `/:resourceId`

1. Fetch resource by id from GraphQL
2. Check `type.name === "Catalogue"`
3. Render `CatalogueLandingView` (stats + sub-resources) OR `CollectionDetailView` (metadata detail)

### Networks

No dedicated `/networks` route. Networks are shown inline on catalogue landing pages.

### Rendering Differences

| Aspect | Catalogue | Collection |
|--------|-----------|-----------|
| Landing | Stats + sub-collections list | Detail page with metadata |
| Tabs | Collections, Variables | Variables, Datasets, Subpopulations |
| Breadcrumb | Home → CatalogueName | Home → [CatalogueName →] ResourceName |
| Children | Shows resources belonging to it | Shows sub-collections if any |

### Breadcrumb Behavior

| Context | Breadcrumb |
|---------|------------|
| Direct navigation to `/GECKO` | Home → GECKO |
| From catalogue `/GECKO?catalogue=LifeCycle` | Home → LifeCycle → GECKO |
| Nested `/GECKO/variables?catalogue=LifeCycle` | Home → LifeCycle → GECKO → Variables |
| Variable `/GECKO/datasets/DS1/VAR1?catalogue=LifeCycle` | Home → LifeCycle → GECKO → Datasets → DS1 → VAR1 |

Note: "Subpopulations" and "Collection events" breadcrumb segments are non-clickable (no index pages).

### cohortOnly Mode

Works unchanged. Flat paths apply, `?catalogue=` not relevant in single-cohort mode.

### Redirects (301)

```
/:catalogue/collections/:id  → /:id?catalogue=:catalogue
/:catalogue/networks/:id     → /:id?catalogue=:catalogue
/:catalogue/variables/:id    → /:id/variables?catalogue=:catalogue (best effort)
```

Handled by `app/middleware/legacy-redirect.global.ts`.

## SEO Additions (Phase 2, future)

1. Canonical URLs: `<link rel="canonical">` on every page (without query params)
2. Sitemap: dynamic sitemap.xml with `/:resourceId` URLs
3. Structured data: JSON-LD (schema.org/Dataset for collections, DataCatalog for catalogues)
4. BreadcrumbList structured data for rich snippets
5. Open Graph + Twitter Card meta tags

## Backend Sitemap (Phase 3, future)

Update CatalogueSiteMap.java to generate `/:resourceId` URLs with lastmod/priority.
