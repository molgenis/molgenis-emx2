package org.molgenis.emx2.rdf.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/** Constants for the FAIR Data Point Ontology, which RDF4J does not ship a vocabulary for. */
public final class HEALTHDCATAP {

  public static final String NAMESPACE = "http://healthdataportal.eu/ns/health#";

  public static final IRI HAS_VARIABLES = Values.iri(NAMESPACE, "hasVariables");

  private HEALTHDCATAP() {}
}
