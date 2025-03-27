package org.molgenis.emx2.rdf;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

public enum BasicIRI {
  /**
   * DCAT:endpointURL is the 'root' location, which is the schema. see:
   * https://www.w3.org/TR/vocab-dcat-3/#Property:data_service_endpoint_url
   */
  DCAT_ENDPOINTURL("http://www.w3.org/ns/dcat#endpointURL"),
  /**
   * FDP-O:metadataIdentifier is the identifier of the metadata entry, which is the subject itself.
   * See: https://specs.fairdatapoint.org/fdp-specs-v1.2.html
   */
  FDP_METADATAIDENTIFIER("https://w3id.org/fdp/fdp-o#metadataIdentifier"),
  LDP_CONTAINS("http://www.w3.org/ns/ldp#contains"),
  LD_DATASET_CLASS("http://purl.org/linked-data/cube#DataSet"),
  LD_DATASET_PREDICATE("http://purl.org/linked-data/cube#dataSet"),
  LD_OBSERVATION("http://purl.org/linked-data/cube#Observation"),
  MOLGENIS("https://molgenis.org"),
  NCIT_CODED_VALUE_DATA_TYPE("http://purl.obolibrary.org/obo/NCIT_C95637"),
  NCIT_CONTROLLED_VOCABULARY("http://purl.obolibrary.org/obo/NCIT_C48697"),
  SIO_DATABASE("http://semanticscience.org/resource/SIO_000750"),
  SIO_DATABASE_TABLE("http://semanticscience.org/resource/SIO_000754"),
  SIO_FILE("http://semanticscience.org/resource/SIO_000396"),
  SIO_IDENTIFIER("http://semanticscience.org/resource/SIO_000115"),
  /**
   * SIO:001055 = observing (definition: observing is a process of passive interaction in which one
   * entity makes note of attributes of one or more entities)
   */
  SIO_OBSERVING("http://semanticscience.org/resource/SIO_001055");

  private final IRI iri;

  public IRI getIri() {
    return iri;
  }

  BasicIRI(String iri) {
    this.iri = Values.iri(iri);
  }
}
