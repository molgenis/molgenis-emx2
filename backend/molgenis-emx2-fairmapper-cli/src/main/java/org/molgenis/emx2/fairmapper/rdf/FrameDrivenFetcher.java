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

  public Model fetch(String url, JsonNode frame, int maxDepth) throws IOException {
    Map<Integer, List<String>> predicatesByDepth = analyzer.analyze(frame, maxDepth);

    Model model = new TreeModel();
    model.addAll(source.fetch(url));

    for (int depth = 0; depth <= maxDepth; depth++) {
      List<String> predicates = predicatesByDepth.get(depth);
      if (predicates == null || predicates.isEmpty()) {
        continue;
      }

      for (String predicate : predicates) {
        Set<String> urisToFetch = extractObjectUris(model, predicate);
        for (String uri : urisToFetch) {
          try {
            Model linkedModel = source.fetch(uri);
            model.addAll(linkedModel);
          } catch (IOException e) {
            System.err.println("Warning: Failed to fetch " + uri + ": " + e.getMessage());
          }
        }
      }
    }

    return model;
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
