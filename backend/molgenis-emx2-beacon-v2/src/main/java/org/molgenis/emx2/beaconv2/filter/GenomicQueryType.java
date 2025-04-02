package org.molgenis.emx2.beaconv2.filter;

public enum GenomicQueryType {
  SEQUENCE(
      """
      { _and: [
        { startPosition: { equals: %d } },
        { refseqAssemblyId: {equals: "%s" } },
        { ref: { like: "%s" } },
        { alt: {like: "%s" } }
      ]}"""),
  RANGE(
      """
      { _or: [
        { _and: [
          { refseqAssemblyId: { equals: "%1$s" } },
          { startPosition: { between: [%2$d , %3$d] } }
        ]},
        { _and: [
          { refseqAssemblyId: { equals: "%1$s" } },
          { stopPosition: { between: [%2$d , %3$d] } }
        ]}
      ]}"""),
  GENE_ID("{ geneId: { equals: \"%s\" } }"),
  BRACKET(
      """
      { _and: [
        { refseqAssemblyId: { equals: "%s" } },
        { startPosition: { between: [ %d, %d ] } },
        { stopPosition: { between: [ %d, %d ]  } }
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
