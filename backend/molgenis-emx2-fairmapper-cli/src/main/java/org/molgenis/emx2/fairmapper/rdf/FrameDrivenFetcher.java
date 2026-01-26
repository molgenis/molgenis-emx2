package org.molgenis.emx2.fairmapper.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;

public class FrameDrivenFetcher {
  private final RdfSource source;
  private final FrameAnalyzer analyzer;
  private final ValueFactory vf;

  public FrameDrivenFetcher(RdfSource source, FrameAnalyzer analyzer) {
    this.source = source;
    this.analyzer = analyzer;
    this.vf = SimpleValueFactory.getInstance();
  }

  public Model fetch(String url, JsonNode frame, int maxDepth, int maxCalls) throws IOException {
    Map<Integer, List<String>> predicatesByDepth = analyzer.analyze(frame, maxDepth);

    Model model = new TreeModel();
    Set<String> fetched = new HashSet<>();
    int callCount = 0;

    model.addAll(source.fetch(url));
    fetched.add(url);
    callCount++;

    for (int depth = 0; depth <= maxDepth && callCount < maxCalls; depth++) {
      List<String> predicates = predicatesByDepth.get(depth);
      if (predicates == null || predicates.isEmpty()) {
        continue;
      }

      for (String predicate : predicates) {
        Set<String> urisToFetch = extractObjectUris(model, predicate);
        for (String uri : urisToFetch) {
          if (callCount >= maxCalls) {
            System.err.println(
                "Warning: maxCalls limit (" + maxCalls + ") reached, skipping remaining URIs");
            break;
          }
          if (fetched.contains(uri)) {
            continue;
          }
          try {
            Model linkedModel = source.fetch(uri);
            model.addAll(linkedModel);
            fetched.add(uri);
            callCount++;
          } catch (IOException e) {
            System.err.println("Warning: Failed to fetch " + uri + ": " + e.getMessage());
          }
        }
      }
    }

    return model;
  }

  public Model fetch(String url, JsonNode frame, int maxDepth) throws IOException {
    return fetch(url, frame, maxDepth, 50);
  }

  private Set<String> extractObjectUris(Model model, String predicateUri) {
    Set<String> uris = new HashSet<>();
    IRI predicate = vf.createIRI(predicateUri);

    for (Statement stmt : model.filter(null, predicate, null)) {
      Value obj = stmt.getObject();
      if (obj.isIRI()) {
        uris.add(obj.stringValue());
      }
    }

    return uris;
  }
}
