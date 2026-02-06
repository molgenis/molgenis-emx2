package org.molgenis.emx2.fairmapper.rdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.fairmapper.FairMapperException;

public class JsonLdRdfConverter {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static Model jsonLdToModel(JsonNode jsonLd) {
    try {
      String jsonLdString = MAPPER.writeValueAsString(jsonLd);
      InputStream inputStream =
          new ByteArrayInputStream(jsonLdString.getBytes(StandardCharsets.UTF_8));
      return Rio.parse(inputStream, "", RDFFormat.JSONLD);
    } catch (Exception e) {
      throw new FairMapperException("Failed to convert JSON-LD to RDF Model", e);
    }
  }

  public static JsonNode modelToJsonLd(Model model) {
    try {
      StringWriter writer = new StringWriter();
      Rio.write(model, writer, RDFFormat.JSONLD);
      return MAPPER.readTree(writer.toString());
    } catch (Exception e) {
      throw new FairMapperException("Failed to convert RDF Model to JSON-LD", e);
    }
  }
}
