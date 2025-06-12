package org.molgenis.emx2.rdf.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.RdfUtils;
import org.molgenis.emx2.rdf.generators.RdfApiGeneratorFactory;
import org.molgenis.emx2.rdf.writers.WriterFactory;

class RdfConfigReaderTest {
  @Test
  void testValidFullRdfConfig() {
    // use non-uppercase enum values to test case insensitivity
    Schema schema =
        mockSchema(
            """
writer: model
generator: sEMantic
""");

    RdfConfig config = RdfConfigReader.read(schema);

    assertAll(
        () -> assertEquals(WriterFactory.MODEL, config.getWriterFactory()),
        () -> assertEquals(RdfApiGeneratorFactory.SEMANTIC, config.getRdfApiGeneratorFactory()));
  }

  @Test
  void testPartlyConfiguredRdfConfig() {
    Schema schema =
        mockSchema(
            """
writer: stream
""");

    RdfConfig config = RdfConfigReader.read(schema);

    assertAll(
        () -> assertEquals(WriterFactory.STREAM, config.getWriterFactory()),
        () -> assertEquals(RdfApiGeneratorFactory.EMX2, config.getRdfApiGeneratorFactory()));
  }

  @Test
  void testEmptyRdfConfig() {
    Schema schema = mockSchema("");

    RdfConfig config = RdfConfigReader.read(schema);

    assertAll(
        () -> assertEquals(WriterFactory.MODEL, config.getWriterFactory()),
        () -> assertEquals(RdfApiGeneratorFactory.EMX2, config.getRdfApiGeneratorFactory()));
  }

  @Test
  void testNoRdfConfig() {
    Schema schema = mock(Schema.class);
    when(schema.hasSetting(RdfUtils.SETTING_RDF_CONFIG)).thenReturn(false);

    RdfConfig config = RdfConfigReader.read(schema);

    assertAll(
        () -> assertEquals(WriterFactory.MODEL, config.getWriterFactory()),
        () -> assertEquals(RdfApiGeneratorFactory.EMX2, config.getRdfApiGeneratorFactory()));
  }

  private Schema mockSchema(String rdfConfigSetting) {
    Schema schema = mock(Schema.class);
    when(schema.hasSetting(RdfUtils.SETTING_RDF_CONFIG)).thenReturn(true);
    when(schema.getSettingValue(RdfUtils.SETTING_RDF_CONFIG)).thenReturn(rdfConfigSetting);
    return schema;
  }
}
