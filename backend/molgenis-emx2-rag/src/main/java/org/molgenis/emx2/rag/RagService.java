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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RagService {
  private static final String URL = "url";
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
      logger.error("Can't load embedding model!", e);
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
        EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(100).build();

    EmbeddingSearchResult<TextSegment> searchResult = store.search(request);
    // Display the results

    Map<String, List<EmbeddingMatch<TextSegment>>> grouped =
        searchResult.matches().stream()
            .filter(match -> match.score() > 0.7) // slightly lower threshold
            .filter(match -> match.embedded().metadata() != null)
            .filter(match -> match.embedded().metadata().containsKey(URL))
            .collect(Collectors.groupingBy(match -> match.embedded().metadata().getString(URL)));
    grouped
        .entrySet()
        .forEach(
            entry -> {
              String url = entry.getKey();
              List<EmbeddingMatch<TextSegment>> matches = entry.getValue();
              logger.info("URL: {}, Matches: {}", url, matches.size());
            });

    Map<String, Double> docScores =
        grouped.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      return e.getValue().stream()
                          .sorted(
                              Comparator.comparingDouble(
                                      (EmbeddingMatch<TextSegment> m) -> m.score())
                                  .reversed())
                          .limit(3) // top 3 chunks
                          .mapToDouble(EmbeddingMatch::score)
                          .sum();
                    }));

    List<QueryResult> results =
        docScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(30)
            .map(
                entry -> {
                  String url = entry.getKey();
                  List<EmbeddingMatch<TextSegment>> matches = grouped.get(url);

                  // pick best chunk as preview
                  EmbeddingMatch<TextSegment> bestMatch =
                      matches.stream()
                          .max(Comparator.comparingDouble(EmbeddingMatch::score))
                          .orElse(null);
                  return getQueryResult(bestMatch);
                })
            .toList();

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
