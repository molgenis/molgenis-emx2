package org.molgenis.emx2.rdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;

public class FilteringRdfHandler implements RDFHandler {

  private static final IRI RDF_TYPE = Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

  private final Map<IRI, List<ColumnMapping>> predicateMap;
  private final Map<Resource, Set<IRI>> typeMap = new HashMap<>();
  private final Map<Resource, Map<ColumnMapping, List<Value>>> matchedData = new HashMap<>();
  private long unmatchedCount = 0;

  public FilteringRdfHandler(Map<IRI, List<ColumnMapping>> predicateMap) {
    this.predicateMap = predicateMap;
  }

  @Override
  public void handleStatement(Statement st) throws RDFHandlerException {
    Resource subject = st.getSubject();
    IRI predicate = st.getPredicate();
    Value object = st.getObject();

    if (RDF_TYPE.equals(predicate)) {
      typeMap.computeIfAbsent(subject, key -> new HashSet<>()).add((IRI) object);
      return;
    }

    List<ColumnMapping> mappings = predicateMap.get(predicate);
    if (mappings != null) {
      Map<ColumnMapping, List<Value>> subjectData =
          matchedData.computeIfAbsent(subject, key -> new HashMap<>());
      for (ColumnMapping mapping : mappings) {
        subjectData.computeIfAbsent(mapping, key -> new ArrayList<>()).add(object);
      }
    } else {
      unmatchedCount++;
    }
  }

  public Map<Resource, Set<IRI>> getTypeMap() {
    return typeMap;
  }

  public Map<Resource, Map<ColumnMapping, List<Value>>> getMatchedData() {
    return matchedData;
  }

  public long getUnmatchedCount() {
    return unmatchedCount;
  }

  @Override
  public void startRDF() throws RDFHandlerException {}

  @Override
  public void endRDF() throws RDFHandlerException {}

  @Override
  public void handleNamespace(String prefix, String uri) throws RDFHandlerException {}

  @Override
  public void handleComment(String comment) throws RDFHandlerException {}
}
