package org.molgenis.emx2.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    List<Row> rows =
        List.of(
            new Row("id", "my-id", "mg_tableclass", "mockSchema.Collections"),
            new Row("id", "my-second-id", "mg_tableclass", "mockSchema.Networks"));

    when(schema.getName()).thenReturn("mockSchema");
    when(schema.getTable("Resources")).thenReturn(table);
    when(table.select(any(), any())).thenReturn(query);
    when(query.retrieveRows()).thenReturn(rows);
    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        """
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" >
      <url>
        <loc>https://my/base/url/all/collections/my-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/networks/my-second-id</loc>
      </url>
    </urlset>""";
    assertEquals(expected, catalogueSiteMap.buildSiteMap());
  }

  @Test
  void buildSiteMapForSchemaWithVariables() {
    Schema schema = mock(Schema.class);
    Table resourceTable = mock(Table.class);
    Table variableTable = mock(Table.class);
    Query resourceQuery = mock(Query.class);

    Query variableQuery = mock(Query.class);
    List<Row> resourceRows =
        List.of(
            new Row("id", "my-collection-id", "mg_tableclass", "mockSchema.Collections"),
            new Row("id", "my-second-collection-id", "mg_tableclass", "mockSchema.Collections"),
            new Row("id", "my-network-id", "mg_tableclass", "mockSchema.Networks"),
            new Row("id", "my-second-network-id", "mg_tableclass", "mockSchema.Networks"),
            new Row("id", "my-catalogue-id", "mg_tableclass", "mockSchema.Catalogues"),
            new Row("id", "my-second-catalogue-id", "mg_tableclass", "mockSchema.Catalogues"));
    List<Row> variableRows =
        List.of(
            new Row(
                "name",
                "Var name",
                "resource",
                "lifetime",
                "dataset",
                "core",
                "mg_tableclass",
                "Variables"));
    when(schema.getName()).thenReturn("mockSchema");
    when(schema.getTable("Resources")).thenReturn(resourceTable);
    when(resourceTable.select(any(), any())).thenReturn(resourceQuery);
    when(resourceQuery.retrieveRows()).thenReturn(resourceRows);

    when(schema.getTable("Variables")).thenReturn(variableTable);
    when(variableTable.select(any(), any(), any())).thenReturn(variableQuery);
    when(variableQuery.where(any())).thenReturn(variableQuery);
    when(variableQuery.retrieveRows()).thenReturn(variableRows);

    CatalogueSiteMap catalogueSiteMap = new CatalogueSiteMap(schema, "https://my/base/url");

    String expected =
        """
    <?xml version="1.0" encoding="UTF-8"?>
    <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" >
      <url>
        <loc>https://my/base/url/all/collections/my-collection-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/collections/my-second-collection-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/networks/my-network-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/networks/my-second-network-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/catalogues/my-catalogue-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/catalogues/my-second-catalogue-id</loc>
      </url>
      <url>
        <loc>https://my/base/url/all/variables/Var%20name-lifetime-core-lifetime?keys=%7B%22name%22%3A%22Var+name%22%2C%22resource%22%3A%7B%22id%22%3A%22lifetime%22%7D%2C%22dataset%22%3A%7B%22name%22%3A%22core%22%2C%22resource%22%3A%7B%22id%22%3A%22lifetime%22%7D%7D%7D</loc>
      </url>
    </urlset>""";
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
