package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecursiveFetcher {
  private static final Logger log = LoggerFactory.getLogger(RecursiveFetcher.class);
  private final RdfSource source;
  private final FetchErrorBehavior errorBehavior;

  public RecursiveFetcher(RdfSource source) {
    this(source, FetchErrorBehavior.WARN_AND_CONTINUE);
  }

  public RecursiveFetcher(RdfSource source, FetchErrorBehavior errorBehavior) {
    this.source = source;
    this.errorBehavior = errorBehavior;
  }

  public Model fetch(String url, int maxDepth, int maxCalls) throws IOException {
    Model model = new TreeModel();
    Set<String> fetched = new HashSet<>();
    int callCount = 0;
    model.addAll(source.fetch(url));
    fetched.add(url);
    callCount++;
    for (int depth = 0; depth < maxDepth && callCount < maxCalls; depth++) {
      Set<String> urisToFetch = extractObjectUris(model, fetched);
      if (urisToFetch.isEmpty()) {
        break;
      }
      for (String uri : urisToFetch) {
        if (callCount >= maxCalls) {
          log.warn("maxCalls limit ({}) reached, skipping remaining URIs", maxCalls);
          break;
        }
        try {
          Model linkedModel = source.fetch(uri);
          model.addAll(linkedModel);
          fetched.add(uri);
          callCount++;
        } catch (IOException e) {
          if (errorBehavior == FetchErrorBehavior.FAIL_FAST) {
            throw new DcatHarvestException("Failed to fetch " + uri + ": " + e.getMessage(), e);
          }
          log.warn("Failed to fetch {}: {}", uri, e.getMessage());
          fetched.add(uri);
        }
      }
    }
    return model;
  }

  private Set<String> extractObjectUris(Model model, Set<String> alreadyFetched) {
    Set<String> uris = new HashSet<>();
    for (Statement stmt : model) {
      Value obj = stmt.getObject();
      if (obj.isIRI() && !alreadyFetched.contains(obj.stringValue())) {
        uris.add(obj.stringValue());
      }
    }
    return uris;
  }
}
