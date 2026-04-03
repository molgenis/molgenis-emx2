package org.molgenis.emx2.rag;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.*;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.io.IOException;

public class Main {

  public static void main(String[] args) throws IOException {

    System.out.println("Hello and welcome to the rag the rag demo!");

    // Setup the model
    EmbeddingModel model = RagModelFactory.create();

    // Setup the store ( used stored embeddings)
    PgVectorEmbeddingStore store = EmbeddingStoreFactory.create();

    RagService rag = new RagService(model, store);

    rag.query("Lung cancer collections");
  }
}
