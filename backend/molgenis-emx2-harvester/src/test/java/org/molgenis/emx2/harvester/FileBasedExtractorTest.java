package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.Test;

class FileBasedExtractorTest {

  @Test
  void shouldCombineAllRdfFiles() {
    List<String> files =
        Stream.of("data/Catalog.ttl", "data/Dataset.ttl", "data/Distribution.ttl")
            .map(FileBasedExtractor.class::getResource)
            .filter(Objects::nonNull)
            .map(URL::getPath)
            .toList();

    SailRepository repository = new SailRepository(new MemoryStore());
    FileBasedExtractor extractor = new FileBasedExtractor(repository, files);
    extractor.extract();

    Variable subject = SparqlBuilder.var("subject");
    Variable predicate = SparqlBuilder.var("predicate");
    Variable object = SparqlBuilder.var("object");

    // Build the query to select all triples
    SelectQuery query = Queries.SELECT(subject).where(subject.has(predicate, object));

    List<String> subjects =
        repository
            .getConnection()
            .prepareTupleQuery(QueryLanguage.SPARQL, query.getQueryString())
            .evaluate()
            .stream()
            .map(binding -> binding.getValue("subject").stringValue())
            .toList();

    assertTrue(
        subjects.containsAll(
            List.of(
                "urn:uuid:f949ecf4-5bdc-4127-b667-a13bc00eb295",
                "urn:uuid:743edbee-48a6-462d-8ea8-d80366c50ef8",
                "urn:uuid:1850b6b2-a5d9-474c-abfb-506f19b13ee7")));
  }
}
