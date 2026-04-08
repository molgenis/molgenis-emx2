package org.molgenis.emx2.rag;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SiteMapReader {

  private final String sitemapUrl;

  public SiteMapReader(String sitemapUrl) {
    this.sitemapUrl = sitemapUrl;
  }

  public List<String> readLocations() throws IOException {
    List<String> urls = new ArrayList<>();

    try {
      URL url = URI.create(sitemapUrl).toURL();

      Document doc =
          DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());

      doc.getDocumentElement().normalize();

      NodeList locNodes = doc.getElementsByTagName("loc");

      for (int i = 0; i < locNodes.getLength(); i++) {
        Node node = locNodes.item(i);
        urls.add(node.getTextContent().trim());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    return urls;
  }
}
