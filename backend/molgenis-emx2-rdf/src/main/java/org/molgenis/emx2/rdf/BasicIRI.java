package org.molgenis.emx2.rdf;

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;

/**
 * Basic IRIs not defined by RDF4J but used within EMX2 to generate triples (can be called similar
 * to {@link org.eclipse.rdf4j.model.vocabulary.RDFS}).
 */
public class BasicIRI {
  /**
   * DCAT:endpointURL is the 'root' location, which is the schema. see:
   * https://www.w3.org/TR/vocab-dcat-3/#Property:data_service_endpoint_url
   */
  public static final IRI DCAT_ENDPOINTURL = iri("http://www.w3.org/ns/dcat#endpointURL");

  /**
   * FDP-O:metadataIdentifier is the identifier of the metadata entry, which is the subject itself.
   * See: https://specs.fairdatapoint.org/fdp-specs-v1.2.html
   */
  public static final IRI FDP_METADATAIDENTIFIER =
      iri("https://w3id.org/fdp/fdp-o#metadataIdentifier");

  public static final IRI LDP_CONTAINS = iri("http://www.w3.org/ns/ldp#contains");
  public static final IRI LD_DATASET_CLASS = iri("http://purl.org/linked-data/cube#DataSet");
  public static final IRI LD_DATASET_PREDICATE = iri("http://purl.org/linked-data/cube#dataSet");
  public static final IRI LD_OBSERVATION = iri("http://purl.org/linked-data/cube#Observation");
  public static final IRI MOLGENIS = iri("https://molgenis.org");
  public static final IRI NCIT_CODED_VALUE_DATA_TYPE =
      iri("http://purl.obolibrary.org/obo/NCIT_C95637");
  public static final IRI NCIT_CONTROLLED_VOCABULARY =
      iri("http://purl.obolibrary.org/obo/NCIT_C48697");
  public static final IRI SIO_DATABASE = iri("http://semanticscience.org/resource/SIO_000750");
  public static final IRI SIO_DATABASE_TABLE =
      iri("http://semanticscience.org/resource/SIO_000754");
  public static final IRI SIO_FILE = iri("http://semanticscience.org/resource/SIO_000396");
  public static final IRI SIO_IDENTIFIER = iri("http://semanticscience.org/resource/SIO_000115");

  /**
   * SIO:001055 = observing (definition: observing is a process of passive interaction in which one
   * entity makes note of attributes of one or more entities)
   */
  public static final IRI SIO_OBSERVING = iri("http://semanticscience.org/resource/SIO_001055");
}
