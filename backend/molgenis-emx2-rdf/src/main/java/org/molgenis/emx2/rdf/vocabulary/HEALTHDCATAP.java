package org.molgenis.emx2.rdf.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Constants for the FAIR Data Point Ontology, which the RDF4J vocabulary does not contain yet.
 *
 * @see https://rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/vocabulary/eu/HealthDCATAP.html
 */
public final class HEALTHDCATAP {

  public static final String NAMESPACE = "http://healthdataportal.eu/ns/health#";

  public static final IRI HAS_VARIABLES = Values.iri(NAMESPACE, "hasVariables");

  private HEALTHDCATAP() {}
}
