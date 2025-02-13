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
  OBO("obo", "http://purl.obolibrary.org/obo/"),
  ORDO("ordo", "http://www.orpha.net/ORDO/"),
  EDAM("edam", "http://edamontology.org/"),
  AFR("arf", "http://purl.allotrope.org/ontologies/result#"),
  AFRL("afrl", "http://purl.allotrope.org/ontologies/role#"),
  EFO("efo", "http://www.ebi.ac.uk/efo/"),
  FG("fg", "https://w3id.org/fair-genomes/resource/"),
  SCHEMA("schema", "http://schema.org/"),
  HL7("hl7", "http://purl.bioontology.org/ontology/HL7/"),
  SNOMEDCT("snomedct", "http://purl.bioontology.org/ontology/SNOMEDCT/"),
  LNC("lnc", "http://purl.bioontology.org/ontology/LNC/"),
  LDP("ldp", "http://www.w3.org/ns/ldp#"),
  EJP("ejp", "https://w3id.org/ejp-rd/vocabulary#"),
  ENSEMBL("ensembl", "http://ensembl.org/glossary/"),
  MESH("mesh", "http://purl.bioontology.org/ontology/MESH/"),
  FDP("fdp-o", "http://w3id.org/fdp/fdp-o#"),
  HEALTH_DCAT_AP("healthDCAT-AP", "urn:uuid:a7ef52b2-bd43-4294-a80f-3e7299af35e4#");

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
