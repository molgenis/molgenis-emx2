# Graph genome API
This API is loosely based on the concept of a graph genome, which is constructed from a population of genome sequences, such that each haploid genome in this population is represented by a sequence path through the graph.
Instead of using individually measured genomes, the graph genome API converts a collection of DNA variation into a network of nodes representing variant reference and alternative alleles, interspersed with reference genome bases.

### Setup
The API reads its data from the _GenomicVariations_ table which represents Beacon v2 [Genomic Variations](https://docs.genomebeacons.org/schemas-md/genomicVariations_defaultSchema/).
The easiest way to enable Beacon v2 in MOLGENIS EMX2 is by choosing 'FAIR_DATA_HUB' as a template for your database.

Variants from this table are only usable by this API when it least these columns contain values:
- Gene identifier (`geneId`)
- Genome assembly identifier (`position_assemblyId`)
- Starting position (`position_start`)

This API makes use of the [UCSC REST API](https://genome.ucsc.edu/goldenPath/help/api.html) to retrieves reference genomic sequences.
If not internet connection is available, or the API is run in 'offline mode', the reference genomic sequence is replaced with repeats of N-bases.

### Endpoint and request
The API can be reached at `<server>/api/graphgenome`.
The following request parameters must be provided:
- `gene`: the HGNC gene identifier to query variants on, for instance `TERC`.
- `assembly`: the local genome assembly identifier to query variants on, typically `GRCh37` or `GRCh38`.
- `ucscgenome`: the UCSC genome assembly identifier, typically `hg19` or `hg38`.

A complete request could, for example, look like this:

`<server>/api/graphgenome?gene=TERC&assembly=GRCh37&ucscgenome=hg19`

### Response graph

The resulting genome graph is returned in RDF Turtle format (TTL).
A basic overview of the graph structure starts at the root node `EMX2`, the next level allows grouping sequences by gene, followed by upstream genome reference sequence of that gene:
```
EMX2 <- graphgenome <- TERC <- TCACAAGCC
```

The first variant reference and variant alternative is then connected to this upstream sequence:
```
TCACAAGCC <- C
TCACAAGCC <- T
```

Followed by more genome reference sequence between the previous and next variant, and so on:
```
C <- CCATTGCCGGCGAGGGGTGACGGAT...CCCACCAACAGGAAAGCGAACTGCA
T <- CCATTGCCGGCGAGGGGTGACGGAT...CCCACCAACAGGAAAGCGAACTGCA
```

And, if available, a clinical interpretation node of the alternative variant:
```
T <- clinical_interpretation0
```

The RDF can be imported into triple stores such a [GraphDB](https://graphdb.ontotext.com/) for convenient and interactive visualization of the genome graph.

### Limitations

The graph building algorithm can handle the following situations:
- Regular interspaced variation
- Multiple variants at the same coordinates
- Variants right next to other variants
- Variants inside upstream indel variants

However, the resulting graph in case of variants inside upstream indels is currently not a proper graph genome representation because the appended reference sequence is 'restarted' instead of recursive backtracking on previous nodes to split up indel reference notation to accomodate additional variation.
A more powerful implementation would be needed to resolve this issue.
