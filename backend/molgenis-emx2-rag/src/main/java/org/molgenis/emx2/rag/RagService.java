package org.molgenis.emx2.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RagService {
  private static final String URL = "url";
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static final String SOURCE = "source";

  private EmbeddingModel model;
  private PgVectorEmbeddingStore store;

  private Logger logger = LoggerFactory.getLogger(RagService.class);

  public RagService(EmbeddingModel model, PgVectorEmbeddingStore store) {
    this.model = model;
    this.store = store;
  }

  public RagService() {

    EmbeddingModel model = null;
    try {
      model = RagModelFactory.create();
    } catch (IOException e) {
      log.error("Can't load embedding model!", e);
    }
    this.model = model;

    PgVectorEmbeddingStore store = EmbeddingStoreFactory.create();

    this.store = store;
  }

  public List<QueryResult> query(String query) {
    // Add the query
    Embedding queryEmbedding = this.model.embed(query).content();

    // Run the query
    EmbeddingSearchRequest request =
        EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(10).build();

    EmbeddingSearchResult<TextSegment> searchResult = store.search(request);
    // Display the results

    List<QueryResult> results =
        searchResult.matches().stream()
            .filter(match -> match.score() > 0.7)
            .filter(
                match ->
                    match.embedded().metadata() != null
                        && match.embedded().metadata().containsKey(URL)
                        && match.embedded().metadata().containsKey(SOURCE))
            .map(RagService::getQueryResult)
            .toList();
    results.forEach(QueryResult::printResults);

    // maps to embbeding result
    return results;
  }

  @NotNull
  private static QueryResult getQueryResult(EmbeddingMatch<TextSegment> match) {
    TextSegment segment = match.embedded();
    Metadata metadata = segment.metadata();
    String url = metadata != null && metadata.containsKey(URL) ? metadata.getString(URL) : null;
    String source =
        metadata != null && metadata.containsKey(SOURCE) ? metadata.getString(SOURCE) : null;
    return new QueryResult(match.score(), source, url);
  }
}
