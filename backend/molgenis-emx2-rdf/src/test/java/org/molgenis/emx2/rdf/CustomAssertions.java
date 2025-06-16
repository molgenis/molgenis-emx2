package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.AssertionFailureBuilder;

public abstract class CustomAssertions {
  public static void equals(InMemoryRDFHandler expected, InMemoryRDFHandler actual) {
    AssertionFailureBuilder failureBuilder = assertionFailure();

    if (!expected.namespaces.equals(actual.namespaces)) {
      failureBuilder
          .message("Namespaces are not equal")
          .expected(expected.namespaces.stream().sorted().toList())
          .actual(actual.namespaces.stream().sorted().toList())
          .buildAndThrow();
    }

    if (!expected.resources.equals(actual.resources)) {
      Collection<Resource> subjectsUnion =
          CollectionUtils.union(expected.resources.keySet(), actual.resources.keySet());
      for (Resource subject : subjectsUnion) {
        if (!expected.resources.containsKey(subject)) {
          failureBuilder
              .message("Actual contains unexpected subject")
              .expected("")
              .actual(subject)
              .buildAndThrow();
        }

        if (!actual.resources.containsKey(subject)) {
          failureBuilder
              .message("Actual missing subject")
              .expected(subject)
              .actual("")
              .buildAndThrow();
        }

        Map<IRI, Set<Value>> expectedPredicateMaps = expected.resources.get(subject);
        Map<IRI, Set<Value>> actualPredicateMaps = actual.resources.get(subject);

        if (!expectedPredicateMaps.equals(actualPredicateMaps)) {
          failureBuilder
              .message("Predicates/objects for subject \"" + subject + "\" do not match")
              .expected(expectedPredicateMaps)
              .actual(actualPredicateMaps)
              .buildAndThrow();
        }
      }
    }
  }
}
