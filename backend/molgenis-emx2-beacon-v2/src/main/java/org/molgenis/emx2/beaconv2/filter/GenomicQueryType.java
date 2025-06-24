package org.molgenis.emx2.beaconv2.filter;

public enum GenomicQueryType {
  SEQUENCE(
      """
      { _and: [
        { position_start: { equals: %d } },
        { position_refseqId: {equals: "%s" } },
        { referenceBases: { like: "%s" } },
        { alternateBases: {like: "%s" } }
      ]}"""),
  RANGE(
      """
      { _or: [
        { _and: [
          { position_refseqId: { equals: "%1$s" } },
          { position_start: { between: [%2$d , %3$d] } }
        ]},
        { _and: [
          { position_refseqId: { equals: "%1$s" } },
          { position_end: { between: [%2$d , %3$d] } }
        ]}
      ]}"""),
  GENE_ID("{ geneId: { equals: \"%s\" } }"),
  BRACKET(
      """
      { _and: [
        { position_refseqId: { equals: "%s" } },
        { position_start: { between: [ %d, %d ] } },
        { position_end: { between: [ %d, %d ]  } }
      ]}"""),
  NO_PARAMS(null);

  private final String graphQlQuery;

  GenomicQueryType(String graphQlQuery) {
    this.graphQlQuery = graphQlQuery;
  }

  public String getGraphQlQuery() {
    return graphQlQuery;
  }
}
