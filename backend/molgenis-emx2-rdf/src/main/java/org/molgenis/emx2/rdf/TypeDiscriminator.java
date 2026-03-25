package org.molgenis.emx2.rdf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

public class TypeDiscriminator {

  public record TableAssignment(String tableName, String typeValue) {}

  private static final Map<IRI, TableAssignment> PRIORITY_MAP = new LinkedHashMap<>();

  static {
    PRIORITY_MAP.put(
        Values.iri("http://www.w3.org/ns/dcat#Catalog"),
        new TableAssignment("Resources", "Catalogue"));
    PRIORITY_MAP.put(
        Values.iri("http://www.w3.org/ns/dcat#Dataset"),
        new TableAssignment("Resources", "Cohort study"));
    PRIORITY_MAP.put(
        Values.iri("http://www.w3.org/ns/dcat#DatasetSeries"),
        new TableAssignment("Resources", "Cohort study"));
    PRIORITY_MAP.put(
        Values.iri("http://xmlns.com/foaf/0.1/Agent"), new TableAssignment("Organisations", null));
    PRIORITY_MAP.put(
        Values.iri("http://www.w3.org/ns/org#Organization"),
        new TableAssignment("Organisations", null));
    PRIORITY_MAP.put(
        Values.iri("http://www.w3.org/2006/vcard/ns#Individual"),
        new TableAssignment("Contacts", null));
  }

  public static TableAssignment assignTable(Set<IRI> subjectTypes) {
    for (Map.Entry<IRI, TableAssignment> entry : PRIORITY_MAP.entrySet()) {
      if (subjectTypes.contains(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }
}
