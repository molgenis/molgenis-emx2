package org.molgenis.emx2.rdf;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.molgenis.emx2.MolgenisException;

public class RdfFetcher {

  private static final String ACCEPT_HEADER =
      "text/turtle, application/ld+json, application/rdf+xml;q=0.9";

  private RdfFetcher() {}

  public static void parse(InputStream input, String formatHint, RDFHandler handler) {
    RDFFormat format = detectFormat(formatHint);
    RDFParser parser = Rio.createParser(format);
    parser.setRDFHandler(handler);
    try {
      parser.parse(input);
    } catch (Exception e) {
      throw new MolgenisException("Failed to parse RDF: " + e.getMessage());
    }
  }

  public static InputStream fetchUrl(String url) {
    try {
      HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
      conn.setRequestProperty("Accept", ACCEPT_HEADER);
      conn.setConnectTimeout(30_000);
      conn.setReadTimeout(60_000);
      conn.setInstanceFollowRedirects(true);
      int status = conn.getResponseCode();
      if (status >= 400) {
        throw new MolgenisException("HTTP " + status + " fetching " + url);
      }
      return conn.getInputStream();
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e) {
      throw new MolgenisException("Failed to fetch URL " + url + ": " + e.getMessage());
    }
  }

  public static String getContentType(HttpURLConnection connection) {
    return connection.getContentType();
  }

  private static RDFFormat detectFormat(String hint) {
    if (hint == null) return RDFFormat.TURTLE;
    if (hint.startsWith(".")) {
      return Rio.getParserFormatForFileName("file" + hint).orElse(RDFFormat.TURTLE);
    }
    return Rio.getParserFormatForMIMEType(hint)
        .orElse(Rio.getParserFormatForFileName("file." + hint).orElse(RDFFormat.TURTLE));
  }
}
