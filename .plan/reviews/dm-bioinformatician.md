# FAIRmapper MVP Review - Bioinformatician / FAIR Data Expert

**Reviewer perspective:** FAIR principles expert, works with RDF/DCAT/Beacon/ontologies

## 1. FAIR Compliance Assessment

**Strengths:**
- **Findable (F)**: dcat-fdp correctly implements FDP metadata profiles with `fdp-o:metadataIdentifier`, persistent URLs, proper JSON-LD `@id` URIs
- **Accessible (A)**: REST endpoints properly implement content negotiation via `output-rdf: turtle`
- **Interoperable (I)**: Solid use of DCAT, FDP-O, datacite vocabularies with correct namespaces
- **Reusable (R)**: License metadata (`dct:license`) present, though hardcoded to CC-BY-4.0

**Critical Gaps:**
- **Missing mandatory DCAT fields**: No `dct:issued`, `dct:modified` for actual data timestamps (FDP metadata timestamps hardcoded to 2024-01-01)
- **No distributions**: `dcat:distribution` completely absent - how do users access actual data?
- **Access rights missing**: No `dct:accessRights` or `odrl:hasPolicy` - crucial for FAIR principle A1.2
- **Temporal/spatial coverage**: Missing `dct:temporal`, `dct:spatial`
- **Language tags**: No `@language` annotations, limits international interoperability

## 2. RDF/Ontology Handling

**What works:**
- JSON-LD framing approach appropriate - easier than SPARQL for data managers
- JSLT for RDF transforms is pragmatic
- Proper typed literals: `{"@type": "xsd:dateTime", "@value": "..."}`
- `get-key()` pattern for JSON-LD keys documented

**Concerns:**
- **JSLT ergonomics for RDF**: Constructing JSON-LD in JSLT is verbose and fragile - typos in URIs won't be caught
- **No validation**: No SHACL validation in pipeline. FDP has strict profiles - how know output is valid?
- **URI construction in JSLT**: Manual string concatenation risks malformed URIs

## 3. Beacon v2 Implementation

**Spec compliance:**
- Response structure matches Beacon v2 models schema ✓
- `meta.beaconId`, `apiVersion`, `returnedSchemas` present ✓
- `responseSummary.exists`, `numTotalResults` present ✓
- `resultSets` with proper nesting ✓

**Issues:**
- **Filter mapping incomplete**: Only supports sex (ncit:C28421) and diseases (ncit:C2991). Real Beacon needs genomic variant filters
- **Sex ontology mismatch**: Maps NCIT → GSSO but Beacon v2 spec uses NCIT directly
- **Missing Beacon fields**: No `info`, `beaconHandovers`, `resultsHandover`
- **Diseases always null**: Query fetches but transform outputs null
- **No granularity control**: `includeResultsetResponses` parameter not implemented

## 4. Top 3 Improvements for Better FAIR Support

**1. Add DCAT mandatory fields + distributions**
```jslt
"dct:issued": {"@type": "xsd:dateTime", "@value": $dataset.created},
"dct:modified": {"@type": "xsd:dateTime", "@value": $dataset.modified},
"dct:accessRights": {"@id": $dataset.accessLevel},
"dcat:distribution": [for ($dataset.files) {
  "@type": "dcat:Distribution",
  "dcat:downloadURL": {"@id": .url},
  "dcat:mediaType": .format
}]
```
Without distributions, catalogs are just descriptions - users can't GET data.

**2. Integrate SHACL validation step**
```yaml
steps:
  - query: src/queries/get-catalog.gql
  - transform: src/transforms/to-dcat-catalog.jslt
  - validate-shacl: https://www.purl.org/fairtools/fdp/schema/0.1/catalogMetadata
  - output-rdf: turtle
```

**3. Simplify RDF syntax - declarative field mapping**
The proposed Phase 7.4 declarative mapping is better:
```yaml
- mapping:
    source-type: "dcat:Catalog"
    target-table: Resources
    fields:
      - from: "dcterms:title"
        to: name
```
For 80% of DCAT mappings, this eliminates JSLT. RDF mappings shouldn't require programming.

## 5. Overall Impression

**Would this help my FAIR data work? Yes, with reservations.**

**Positives:**
- Tool I can configure without Java releases
- JSON-LD framing makes RDF approachable
- Testing framework excellent - rare in FAIR tools
- Clear separation (fetch, transform, output-rdf)

**Blockers for production:**
- Incomplete DCAT - can't publish to real FDP networks yet
- No SHACL validation = blind publishing
- Missing distribution metadata = findable but not accessible
- Hardcoded timestamps and licenses

**Missing from docs:**
- Zero explanation of WHY FAIR matters
- No guidance on ontology selection for themes/keywords
- No mention of FDP registration/harvesting workflows
- Troubleshooting has nothing about RDF validation errors

**Comparison:**
- **vs FAIR Data Point (Java)**: FAIRmapper more flexible but less compliant out-of-box
- **vs RML**: FAIRmapper simpler but less expressive
- **vs custom Python**: FAIRmapper has better structure but JSLT awkward

**Verdict:** Promising MVP that solves real problems (Java-free FAIR tooling, testable transforms). Needs DCAT completeness and validation before production. Architecture is sound - fix metadata gaps and this could replace custom ETL scripts in many labs.
