package org.molgenis.emx2.rag;

import static org.molgenis.emx2.rag.RagConstants.*;
import static org.molgenis.emx2.rag.RagConstants.CATALOGUE_LOCATION;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Embeddor {
  private static final String URL = "url";
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static final String SOURCE = "source";

  public static void main(String[] args) throws IOException {

    System.out.println("Start embedding");
    long startTime = System.currentTimeMillis();

    EmbeddingModel embeddingModel = RagModelFactory.create();

    PgVectorEmbeddingStore embeddingStore = EmbeddingStoreFactory.create();
    //    InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    EmbeddingStoreIngestor ingestor =
        EmbeddingStoreIngestor.builder()
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build();

    List<String> locations = new SiteMapReader(CATALOGUE_LOCATION).readLocations();

    System.out.println("Number of documents found in site map: " + locations.size());

    boolean useLimit = PARSE_LIMIT > 0;
    int runs = useLimit ? PARSE_LIMIT : locations.size();

    for (int i = 0; i < runs; i++) {
      String url = locations.get(i);
      String[] pathSections = url.split("/");
      boolean isCollection = Arrays.asList(pathSections).contains("collections");
      boolean isEmbedding = Arrays.asList(pathSections).contains("variables");
      String type = isCollection ? "collections " : isEmbedding ? "variables " : "other ";
      System.out.println(
          "Parsing url(" + i + "/" + locations.size() + "): " + url + " type: " + type);

      Document doc = getDocument(locations.get(i));
      if (doc != null) {
        doc.metadata().put(SOURCE, type);
        doc.metadata().put(URL, url);
        ingestor.ingest(doc);
      }
    }

    long endTime = System.currentTimeMillis() - startTime;
    System.out.println("Embedding completed in: " + endTime / 1000 + " seconds");
  }

  private static Document getDocument(String url) {
    try {
      org.jsoup.nodes.Document jsoupDocument = org.jsoup.Jsoup.connect(url).get();
      jsoupDocument.select("script, style, noscript").remove();
      String text = jsoupDocument.body().text();
      return Document.from(text);
    } catch (IOException e) {
      log.error("Failed to fetch document from url: {}", url, e);
      return null;
    }
  }
}
