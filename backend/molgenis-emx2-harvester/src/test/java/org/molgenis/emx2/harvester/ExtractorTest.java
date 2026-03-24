package org.molgenis.emx2.harvester;

import java.io.IOException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

class ExtractorTest {

  @Test
  void givenUrl_thenExtractRdf() throws IOException {
    String url = "http://localhost:8081/pet-store/api/ttl";

    Extractor extractor = new Extractor();
    Model model = extractor.extract(url);

    for (Statement statement : model) {
      System.out.println(
          String.join(
              " -> ",
              statement.getSubject().stringValue(),
              statement.getPredicate().stringValue(),
              statement.getObject().stringValue()));
    }
  }
}
