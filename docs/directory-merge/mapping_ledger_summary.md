# Phase 2 - case-by-case mapping ledger, summary

Full row-level detail lives in `mapping_ledger.csv` (29013 rows).

## Headline

**29013 directory records ledgered -> 27265 auto (94.0%) / 919 needs curation (3.2%) / 829 dropped (2.9%).**

## By directory_table

| directory_table | rows | auto | needs_curation | dropped |
|---|---:|---:|---:|---:|
| CollectionFacts | 19581 | 19581 | 0 | 0 |
| Collections | 4954 | 4695 | 259 | 0 |
| Persons | 2578 | 1754 | 0 | 824 |
| Biobanks | 849 | 374 | 475 | 0 |
| JuridicalPerson | 512 | 461 | 51 | 0 |
| AlsoKnownIn | 166 | 27 | 134 | 5 |
| Studies | 135 | 135 | 0 | 0 |
| QualityInfoBiobanks | 105 | 105 | 0 | 0 |
| Networks | 59 | 59 | 0 | 0 |
| QualityInfoCollections | 44 | 44 | 0 | 0 |
| NationalNodes | 26 | 26 | 0 | 0 |
| Services | 4 | 4 | 0 | 0 |

## By disposition (total)

| disposition | rows | % |
|---|---:|---:|
| auto | 27265 | 94.0% |
| needs_curation | 919 | 3.2% |
| dropped | 829 | 2.9% |

## By catalogue_table

| catalogue_table | rows |
|---|---:|
| Collection facts | 20549 |
| Collections | 4327 |
| Contacts | 1754 |
| DROPPED | 1001 |
| Organisations | 538 |
| Biobanks | 392 |
| Quality info | 149 |
| Collection events | 121 |
| Subpopulations | 92 |
| Networks | 59 |
| External identifiers | 27 |
| Services | 4 |

## from_UMCG subset

**855 / 29013 rows (2.9%) are UMCG-attributable** (id/name/parent/biobank text matches `\bumcg\b` or `\bgroningen\b`; the 312 sub-collection rows reuse the two-tier sub-collection detector verbatim, everything else uses a generic id/name/biobank-name/juridical_person match computed here).

| disposition | UMCG rows |
|---|---:|
| auto | 701 |
| needs_curation | 147 |
| dropped | 7 |

By directory_table (UMCG subset):

| directory_table | UMCG rows |
|---|---:|
| CollectionFacts | 412 |
| Collections | 384 |
| Persons | 28 |
| Biobanks | 27 |
| Networks | 2 |
| QualityInfoCollections | 1 |
| JuridicalPerson | 1 |

