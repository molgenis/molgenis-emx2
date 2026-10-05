package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.TestPipelineFiles.withExtract;

import java.net.URI;
import java.util.List;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.fairmapper.extractors.CrawlStep;
import org.molgenis.emx2.fairmapper.extractors.CrawlSteps;
import org.molgenis.emx2.rdf.vocabulary.FDPO;

class ExtractSpecTest {

  @Test
  void givenUrlOnly_whenParsed_thenNoCrawlAndStrict() {
    ExtractSpec extract = parse("- extract: { url: https://fdp.example.org }");

    assertEquals(new ExtractSpec(URI.create("https://fdp.example.org"), List.of(), true), extract);
  }

  @Test
  void givenStrictFalse_whenParsed_thenNotStrict() {
    assertFalse(parse("- extract: { url: https://fdp.example.org, strict: false }").strict());
  }

  @Test
  void givenCrawlPreset_whenParsed_thenUsePresetSteps() {
    ExtractSpec extract = parse("- extract: { url: https://fdp.example.org, crawl: fdp }");

    assertEquals(CrawlSteps.FDP.steps(), extract.crawl());
  }

  @Test
  void givenCrawlStepsWithPrefixedNames_whenParsed_thenResolveDefaultNamespaces() {
    ExtractSpec extract =
        parse(
            """
            - extract:
                url: https://fdp.example.org
                crawl:
                  - { name: catalog, predicate: fdp-o:metadataCatalog }
                  - { name: dataset, predicate: dcat:dataset }
            """);

    assertEquals(
        List.of(
            new CrawlStep("catalog", FDPO.METADATA_CATALOG),
            new CrawlStep("dataset", DCAT.HAS_DATASET)),
        extract.crawl());
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://example.org/ns#hasPart", "\"<http://example.org/ns#hasPart>\""})
  void givenCrawlStepWithFullIri_whenParsed_thenUseIri(String predicate) {
    ExtractSpec extract =
        parse(
            "- extract: { url: https://fdp.example.org, crawl: [{ name: part, predicate: %s }] }"
                .formatted(predicate));

    assertEquals(
        List.of(new CrawlStep("part", Values.iri("http://example.org/ns#hasPart"))),
        extract.crawl());
  }

  @Test
  void givenUnknownCrawlPreset_whenParsed_thenThrowException() {
    assertInvalid(
        "- extract: { url: https://fdp.example.org, crawl: ftp }",
        "unknown crawl preset 'ftp'; known presets: fdp");
  }

  @ParameterizedTest
  @ValueSource(strings = {"foo:about", "\"dcat:\"", "about", "\"<not an iri>\""})
  void givenInvalidPredicate_whenParsed_thenThrowException(String predicate) {
    assertInvalid(
        "- extract: { url: https://fdp.example.org, crawl: [{ name: x, predicate: %s }] }"
            .formatted(predicate),
        "' is not");
  }

  @Test
  void givenCrawlStepWithoutPredicate_whenParsed_thenThrowException() {
    assertInvalid(
        "- extract: { url: https://fdp.example.org, crawl: [{ name: catalog }] }",
        "each crawl step requires a name and a predicate");
  }

  @Test
  void givenCrawlStepWithUnknownOption_whenParsed_thenThrowException() {
    assertInvalid(
        "- extract: { url: https://fdp.example.org, crawl: [{ name: c, predicate: dcat:dataset, depth: 2 }] }",
        "unknown crawl step option 'depth'");
  }

  @Test
  void givenCrawlOfWrongType_whenParsed_thenThrowException() {
    assertInvalid(
        "- extract: { url: https://fdp.example.org, crawl: { name: c } }",
        "crawl must be a crawl preset name or a list of crawl steps");
  }

  @ParameterizedTest
  @ValueSource(strings = {"- extract: {}", "- extract"})
  void givenNoUrl_whenParsed_thenThrowException(String extractStep) {
    assertInvalid(extractStep, "extract requires url");
  }

  @Test
  void givenRelativeUrl_whenParsed_thenThrowException() {
    assertInvalid("- extract: { url: fdp.example.org }", "extract url must be absolute");
  }

  @Test
  void givenUnknownOption_whenParsed_thenThrowException() {
    assertInvalid("- extract: { url: https://fdp.example.org, file: ./data.ttl }", "file");
  }

  private static ExtractSpec parse(String extractStep) {
    return PipelineFile.parse(withExtract(extractStep)).extract();
  }

  private static void assertInvalid(String extractStep, String expectedMessagePart) {
    TestPipelineFiles.assertInvalid(withExtract(extractStep), expectedMessagePart);
  }
}
