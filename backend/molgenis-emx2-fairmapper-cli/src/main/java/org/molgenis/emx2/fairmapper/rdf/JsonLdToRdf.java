package org.molgenis.emx2.fairmapper.rdf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.fairmapper.FairMapperException;

public class JsonLdToRdf {

  public String convert(String jsonLd, String format) {
    try {
      RDFFormat outputFormat = getRdfFormat(format);

      InputStream inputStream = new ByteArrayInputStream(jsonLd.getBytes(StandardCharsets.UTF_8));
      Model model = Rio.parse(inputStream, "", RDFFormat.JSONLD);

      StringWriter writer = new StringWriter();
      Rio.write(model, writer, outputFormat);
      return writer.toString();

    } catch (Exception e) {
      throw new FairMapperException("Failed to convert JSON-LD to RDF format: " + format, e);
    }
  }

  private RDFFormat getRdfFormat(String format) {
    return switch (format.toLowerCase()) {
      case "turtle" -> RDFFormat.TURTLE;
      case "jsonld" -> RDFFormat.JSONLD;
      case "ntriples" -> RDFFormat.NTRIPLES;
      default -> throw new FairMapperException("Unsupported RDF format: " + format);
    };
  }
}
