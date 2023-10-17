package org.molgenis.emx2.semantics.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public class RootToRDF {
  private RootToRDF() {
    // static only
  }

  public static void describeRoot(ModelBuilder builder, String rootContext) {
    // SIO:000750 = database
    builder.add(rootContext, RDF.TYPE, iri("http://semanticscience.org/resource/SIO_000750"));
    builder.add(rootContext, RDFS.LABEL, "EMX2");
    builder.add(rootContext, DCTERMS.DESCRIPTION, "MOLGENIS EMX2 database at " + rootContext);
    builder.add(rootContext, DCTERMS.CREATOR, iri("https://molgenis.org"));
  }
}
