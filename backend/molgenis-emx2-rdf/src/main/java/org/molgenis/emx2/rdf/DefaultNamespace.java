package org.molgenis.emx2.rdf;

import java.util.Arrays;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

public enum DefaultNamespace {
  RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
  RDFS("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
  XSD("xsd", "http://www.w3.org/2001/XMLSchema#"),
  OWL("owl", "http://www.w3.org/2002/07/owl#"),
  SIO("sio", "http://semanticscience.org/resource/"),
  QB("qb", "http://purl.org/linked-data/cube#"),
  SKOS("skos", "http://www.w3.org/2004/02/skos/core#"),
  DCTERMS("dcterms", "http://purl.org/dc/terms/"),
  DCAT("dcat", "http://www.w3.org/ns/dcat#"),
  FOAF("foaf", "http://xmlns.com/foaf/0.1/"),
  VCARD("vcard", "http://www.w3.org/2006/vcard/ns#"),
  ORG("org", "http://www.w3.org/ns/org#"),
  FDP("fdp-o", "https://w3id.org/fdp/fdp-o#");

  private final Namespace namespace;

  public Namespace getNamespace() {
    return namespace;
  }

  DefaultNamespace(String prefix, String namespace) {
    this.namespace = new SimpleNamespace(prefix, namespace);
  }

  public static Stream<Namespace> streamAll() {
    return Arrays.stream(DefaultNamespace.values()).map(DefaultNamespace::getNamespace);
  }
}
