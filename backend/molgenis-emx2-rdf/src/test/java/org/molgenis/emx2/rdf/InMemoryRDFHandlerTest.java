package org.molgenis.emx2.rdf;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class InMemoryRDFHandlerTest {
  // See text behind "#" for inline explanations
  private final String rdfInput =
      """
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix dcterms: <http://purl.org/dc/terms/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<http://localhost:8080/pet%20store/api/rdf/User/username=bofke> rdfs:label "bofke";
  # "a11ac033b28f42dd9760547d622e5eea" should be changed to "8hlbnm.jpg" to enable easy comparison
  foaf:img <http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea>;
  <http://localhost:8080/pet%20store/api/rdf/User/column/picture> <http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea>;
  # custom date should not change
  <http://localhost:8080/pet%20store/api/rdf/User/column/customDate> "2025-06-16T15:01:15"^^xsd:dateTime;
  # dcterms/"mg_" columns related to creation/modification date should change
  dcterms:created "2025-06-16T15:01:15"^^xsd:dateTime;
  dcterms:modified "2025-06-16T15:01:15"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_insertedOn> "2025-06-16T15:01:15"^^xsd:dateTime;
  <http://localhost:8080/pet%20store/api/rdf/User/column/mg_updatedOn> "2025-06-16T15:01:15"^^xsd:dateTime .

# "a11ac033b28f42dd9760547d622e5eea" should be changed to "8hlbnm.jpg" to enable easy comparison
<http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea> rdfs:label "8hlbnm.jpg";
  dcterms:title "8hlbnm.jpg";
  dcterms:format <http://www.iana.org/assignments/media-types/image/jpeg> .
""";

  private final String rdfFixed =
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

  // As there is no extra data is present for "a11ac033b28f42dd9760547d622e5eea", dynamic part of
  // the object will be replaced with "identicalFileIRI" instead.
  private final String rdfNoFileLabelInput =
      """
<http://localhost:8080/pet%20store/api/rdf/User/username=bofke>
  <http://localhost:8080/pet%20store/api/rdf/User/column/picture> <http://localhost:8080/pet%20store/api/file/User/picture/a11ac033b28f42dd9760547d622e5eea> .
""";

  private final String rdfNoFileLabelFixed =
      """
<http://localhost:8080/pet%20store/api/rdf/User/username=bofke>
  <http://localhost:8080/pet%20store/api/rdf/User/column/picture> <http://localhost:8080/pet%20store/api/file/User/picture/identicalFileIRI> .
""";

  private final String rdfBNode =
      """
[] a <http://xmlns.com/foaf/0.1/Person> .
[] a <http://xmlns.com/foaf/0.1/Organization> .
""";

  private final String rdfBNodeFixed =
      """
_:0 a <http://xmlns.com/foaf/0.1/Person> .
_:1 a <http://xmlns.com/foaf/0.1/Organization> .
""";

  @Test
  void testRdfComparisonFixes() throws IOException {
    InMemoryRDFHandler handlerFixedString = new InMemoryRDFHandler(false);
    RdfParser.parseString(handlerFixedString, rdfFixed);

    InMemoryRDFHandler handlerWithFix = new InMemoryRDFHandler(true);
    RdfParser.parseString(handlerWithFix, rdfInput);

    CustomAssertions.equals(handlerFixedString, handlerWithFix);
  }

  @Test
  void testRdfComparisonFixesWithNoFileLabel() throws IOException {
    InMemoryRDFHandler handlerFixedString = new InMemoryRDFHandler(false);
    RdfParser.parseString(handlerFixedString, rdfNoFileLabelFixed);

    InMemoryRDFHandler handlerWithFix = new InMemoryRDFHandler(true);
    RdfParser.parseString(handlerWithFix, rdfNoFileLabelInput);

    CustomAssertions.equals(handlerFixedString, handlerWithFix);
  }

  @Test
  void testRdfComparisonFixesBlankNode() throws IOException {
    InMemoryRDFHandler handlerFixedString = new InMemoryRDFHandler(false);
    RdfParser.parseString(handlerFixedString, rdfBNodeFixed);

    InMemoryRDFHandler handlerWithFix = new InMemoryRDFHandler(true);
    RdfParser.parseString(handlerWithFix, rdfBNode);

    CustomAssertions.equals(handlerFixedString, handlerWithFix);
  }
}
