package org.molgenis.emx2.rdf;

import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/** A RDF Handler that stores the RDF in memory for usage in testing. */
abstract class InMemoryRDFHandler implements RDFHandler {
  public Map<Resource, Map<IRI, Set<Value>>> resources = new HashMap<>();
  public Set<Namespace> namespaces = new HashSet<>();

  @Override
  public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
    namespaces.add(Values.namespace(prefix, uri));
  }

  @Override
  public void handleStatement(Statement st) throws RDFHandlerException {
    // Merge the statement with existing statements for this subject.
    var statementsForSubject =
        resources.getOrDefault(st.getSubject(), new HashMap<IRI, Set<Value>>());
    var values = statementsForSubject.getOrDefault(st.getPredicate(), new HashSet<Value>());
    values.add(st.getObject());
    statementsForSubject.put(st.getPredicate(), values);
    resources.put(st.getSubject(), statementsForSubject);
  }

  @Override
  public void startRDF() throws RDFHandlerException {}

  @Override
  public void endRDF() throws RDFHandlerException {}

  @Override
  public void handleComment(String comment) throws RDFHandlerException {}
}
