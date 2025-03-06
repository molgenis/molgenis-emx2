package org.molgenis.emx2.rdf;

import java.util.Arrays;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

public enum DefaultNamespace {
  AFR("afr", "http://purl.allotrope.org/ontologies/result#"),
  AFRL("afrl", "http://purl.allotrope.org/ontologies/role#"),
  DC("dc", "http://purl.org/dc/elements/1.1/"),
  DCAT("dcat", "http://www.w3.org/ns/dcat#"),
  DCTERMS("dcterms", "http://purl.org/dc/terms/"),
  EDAM("edam", "http://edamontology.org/"),
  EFO("efo", "http://www.ebi.ac.uk/efo/"),
  EJP("ejp", "https://w3id.org/ejp-rd/vocabulary#"),
  ENSEMBL("ensembl", "http://ensembl.org/glossary/"),
  FDP("fdp-o", "http://w3id.org/fdp/fdp-o#"),
  FG("fg", "https://w3id.org/fair-genomes/resource/"),
  FOAF("foaf", "http://xmlns.com/foaf/0.1/"),
  HEALTHDCAT_AP("healthDCAT-AP", "urn:uuid:a7ef52b2-bd43-4294-a80f-3e7299af35e4#"),
  HL7("hl7", "http://purl.bioontology.org/ontology/HL7/"),
  LDP("ldp", "http://www.w3.org/ns/ldp#"),
  LNC("lnc", "http://purl.bioontology.org/ontology/LNC/"),
  MESH("mesh", "http://purl.bioontology.org/ontology/MESH/"),
  OBO("obo", "http://purl.obolibrary.org/obo/"),
  OBO_IN_OWL("oboInOwl", "http://www.geneontology.org/formats/oboInOwl#"),
  ODRL("odrl", "http://www.w3.org/ns/odrl/2/"),
  ORDO("ordo", "http://www.orpha.net/ORDO/"),
  ORG("org", "http://www.w3.org/ns/org#"),
  OWL("owl", "http://www.w3.org/2002/07/owl#"),
  PROV("prov", "http://www.w3.org/ns/prov#"),
  QB("qb", "http://purl.org/linked-data/cube#"),
  RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
  RDFS("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
  SCHEMA("schema", "http://schema.org/"),
  SIO("sio", "http://semanticscience.org/resource/"),
  SKOS("skos", "http://www.w3.org/2004/02/skos/core#"),
  SNOMEDCT("snomedct", "http://purl.bioontology.org/ontology/SNOMEDCT/"),
  VCARD("vcard", "http://www.w3.org/2006/vcard/ns#"),
  XSD("xsd", "http://www.w3.org/2001/XMLSchema#");

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
