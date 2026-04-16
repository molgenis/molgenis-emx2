package org.molgenis.emx2.rag;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

public class EmbeddingStoreFactory {

  private EmbeddingStoreFactory() {}

  public static PgVectorEmbeddingStore create() {
    return PgVectorEmbeddingStore.builder()
        .host("localhost")
        .port(5436)
        .database("emx-2-vector")
        .user("postgres")
        .password("postgres")
        .table("embeddingschunked")
        .dimension(384)
        .build();
  }
}
