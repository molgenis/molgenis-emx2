package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrichmentTest {

  private static final String SELECT_QUERY =
      """
      PREFIX afr: <http://purl.allotrope.org/ontologies/result#>
      PREFIX afrl: <http://purl.allotrope.org/ontologies/role#>
      PREFIX dc: <http://purl.org/dc/elements/1.1/>
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dcatap: <http://data.europa.eu/r5r/>
      PREFIX dcterms: <http://purl.org/dc/terms/>
      PREFIX edam: <http://edamontology.org/>
      PREFIX efo: <http://www.ebi.ac.uk/efo/>
      PREFIX ejp: <https://w3id.org/ejp-rd/vocabulary#>
      PREFIX ensembl: <http://ensembl.org/glossary/>
      PREFIX fdp-o: <https://w3id.org/fdp/fdp-o#>
      PREFIX fg: <https://w3id.org/fair-genomes/resource/>
      PREFIX foaf: <http://xmlns.com/foaf/0.1/>
      PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
      PREFIX hl7: <http://purl.bioontology.org/ontology/HL7/>
      PREFIX ldp: <http://www.w3.org/ns/ldp#>
      PREFIX lnc: <http://purl.bioontology.org/ontology/LNC/>
      PREFIX mesh: <http://purl.bioontology.org/ontology/MESH/>
      PREFIX obo: <http://purl.obolibrary.org/obo/>
      PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
      PREFIX odrl: <http://www.w3.org/ns/odrl/2/>
      PREFIX ordo: <http://www.orpha.net/ORDO/>
      PREFIX org: <http://www.w3.org/ns/org#>
      PREFIX owl: <http://www.w3.org/2002/07/owl#>
      PREFIX prov: <http://www.w3.org/ns/prov#>
      PREFIX qb: <http://purl.org/linked-data/cube#>
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
      PREFIX schema: <http://schema.org/>
      PREFIX sio: <http://semanticscience.org/resource/>
      PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
      PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
      SELECT
          ?Resources ?start_year ?end_year
      WHERE {
          # ?Resources a dcat:dataset .
          # foaf:catalog_id ?id ;
          ?Resources dcterms:title ?name .
          OPTIONAL { ?Resources dcat:startDate ?start_year . }
          OPTIONAL { ?Resources dcat:endDate ?end_year . }
      }
      GROUP BY ?Resources ?rdf_type ?Endpoint_id ?fdp_endpoint ?ldp_membership_relation ?id ?pid ?name ?local_name ?acronym ?website ?description ?start_year ?end_year ?conforms_to ?has_member_relation ?issued ?modified ?design_description ?data_collection_description ?reason_sustained ?data_resources ?number_of_participants ?underlying_population ?age_min ?age_max ?publisher ?creator ?contact_point ?child_networks ?access_rights ?release_type ?provenance_statement ?theme ?applicable_legislation
      """;

  private static final String CONSTUCT =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dcterms: <http://purl.org/dc/terms/>
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

      CONSTRUCT {
          ?Resources dcat:startDate ?startYear .
          ?Resources dcat:endDate ?endYear .
      }
      WHERE {
          ?Resources rdf:type dcat:Dataset .
          ?Resources dcterms:temporal ?temporal .
          ?temporal dcat:endDate ?endDate .
          ?temporal dcat:startDate ?startDate .
          BIND(YEAR(?startDate) AS ?startYear)
          BIND(YEAR(?endDate) AS ?endYear)
      }
      """;

  private SailRepository repository;
  private SailRepositoryConnection conn;

  @BeforeEach
  void setUp() throws IOException {
    repository = new SailRepository(new MemoryStore());
    conn = repository.getConnection();
    try (InputStream inputStream = readTtl("dataset.ttl")) {
      conn.add(inputStream, RDFFormat.TURTLE);
    }
  }

  @AfterEach
  void tearDown() {
    conn.close();
  }

  @Test
  void shouldAddStartAndEndYear() {
    System.out.println("Without construct");
    printStartAndEndFromData();
    System.out.println("-".repeat(100));

    GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTUCT);
    Model result = QueryResults.asModel(graphQuery.evaluate());
    result.forEach(conn::add);
    conn.commit();

    System.out.println("With construct");
    printStartAndEndFromData();
  }

  private void printStartAndEndFromData() {
    List<BindingSet> results =
        conn.prepareTupleQuery(QueryLanguage.SPARQL, SELECT_QUERY).evaluate().stream().toList();

    System.out.println(results.getFirst().getValue("start_year"));
    System.out.println(results.getFirst().getValue("end_year"));
  }

  private InputStream readTtl(String path) {
    return EnrichmentTest.class.getResourceAsStream(path);
  }
}
