package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

/** Tests specific for {@link org.molgenis.emx2.rdf.generators.Emx2RdfGenerator} */
class Emx2RdfsOntologyTest extends OntologyTest {
  @Test
  void testThatOntologyTermsUseRDFSchema() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(ontologyTest, "Diseases");

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/Diseases/U07.1")) {
        Map<IRI, Set<Value>> data = handler.resources.get(subject);
        assertTrue(data.containsKey(RDFS.LABEL), "The class should have a label");
        assertTrue(
            data.containsKey(RDFS.SUBCLASSOF),
            "Children should be defined as a subClass of a parent Class");
        assertTrue(
            data.containsKey(OWL.SAMEAS),
            "URLs to the canonical version should be defined a owl:sameAs");
        assertTrue(
            data.containsKey(RDFS.ISDEFINEDBY), "Definition should be given as rdsf:isDefinedBy");
        assertTrue(data.containsKey(SKOS.NOTATION), "Code should be defined as a skos:Notation");
      }
    }
  }

  /**
   * Ontology tables are describing classes.
   *
   * @see <a href="https://github.com/molgenis/molgenis-emx2/issues/2984">Issue #2997</a>
   * @throws IOException
   */
  @Test
  void testThatOntologyTermsAreClasses() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(ontologyTest, "Diseases");

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/Diseases/U07.1")) {
        Set<Value> types = handler.resources.get(subject).get(RDF.TYPE);
        assertTrue(types.contains(OWL.CLASS), "Ontology tables define classes");
      }
    }
  }
}
