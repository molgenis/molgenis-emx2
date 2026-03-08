# Plan: Add Canonical URLs to Catalogue App

## Problem
Same content accessible via multiple URLs hurts SEO:
- Variables: 3 different paths
- Datasets: 2 different paths
- Resources: multiple catalogue contexts

## Solution
Add `<link rel="canonical">` to all pages via `useHead()`.

## Decisions
- **Relative URLs** (no siteUrl config needed)
- **List pages**: canonical ignores query params (filters/pagination)

## Canonical URL Strategy

| Resource | Canonical Path |
|----------|----------------|
| Variable | `/all/variables/{id}` |
| Dataset | `/all/datasets/{resource}/{name}` |
| Dataset variable | `/all/datasets/{resource}/{name}/{variable}` |
| Collection/Network | `/all/{resourceType}/{resource}` |
| Subpopulation | `/all/{resourceType}/{resource}/subpopulations/{id}` |
| Collection event | `/all/{resourceType}/{resource}/collection-events/{id}` |

Using `/all/` as catalogue prefix (global/cross-catalogue scope).

## Implementation

### 1. Create composable: `useCanonical.ts`
Location: `apps/catalogue/app/composables/useCanonical.ts`
```typescript
export function useCanonical(path: string) {
  useHead({
    link: [{ rel: 'canonical', href: path }]
  })
}
```

### 2. Add canonical to each page

| File | Canonical |
|------|-----------|
| `pages/index.vue` | `/` |
| `pages/about.vue` | `/about` |
| `pages/[catalogue]/index.vue` | `/{catalogue}` |
| `pages/[catalogue]/about.vue` | `/{catalogue}/about` |
| `pages/[catalogue]/variables/index.vue` | `/all/variables` |
| `pages/[catalogue]/variables/[variable].vue` | `/all/variables/{variable}` |
| `pages/[catalogue]/datasets/index.vue` | `/all/datasets` |
| `pages/[catalogue]/datasets/[resource]/index.vue` | `/all/datasets/{resource}` |
| `pages/[catalogue]/datasets/[resource]/[name]/index.vue` | `/all/datasets/{resource}/{name}` |
| `pages/[catalogue]/datasets/[resource]/[name]/[variable].vue` | `/all/datasets/{resource}/{name}/{variable}` |
| `pages/[catalogue]/[resourceType]/index.vue` | `/all/{resourceType}` |
| `pages/[catalogue]/[resourceType]/[resource]/index.vue` | `/all/{resourceType}/{resource}` |
| `pages/[catalogue]/[resourceType]/[resource]/variables/[variable].vue` | `/all/variables/{variable}` |
| `pages/[catalogue]/[resourceType]/[resource]/subpopulations/[subpopulation].vue` | `/all/{resourceType}/{resource}/subpopulations/{subpopulation}` |
| `pages/[catalogue]/[resourceType]/[resource]/collection-events/[collectionevent].vue` | `/all/{resourceType}/{resource}/collection-events/{collectionevent}` |

## Files to Create/Modify

**Create:**
- `apps/catalogue/app/composables/useCanonical.ts`

**Modify:**
- 15 page files (add `useCanonical()` call in script setup)

## Verification

1. Run dev server: `cd apps/catalogue && pnpm dev`
2. View page source, search for `rel="canonical"`
3. Test same resource via different paths → same canonical URL:
   - `/all/variables/SomeVar`
   - `/someCatalogue/collections/SomeCollection/variables/SomeVar`
   - Both should have canonical: `/all/variables/SomeVar`
