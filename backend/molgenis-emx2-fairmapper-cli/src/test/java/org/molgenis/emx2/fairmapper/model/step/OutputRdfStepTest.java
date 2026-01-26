package org.molgenis.emx2.fairmapper.model.step;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class OutputRdfStepTest {

  @Test
  void resolveFormatWithTurtleAccept() {
    OutputRdfStep step = new OutputRdfStep("jsonld", List.of());
    assertEquals("turtle", step.resolveFormat("text/turtle"));
  }

  @Test
  void resolveFormatWithJsonLdAccept() {
    OutputRdfStep step = new OutputRdfStep("turtle", List.of());
    assertEquals("jsonld", step.resolveFormat("application/ld+json"));
  }

  @Test
  void resolveFormatWithNtriplesAccept() {
    OutputRdfStep step = new OutputRdfStep("turtle", List.of());
    assertEquals("ntriples", step.resolveFormat("application/n-triples"));
  }

  @Test
  void resolveFormatWithNullAccept() {
    OutputRdfStep step = new OutputRdfStep("turtle", List.of());
    assertEquals("turtle", step.resolveFormat(null));
  }

  @Test
  void resolveFormatWithEmptyAccept() {
    OutputRdfStep step = new OutputRdfStep("jsonld", List.of());
    assertEquals("jsonld", step.resolveFormat(""));
  }

  @Test
  void resolveFormatWithUnknownAccept() {
    OutputRdfStep step = new OutputRdfStep("turtle", List.of());
    assertEquals("turtle", step.resolveFormat("text/html"));
  }

  @Test
  void resolveFormatWithMultipleAccept() {
    OutputRdfStep step = new OutputRdfStep("ntriples", List.of());
    assertEquals("turtle", step.resolveFormat("text/html, text/turtle;q=0.9"));
  }
}
