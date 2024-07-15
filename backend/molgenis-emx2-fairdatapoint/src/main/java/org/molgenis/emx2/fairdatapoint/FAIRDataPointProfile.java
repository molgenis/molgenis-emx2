package org.molgenis.emx2.fairdatapoint;

public class FAIRDataPointProfile {

  /*
  As defined by https://specs.fairdatapoint.org/
   */
  public static final String FDP_SHACL =
      """
			@prefix      : <http://fairdatapoint.org/> .
			@prefix  dcat: <http://www.w3.org/ns/dcat#> .
			@prefix   dct: <http://purl.org/dc/terms/> .
			@prefix fdp-o: <http://purl.org/fdp/fdp-o#> .
			@prefix  foaf: <http://xmlns.com/foaf/0.1/> .
			@prefix   ldp: <http://www.w3.org/ns/ldp#> .
			@prefix    sh: <http://www.w3.org/ns/shacl#> .
			@prefix   xsd: <http://www.w3.org/2001/XMLSchema#> .

			:FAIRDataPointShape a sh:NodeShape ;
			  sh:targetClass fdp-o:MetadataService ;
			  sh:property [
			    sh:path dct:title ;
			    sh:nodeKind sh:Literal ;
			    sh:minCount 1 ;
			  ], [
			    sh:path dct:hasVersion ;
			    sh:nodeKind sh:Literal ;
			    sh:maxCount ;
			  ], [
			    sh:path dct:description ;
			    sh:nodeKind sh:Literal ;
			  ], [
			    sh:path fdp-o:metadataCatalog ;
			    sh:node :CatalogShape ;
			    sh:minCount 1;
			  ], [
			    sh:path dct:publisher ;
			    sh:node :AgentShape ;
			    sh:minCount 1;
			  ], [
			    sh:path dct:language ;
			    sh:nodeKind sh:IRI ;
			  ], [
			    sh:path dct:license ;
			    sh:nodeKind sh:IRI ;
			    sh:minCount 1;
			    sh:maxCount 1 ;
			  ], [
			    sh:path dct:conformsTo ;
			    sh:nodeKind sh:IRI ;
			    sh:maxCount 1;
			    sh:minCount 1;
			  ], [
			    sh:path dct:rights ;
			    sh:nodeKind sh:IRI ;
			  ], [
			    sh:path dct:accessRights ;
			    sh:nodeKind sh:IRI ;
			  ], [
			    sh:path dcat:contactPoint ;
			    sh:node sh:ContactPointShape ;
			  ], [
			    sh:path dcat:keyword ;
			    sh:nodeKind sh:Literal ;
			  ], [
			    sh:path dcat:theme ;
			    sh:nodeKind sh:IRI ;
			  ], [
			    sh:path dcat:endPointURL ;
			    sh:nodeKind sh:IRI ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ], [
			    sh:path fdp-o:startDate ;
			    sh:datatype xsd:date ;
			    sh:maxCount 1 ;
			  ], [
			    sh:path fdp-o:endDate ;
			    sh:datatype xsd:date ;
			    sh:maxCount 1 ;
			  ], [
			    sh:path fdp-o:metadataIdentifier ;
			    sh:nodeKind sh:IRI ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ], [
			    sh:path fdp-o:metadataIssued ;
			    sh:datatype xsd:dateTime ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ], [
			    sh:path fdp-o:metadataIdentifier ;
			    sh:datatype xsd:dateTime ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ], [
			    sh:path dct:conformsToFdpSpec ;
			    sh:nodeKind sh:IRI ;
			    sh:maxCount 1;
			    sh:minCount 1;
			  ].

			:AgentShape a sh:NodeShape ;
			  sh:targetClass foaf:Agent ;
			  sh:property [
			    sh:path foaf:name ;
			    sh:nodeKind sh:Literal ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ].

			:ContactPointShape a sh:NodeShape ;
			  sh:targetClass vcard:Kind ;
			  sh:property [
			    sh:path vcard:hasEmail ;
			    sh:nodeKind sh:Literal ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ].

			:FAIRDataPointContainerShape a sh:NodeShape ;
			  sh:targetClass ldp:DirectContainer ;
			  sh:property [
			    sh:name "title" ;
			    sh:description "Name identifying the member resources" ;
			    sh:path dct:title ;
			    sh:nodeKind sh:Literal ;
			    sh:minCount 1 ;
			    sh:uniqueLang true ;
			  ], [
			    sh:name "membership resource" ;
			    sh:description "" ;
			    sh:path ldp:membershipResource ;
			    sh:nodeKind fdp-o:FairDataPoint ;
			    sh:maxCount 1 ;
			  ], [
			    sh:name "has member relation" ;
			    sh:description "The predicate used in the metadata to relate the FAIR Data Point with its member catalogs." ;
			    sh:path ldp:hasMemberRelation ;
			    sh:nodeKind fdp-o:metadataCatalog ;
			    sh:maxCount 1 ;
			    sh:minCount 1 ;
			  ], [
			    sh:name "contains" ;
			    sh:description "A set of triples, maintained by the LDP container, that lists documents created by the LDP container." ;
			    sh:path ldp:contains ;
			    sh:node :CatalogShape ;
			  ].""";

  /*
  As defined by https://specs.fairdatapoint.org/
   */
  public static final String CATALOG_SHACL =
      """
		  @prefix     : <http://fairdatapoint.org/> .
		  @prefix dcat: <http://www.w3.org/ns/dcat#> .
		  @prefix  dct: <http://purl.org/dc/terms/> .
		  @prefix foaf: <http://xmlns.com/foaf/0.1/> .
		  @prefix   sh: <http://www.w3.org/ns/shacl#> .
		  @prefix  xsd: <http://www.w3.org/2001/XMLSchema#> .

		  :CatalogShape a sh:NodeShape ;
		    sh:targetClass dcat:Catalog ;
		    sh:property [
		      sh:path dct:title ;
		      sh:nodeKind sh:Literal ;
		      sh:minCount 1 ;
		    ], [
		      sh:path dct:hasVersion ;
		      sh:nodeKind sh:Literal ;
		      sh:maxCount ;
		    ], [
		      sh:path dct:description ;
		      sh:nodeKind sh:Literal ;
		    ], [
		      sh:path dct:publisher ;
		      sh:node :AgentShape ;
		      sh:minCount 1;
		    ], [
		      sh:path dct:language ;
		      sh:nodeKind sh:IRI ;
		    ], [
		      sh:path dct:license ;
		      sh:nodeKind sh:IRI ;
		      sh:minCount 1;
		      sh:maxCount 1 ;
		    ], [
		      sh:path dct:conformsTo ;
		      sh:nodeKind sh:IRI ;
		      sh:maxCount 1;
		      sh:minCount 1;
		    ], [
		      sh:path dct:rights ;
		      sh:nodeKind sh:IRI ;
		    ], [
		      sh:path dct:accessRights ;
		      sh:nodeKind sh:IRI ;
		    ], [
		      sh:path dct:hasPart ;
		      sh:node sh:ContactPointShape ;
		    ], [
		      sh:path dcat:keyword ;
		      sh:nodeKind sh:Literal ;
		    ], [
		      sh:path dcat:theme ;
		      sh:nodeKind sh:IRI ;
		    ], [
		      sh:path dcat:endPointURL ;
		      sh:nodeKind sh:IRI ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ], [
		      sh:path fdp-o:startDate ;
		      sh:datatype xsd:date ;
		      sh:maxCount 1 ;
		    ], [
		      sh:path fdp-o:endDate ;
		      sh:datatype xsd:date ;
		      sh:maxCount 1 ;
		    ], [
		      sh:path fdp-o:metadataIdentifier ;
		      sh:nodeKind sh:IRI ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ], [
		      sh:path fdp-o:metadataIssued ;
		      sh:datatype xsd:dateTime ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ], [
		      sh:path fdp-o:metadataIdentifier ;
		      sh:datatype xsd:dateTime ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ].

		  :AgentShape a sh:NodeShape ;
		    sh:targetClass foaf:Agent ;
		    sh:property [
		      sh:path foaf:name ;
		      sh:nodeKind sh:Literal ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ].

		  :ContactPointShape a sh:NodeShape ;
		    sh:targetClass vcard:Kind ;
		    sh:property [
		      sh:path vcard:hasEmail ;
		      sh:nodeKind sh:Literal ;
		      sh:maxCount 1 ;
		      sh:minCount 1 ;
		    ].""";

  public static final String DCAT_PREFIXES =
      """
			@prefix : <http://data.europa.eu/r5r#> .
			@prefix adms: <http://www.w3.org/ns/adms#> .
			@prefix dash: <http://datashapes.org/dash#> .
			@prefix dc: <http://purl.org/dc/elements/1.1/> .
			@prefix dcat: <http://www.w3.org/ns/dcat#> .
			@prefix dct: <http://purl.org/dc/terms/> .
			@prefix foaf: <http://xmlns.com/foaf/0.1/> .
			@prefix org: <http://www.w3.org/ns/org#> .
			@prefix owl: <http://www.w3.org/2002/07/owl#> .
			@prefix prov: <http://www.w3.org/ns/prov#> .
			@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
			@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
			@prefix schema: <http://schema.org/> .
			@prefix sh: <http://www.w3.org/ns/shacl#> .
			@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
			@prefix skosxl: <http://www.w3.org/2008/05/skos-xl#> .
			@prefix spdx: <http://spdx.org/rdf/terms#> .
			@prefix tosh: <http://topbraid.org/tosh#> .
			@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
			@prefix xml: <http://www.w3.org/XML/1998/namespace> .
			@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
			""";

  /*
  As defined by https://github.com/SEMICeu/dcat-ap_shacl/blob/master/shacl/dcat-ap.shapes.ttl
  	*/
  public static final String DATASET_SHACL =
      DCAT_PREFIXES
          + """
		  dcat:Dataset
		    rdf:type sh:NodeShape ;
		    sh:property [
		        sh:path dct:accrualPeriodicity ;
		              sh:nodeKind sh:IRI ;
		      ] ;
		    sh:property [
		        sh:path dct:accessRights ;
		        sh:class dct:RightsStatement ;
		        sh:maxCount 1 ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:accrualPeriodicity ;
		        sh:class dct:Frequency ;
		        sh:maxCount 1 ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:conformsTo ;
		        sh:class dct:Standard ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:description ;
		        sh:minCount 1 ;
		              sh:nodeKind sh:Literal ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:hasVersion ;
		        sh:class dcat:Dataset ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:isVersionOf ;
		        sh:class dcat:Dataset ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:issued ;
		        sh:maxCount 1 ;
		              sh:severity sh:Violation ;
		        sh:shape :DateOrDateTimeDataType ;
		      ] ;
		    sh:property [
		        sh:path dct:language ;
		              sh:nodeKind sh:IRI ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:modified ;
		        sh:maxCount 1 ;
		              sh:severity sh:Violation ;
		        sh:shape :DateOrDateTimeDataType ;
		      ] ;
		    sh:property [
		        sh:path dct:provenance ;
		        sh:class dct:ProvenanceStatement ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:publisher ;
		        sh:class foaf:Agent ;
		        sh:maxCount 1 ;
		              sh:nodeKind sh:IRI ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:relation ;
		        sh:nodeKind sh:IRI ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:source ;
		        sh:class dcat:Dataset ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:spatial ;
		        sh:class dct:Location ;
		              sh:nodeKind sh:IRI ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:temporal ;
		        sh:class dct:PeriodOfTime ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dct:title ;
		        sh:minCount 1 ;
		              sh:nodeKind sh:Literal ;
		        sh:severity sh:Violation ;
		      ] ;
		      sh:property [
		        sh:path dcat:theme ;
		              sh:nodeKind sh:IRI ;
		      ] ;
		    sh:property [
		        sh:path dct:type ;
		        sh:class skos:Concept ;
		        sh:maxCount 1 ;
		              sh:nodeKind sh:IRI ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path owl:versionInfo ;
		        sh:maxCount 1 ;
		              sh:nodeKind sh:Literal ;
		        sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path adms:identifier ;
		        sh:class adms:Identifier ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path adms:sample ;
		        sh:class dcat:Distribution ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dcat:contactPoint ;
		        sh:class vcard:Kind ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dcat:distribution ;
		        sh:class dcat:Distribution ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dcat:landingPage ;
		        sh:class foaf:Document ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path dcat:theme ;
		        sh:class skos:Concept ;
		              sh:severity sh:Violation ;
		      ] ;
		    sh:property [
		        sh:path foaf:page ;
		        sh:class foaf:Document ;
		              sh:severity sh:Violation ;
		      ].""";

  /*
  As defined by https://github.com/SEMICeu/dcat-ap_shacl/blob/master/shacl/dcat-ap.shapes.ttl
  */
  public static final String DISTRIBUTION_SHACL =
      DCAT_PREFIXES
          + """
			dcat:Distribution
			  rdf:type sh:NodeShape ;
			  sh:property [
			      sh:path dct:conformsTo ;
			            sh:nodeKind sh:BlankNodeOrIRI ;
			      sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:description ;
			            sh:nodeKind sh:Literal ;
			      sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:format ;
			      sh:class dct:MediaTypeOrExtent ;
			      sh:maxCount 1 ;
			            sh:nodeKind sh:IRI ;
			      sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:issued ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			      sh:node :DateOrDateTimeDataType ;
			    ] ;
			  sh:property [
			      sh:path dct:language ;
			      sh:class dct:LinguisticSystem ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:license ;
			      sh:class dct:LicenseDocument ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:modified ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			      sh:node :DateOrDateTimeDataType ;
			    ] ;
			  sh:property [
			      sh:path dct:rights ;
			      sh:class dct:RightsStatement ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dct:title ;
			            sh:nodeKind sh:Literal ;
			      sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path spdx:checksum ;
			      sh:class spdx:Checksum ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path adms:status ;
			      sh:class skos:Concept ;
			      sh:maxCount 1 ;
			            sh:nodeKind sh:IRI ;
			      sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dcat:accessURL ;
			      sh:nodeKind sh:IRI ;
			      sh:minCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dcat:byteSize ;
			      sh:datatype xsd:decimal ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dcat:downloadURL ;
			      sh:nodeKind sh:IRI ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path dcat:mediaType ;
			      sh:class dct:MediaTypeOrExtent ;
			      sh:maxCount 1 ;
			            sh:severity sh:Violation ;
			    ] ;
			  sh:property [
			      sh:path foaf:page ;
			      sh:class foaf:Document ;
			            sh:severity sh:Violation ;
			    ].""";
}
