package org.molgenis.emx2.rdf;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class InMemoryRDFHandlerTest {
  private final String rdfExample =
      """
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://localhost:8080/pet%20store/api/rdf/User/username=bofke> rdfs:label "bofke";
  foaf:img <http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea>;
  <http://localhost:8080/pet%20store/api/rdf/User/column/picture> <http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea>;
  <http://localhost:8080/pet%20store/api/rdf/User/column/customDate> "2025-06-16T15:01:15"^^xsd:dateTime;
  dcterms:created "2025-06-16T15:01:15"^^xsd:dateTime;
  dcterms:modified "2025-06-16T15:01:15"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_insertedOn> "2025-06-16T15:01:15"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_updatedOn> "2025-06-16T15:01:15"^^xsd:dateTime .

<http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea> rdfs:label "8hlbnm.jpg";
  dcterms:title "8hlbnm.jpg";
  dcterms:format <http://www.iana.org/assignments/media-types/image/jpeg> .
""";

  private final String rdfExampleAfterFix =
      """
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://localhost:8080/pet%20store/api/rdf/User/username=bofke> rdfs:label "bofke";
  foaf:img <http://localhost:8080/pet%20store/api/file/User/picture/8hlbnm.jpg>;
  <http://localhost:8080/pet%20store/api/rdf/User/column/picture> <http://localhost:8080/pet%20store/api/file/User/picture/8hlbnm.jpg>;
  <http://localhost:8080/pet%20store/api/rdf/User/column/customDate> "2025-06-16T15:01:15"^^xsd:dateTime;
  dcterms:created "2021-02-08T12:15:00"^^xsd:dateTime;
  dcterms:modified "2021-02-08T12:15:00"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_insertedOn> "2021-02-08T12:15:00"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_updatedOn> "2021-02-08T12:15:00"^^xsd:dateTime .

<http://localhost:8080/pet%20store/api/file/User/picture/8hlbnm.jpg> rdfs:label "8hlbnm.jpg";
  dcterms:title "8hlbnm.jpg";
  dcterms:format <http://www.iana.org/assignments/media-types/image/jpeg> .
""";

  @Test
  void testRdfComparisonFixChanges() throws IOException {
    InMemoryRDFHandler handlerFixedString = new InMemoryRDFHandler(false);
    RdfParser.parseString(handlerFixedString, rdfExampleAfterFix);

    InMemoryRDFHandler handlerWithFix = new InMemoryRDFHandler(true);
    RdfParser.parseString(handlerWithFix, rdfExample);

    CustomAssertions.equals(handlerFixedString, handlerWithFix);
  }
}
