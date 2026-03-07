package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestException;

class RdfFetcherTest {

  @Test
  void parsesTurtleContent() throws IOException {
    InputStream is =
        getClass().getResourceAsStream("/org/molgenis/emx2/fairmapper/dcat/catalog.ttl");
    assertNotNull(is);
    String turtle = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    RdfFetcher fetcher = new RdfFetcher();
    Model model = fetcher.parseTurtle(turtle);

    assertNotNull(model);
    assertFalse(model.isEmpty());
    assertTrue(model.size() > 10);
  }

  @Test
  void rejectsOversizedContent() {
    RdfFetcher fetcher = new RdfFetcher(10L);
    assertThrows(DcatHarvestException.class, () -> fetcher.parseTurtle("x".repeat(100)));
  }

  @Test
  void rejectsNonHttpSchemeOnFetch() {
    RdfFetcher fetcher = new RdfFetcher();
    assertThrows(DcatHarvestException.class, () -> fetcher.fetch("ftp://example.org/rdf"));
  }
}
