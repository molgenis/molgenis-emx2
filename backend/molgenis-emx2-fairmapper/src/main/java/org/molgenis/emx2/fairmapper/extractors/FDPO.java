package org.molgenis.emx2.fairmapper.extractors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/** Constants for the FAIR Data Point Ontology, which RDF4J does not ship a vocabulary for. */
public final class FDPO {

  public static final String NAMESPACE = "https://w3id.org/fdp/fdp-o#";

  public static final IRI METADATA_CATALOG = Values.iri(NAMESPACE, "metadataCatalog");

  private FDPO() {}
}
