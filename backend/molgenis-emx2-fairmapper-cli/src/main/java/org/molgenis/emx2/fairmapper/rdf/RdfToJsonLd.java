package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import java.io.StringWriter;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;

public class RdfToJsonLd {

  public String convert(Model model) throws IOException {
    try {
      StringWriter writer = new StringWriter();
      RDFWriter rdfWriter = Rio.createWriter(RDFFormat.JSONLD, writer);

      WriterConfig config = rdfWriter.getWriterConfig();
      config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
      config.set(JSONLDSettings.COMPACT_ARRAYS, true);

      rdfWriter.startRDF();
      for (org.eclipse.rdf4j.model.Statement statement : model) {
        rdfWriter.handleStatement(statement);
      }
      rdfWriter.endRDF();

      return writer.toString();
    } catch (Exception e) {
      throw new IOException("Failed to convert RDF to JSON-LD: " + e.getMessage(), e);
    }
  }
}
