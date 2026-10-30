package org.molgenis.emx2.rdf.service;

import java.io.IOException;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;
import org.molgenis.emx2.rdf.RdfValidator;

/**
 * Checks presence of triples describing columns.
 *
 * @see org.molgenis.emx2.rdf.generators.Emx2RdfGenerator#describeColumn(Column)
 */
public class Emx2TableInheritanceTest extends TableInheritanceTest {
  @Test
  void testColumnsPresent_grandChild() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "GrandchildTypeA");
    new RdfValidator()
        .add(
            getIri(SCHEMA_NAME, "GrandchildTypeA/column/grandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            true)
        .add(getIri(SCHEMA_NAME, "Child/column/childColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, false)
        .add(getIri(SCHEMA_NAME, "Root/column/rootColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, false)
        .add(
            getIri(EXTERNAL_SCHEMA_NAME, "ExternalChild/column/externalChildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            false)
        .validate(handler);
  }

  @Test
  void testColumnsPresent_child() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Child");
    new RdfValidator()
        .add(
            getIri(SCHEMA_NAME, "GrandchildTypeA/column/grandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            true)
        .add(getIri(SCHEMA_NAME, "Child/column/childColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, true)
        .add(getIri(SCHEMA_NAME, "Root/column/rootColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, false)
        .add(
            getIri(EXTERNAL_SCHEMA_NAME, "ExternalChild/column/externalChildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            false)
        .validate(handler);
  }

  @Test
  void testColumnsPresent_root() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(tableInherTest, "Root");
    new RdfValidator()
        .add(
            getIri(SCHEMA_NAME, "GrandchildTypeA/column/grandchildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            true)
        .add(getIri(SCHEMA_NAME, "Child/column/childColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, true)
        .add(getIri(SCHEMA_NAME, "Root/column/rootColumn"), RDF.TYPE, OWL.DATATYPEPROPERTY, true)
        .add(
            getIri(EXTERNAL_SCHEMA_NAME, "ExternalChild/column/externalChildColumn"),
            RDF.TYPE,
            OWL.DATATYPEPROPERTY,
            false)
        .validate(handler);
  }
}
