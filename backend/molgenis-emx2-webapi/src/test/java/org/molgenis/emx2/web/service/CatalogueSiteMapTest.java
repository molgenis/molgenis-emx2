package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

@Tag("slow")
public class CatalogueSiteMapTest {

  @Test
  public void buildSiteMap() {
    Schema schema = mock(Schema.class);
    Table table = mock(Table.class);
    Query query = mock(Query.class);
    List<Row> row = Collections.singletonList(new Row("id", "my-cohort-id"));
    when(schema.getTable("Cohorts")).thenReturn(table);
    when(table.select(any())).thenReturn(query);
    when(query.retrieveRows()).thenReturn(row);
    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" >\n"
            + "  <url>\n"
            + "    <loc>https://my/base/url/ssr-catalogue/all/Cohorts/my-cohort-id</loc>\n"
            + "  </url>\n"
            + "</urlset>";
    assertEquals(expected, catalogueSiteMap.buildSiteMap());
  }
}
