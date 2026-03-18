# DCAT Harvester Plan

## Status: In Progress

## Completed
- [x] Core pipeline: Fetch -> Frame -> Map -> Import
- [x] RdfFetcher with recursive crawling (same-host filter)
- [x] JSON-LD framing via Titanium
- [x] JSLT transform for DCAT -> MOLGENIS mapping
- [x] DcatHarvestApi REST endpoint
- [x] Progress reporting: 4 subtasks with counts (triples, resources, rows, imports)
- [x] Per-type counts in Import step (Catalogue, Cohort study, etc.)
- [x] Failed imports: error subtask per failed resource + failed count in summary
- [x] saveResourceRows accepts Task for per-save progress subtasks
- [x] Unit tests (DcatHarvestTaskUnitTest, RdfFetcherTest, etc.)
- [x] Integration test (CatalogueTest roundtrip)
- [x] Sonar quality gate: 0 bugs, 82.2% coverage
- [x] CI green

## Pipeline Steps (DcatHarvestTask.run)
1. **Fetch** — RdfFetcher.fetchRecursively, reports triple count
2. **Frame** — JsonLdFrameGenerator + JsonLdFramer, reports resource count
3. **Map** — buildReverseContext + collectResourceRows, reports per-type row counts
4. **Import** — saveResourceRows, per-failure error subtask, summary with per-type counts + failed count

## TODO
- [ ] UI: Add 'upload' option (paste/upload RDF file) alongside current 'link' input
  - Harvest.vue already supports URL input
  - Need file upload or textarea for pasting RDF content
  - Backend already has `harvestRdf(String rdfTurtle)` method
- [ ] Consider pagination for large catalogs
- [ ] Error display in UI (show failed resources from task subtasks)
