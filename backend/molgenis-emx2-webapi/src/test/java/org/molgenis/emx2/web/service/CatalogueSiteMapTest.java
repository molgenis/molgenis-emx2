package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class CatalogueSiteMapTest {

  @Test
  public void buildSiteMap() {
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    Query query = mock(Query.class);
    List<Row> row =
        List.of(
            new Row("id", "my-id", "type", "Data source"),
            new Row("id", "my-second-id", "type", "Network"));

    when(schema.getTable("Resources")).thenReturn(table);
    when(table.select(any(), any())).thenReturn(query);
    when(query.retrieveRows()).thenReturn(row);
    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" >\n"
            + "  <url>\n"
            + "    <loc>https://my/base/url/catalogue/all/collections/my-id</loc>\n"
            + "  </url>\n"
            + "  <url>\n"
            + "    <loc>https://my/base/url/catalogue/all/networks/my-second-id</loc>\n"
            + "  </url>\n"
            + "</urlset>";
    assertEquals(expected, catalogueSiteMap.buildSiteMap());
  }

  @Test
  void buildSiteMapWhenExpectedTableIsMissing() {
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    Query query = mock(Query.class);

    when(table.select(any(), any())).thenReturn(query);

    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");
    assertThrows(MolgenisException.class, catalogueSiteMap::buildSiteMap);
  }
}
