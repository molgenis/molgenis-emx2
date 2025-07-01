package org.molgenis.emx2.rdf;

import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;
import static org.molgenis.emx2.Constants.API_FILE;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * An RDF Handler that stores the RDF in memory for usage in testing. Additionally, it contains
 * certain behaviours to allow for comparing RDF data stored in EMX2 that would otherwise cause a
 * failure while for comparison this is not beneficial. These include:
 *
 * <ul>
 *   <li>The UUID within a FILE API path will be replaced with the RDFS:label if possible, otherwise
 *       a generic FILE API path will be used (so that a different UUID will not cause a failure
 *       during comparison).
 *   <li>Datetime predicates regarding creation/modification will be replaced with a single value so
 *       that different creation/modification dates do not make the comparison fail.
 * </ul>
 *
 * This behaviour can be disabled by calling the constructor with `false`.
 */
class InMemoryRDFHandler implements RDFHandler {
  private static final DatatypeFactory datatypeFactory;
  private static final XMLGregorianCalendar REPLACEMENT_DATE;

  private boolean fixValuesForComparison = true;
  private final Map<Value, Value> fileIriMappings = new HashMap<>();

  public Map<Resource, Map<IRI, Set<Value>>> resources = new HashMap<>();
  public Set<Namespace> namespaces = new HashSet<>();

  static {
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new RuntimeException(e);
    }

    REPLACEMENT_DATE =
        datatypeFactory.newXMLGregorianCalendar(
            2021, 2, 8, 12, 15, 0, FIELD_UNDEFINED, FIELD_UNDEFINED);
  }

  InMemoryRDFHandler() {}

  InMemoryRDFHandler(boolean fixValuesForComparison) {
    this.fixValuesForComparison = fixValuesForComparison;
  }

  @Override
  public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
    namespaces.add(Values.namespace(prefix, uri));
  }

  @Override
  public void handleStatement(Statement st) throws RDFHandlerException {
    Resource subject = st.getSubject();
    IRI predicate = st.getPredicate();
    Value object = st.getObject();

    // Replace creation/modified date to prevent failing tests.
    if (fixValuesForComparison
        && (predicate.equals(DCTERMS.CREATED)
            || predicate.equals(DCTERMS.MODIFIED)
            || predicate.stringValue().endsWith("/column/mg_insertedOn")
            || predicate.stringValue().endsWith("/column/mg_updatedOn"))) {
      object = Values.literal(REPLACEMENT_DATE);
    }

    addStatement(subject, predicate, object);
  }

  private void addStatement(Resource subject, IRI predicate, Value object) {
    // Merge the statement with existing statements for this subject.
    Map<IRI, Set<Value>> statementsForSubject = resources.getOrDefault(subject, new HashMap<>());
    Set<Value> values = statementsForSubject.getOrDefault(predicate, new HashSet<>());
    values.add(object);
    statementsForSubject.put(predicate, values);
    resources.put(subject, statementsForSubject);
  }

  @Override
  public void startRDF() throws RDFHandlerException {}

  @Override
  public void endRDF() throws RDFHandlerException {
    if (fixValuesForComparison) {
      processFileIris();
    }
  }

  private void processFileIris() {
    // Replace UUID in FILE API subjects with the RDFS label (if subject is present).
    // If subject is found, assumes rdfs:label predicate with 1 object is present.
    Set<Value> subjectsToReplace =
        resources.keySet().stream()
            .filter(Resource::isIRI)
            .filter(i -> i.stringValue().contains(API_FILE))
            .collect(Collectors.toUnmodifiableSet());

    for (Value subject : subjectsToReplace) {
      Map<IRI, Set<Value>> value = resources.remove(subject);
      String label = value.get(RDFS.LABEL).stream().findFirst().get().stringValue();
      Resource newKey = createNewFileIri(subject, label);
      resources.put(newKey, value);
      fileIriMappings.put(subject, newKey);
    }

    // Replace objects with their new IRI.
    if (fileIriMappings.isEmpty()) {
      // Fallback in case no rdfs:label data is present for file IRIs.
      // The objects will all be identical as otherwise due to UUID tests would fail.
      for (Map<IRI, Set<Value>> predicateObjects : resources.values()) {
        for (Set<Value> objects : predicateObjects.values()) {
          for (Value object : objects) {
            if (object.isIRI() && object.stringValue().contains(API_FILE)) {
              objects.remove(object);
              objects.add(createNewFileIri(object, "identicalFileIRI"));
            }
          }
        }
      }
    } else { // Replace objects with their label-IRIs where possible.
      for (Map<IRI, Set<Value>> predicateObjects : resources.values()) {
        for (Set<Value> objects : predicateObjects.values()) {
          for (Value intersection :
              CollectionUtils.intersection(objects, fileIriMappings.keySet())) {
            objects.remove(intersection);
            objects.add(fileIriMappings.get(intersection));
          }
        }
      }
    }
  }

  private Resource createNewFileIri(Value value, String identifier) {
    int lastSlashPos = value.stringValue().lastIndexOf("/");
    return Values.iri(value.stringValue().substring(0, lastSlashPos + 1) + identifier);
  }

  @Override
  public void handleComment(String comment) throws RDFHandlerException {}
}
