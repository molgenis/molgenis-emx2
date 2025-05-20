package org.molgenis.emx2.rdf.writers;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.molgenis.emx2.rdf.RdfMapData;

abstract class RdfStreamWriter extends RdfWriter {
  private final StreamRDF streamRdf;

  protected RdfStreamWriter(RdfMapData rdfMapData, OutputStream out, Lang lang) {
    super(rdfMapData);
    streamRdf = StreamRDFWriter.getWriterStream(out, lang);
    streamRdf.start();
  }

  protected void writeTriple(Triple triple) {
    streamRdf.triple(triple);
  }

  protected void writeTriple(Node subject, Node predicate, Node object) {
    writeTriple(Triple.create(subject, predicate, object));
  }

  @Override
  public void close() throws IOException {
    streamRdf.finish();
  }
}
