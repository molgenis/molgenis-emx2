package org.molgenis.emx2.rdf;

import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

abstract class InMemoryRDFHandler implements RDFHandler {
  public Map<Resource, Map<IRI, List<Value>>> resources = new HashMap<>();
  public List<Namespace> namespaces = new ArrayList<>();

  @Override
  public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
    namespaces.add(Values.namespace(prefix, uri));
  }

  @Override
  public void handleStatement(Statement st) throws RDFHandlerException {
    resources.merge(
        st.getSubject(),
        Map.of(st.getPredicate(), Collections.singletonList(st.getObject())),
        (oldValue, newValue) -> {
          Map<IRI, List<Value>> updatedMap = new HashMap<>(oldValue);
          for (var entry : newValue.entrySet()) {
            updatedMap.merge(
                entry.getKey(),
                entry.getValue(),
                (oldList, newList) -> {
                  Set<Value> updatedSet = new HashSet<>(oldList);
                  updatedSet.addAll(newList);
                  return new ArrayList<>(updatedSet);
                });
          }
          return updatedMap;
        });
  }

  @Override
  public void startRDF() throws RDFHandlerException {}

  @Override
  public void endRDF() throws RDFHandlerException {}

  @Override
  public void handleComment(String comment) throws RDFHandlerException {}
}
