package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;

public class RdfValidator {
  public Map<Resource, Map<IRI, Map<Value, Boolean>>> validations = new HashMap<>();

  public RdfValidator add(Triple triple, boolean isPresent) {
    return add(triple.getSubject(), triple.getPredicate(), triple.getObject(), isPresent);
  }

  public RdfValidator add(String subject, String predicate, String object, boolean isPresent) {
    return add(subject, predicate, object, false, isPresent);
  }

  public RdfValidator add(
      String subject, String predicate, String object, boolean objectIsIRI, boolean isPresent) {
    Value objectValue = (objectIsIRI ? Values.iri(object) : Values.literal(object));
    return add((Resource) Values.iri(subject), (IRI) Values.iri(predicate), objectValue, isPresent);
  }

  public RdfValidator add(Resource subject, IRI predicate, Value object, boolean isPresent) {
    validations.putIfAbsent(subject, new HashMap<>());
    Map<IRI, Map<Value, Boolean>> predicates = validations.get(subject);
    predicates.putIfAbsent(predicate, new HashMap<>());
    Map<Value, Boolean> subjects = predicates.get(predicate);
    subjects.put(object, isPresent);
    return this;
  }

  public RdfValidator addAll(Collection<Triple> triples, boolean isPresent) {
    triples.forEach(i -> add(i, isPresent));
    return this;
  }

  void validate(InMemoryRDFHandler handler) {
    // Tracks errors.
    List<String> errors = new ArrayList<>();

    // Compares expected with actual.
    for (Map.Entry<Resource, Map<IRI, Map<Value, Boolean>>> validationSubject :
        validations.entrySet()) {
      Map<IRI, Set<Value>> foundPredicates =
          handler.resources.getOrDefault(validationSubject.getKey(), null);

      for (Map.Entry<IRI, Map<Value, Boolean>> validationPredicate :
          validationSubject.getValue().entrySet()) {
        Set<Value> foundObjects;
        if (foundPredicates != null) {
          foundObjects = foundPredicates.getOrDefault(validationPredicate.getKey(), null);
        } else {
          foundObjects = null;
        }
        for (Map.Entry<Value, Boolean> validationObject :
            validationPredicate.getValue().entrySet()) {
          if (validationObject.getValue()
              && (foundObjects == null || !foundObjects.contains(validationObject.getKey()))) {
            errors.add(
                String.format(
                    "Triple <<%s %s %s>> is expected but not found",
                    validationSubject.getKey(),
                    validationPredicate.getKey(),
                    validationObject.getKey()));
          } else if (!validationObject.getValue()
              && foundObjects != null
              && foundObjects.contains(validationObject.getKey())) {
            errors.add(
                String.format(
                    "Triple <<%s %s %s>> is found while it should not be present",
                    validationSubject.getKey(),
                    validationPredicate.getKey(),
                    validationObject.getKey()));
          }
        }
      }
    }

    // Compares error ArrayList to empty one so actual messages are shown if any are found.
    assertEquals(new ArrayList<>(), errors);
  }
}
